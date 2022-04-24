package com.annevonwolffen.testrxandcoroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

@ExperimentalStdlibApi
class ExceptionsInCoroutines {

    val data = ThreadLocal<Int>()
    val context = Dispatchers.Default + CoroutineName("debug") + data.asContextElement(44)

    private fun CoroutineScope.printCoroutineData() {
        val dispatcher = coroutineContext[CoroutineDispatcher]
        val coroutineName = coroutineContext[CoroutineName]?.name
        println("$dispatcher, $coroutineName, ${data.get()}")
    }

    @Test
    fun test() {
        runBlocking {
            launch(context) {
                printCoroutineData()
                launch {
                    printCoroutineData()
                    withContext(Dispatchers.Unconfined) {
                        printCoroutineData()
                    }
                }
            }
            launch(CoroutineName("prod")) {
                printCoroutineData()
                launch(data.asContextElement(11)) {
                    printCoroutineData()
                }
            }
        }
    }

    @Test
    fun launch() {
        runBlocking {
            launch(Dispatchers.IO) {
                launch {
                    println("A")
                }
                println("B")
            }
            println("C")
        }
        //CBA
    }

    @Test
    fun withContext() {
        runBlocking {
            withContext(Dispatchers.IO) {
                launch {
                    println('A')
                }
                println("B")
            }
            println("C")
        }
        //BAC
    }

    @Test
    fun testCancel() {
        runBlocking {
            println("before launch")
            val job = launch(Dispatchers.IO) {
                println("in launch")
                delay(1000)
            }
            println("before cancel")
            job.cancel()
            println("after cancel")
        }
    }

    @Test
    fun callSuspendFromCanceledJob() {
        runBlocking {
            val job = launch(Dispatchers.IO) {
                delay(100)
                // if (!isActive) {
                suspendFun()
                // }
            }
            println("before cancel")
            job.cancelAndJoin()
            println("after cancel")
        }
    }

    @Test
    fun errorHandling() {
        runBlocking {
            launch(Dispatchers.Unconfined) { // #1
                println("in coroutine #1")

                launch { /* #2 */
                    println("in coroutine #2")
                }
                launch { // #3
                    println("in coroutine #3")
                    launch(Dispatchers.IO) { // #4
                        println("in coroutine #4")
                        launch { /* #5 */
                            delay(1000)
                            println("in coroutine #5")
                        }
                        launch { /* #6 */
                            delay(1000)
                            println("in coroutine #6")
                        }
                        throw CancellationException()
                    }
                    coroutineScope { // #7
                        println("in coroutine #7")
                        async {
                            delay(1000)
                            // #8
                            println("in coroutine #8")
                        }
                        async {
                            delay(1000)
                            // #9
                            if (isActive) {
                                println("in coroutine #9")
                            }
                        }
                        launch {
                            // #10
                            println("in coroutine #10")
                            // throw Exception("OOOOOPS!!!")
                            // throw CancellationException()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun cancelling() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) {
                coroutineScope {
                    // print a message twice a second
                    if (System.currentTimeMillis() >= nextPrintTime) {
                        println("Hello ${i++}")
                        nextPrintTime += 500L
                    }
                }
            }
        }
        delay(1000L)
        println("Cancel!")
        job.cancel()
        println("Done!")
    }

    @Test
    fun exceptionHanding() {
        runBlocking {
            launch {
                launch {
                    // #1
                    delay(100)
                    println("in coroutine #1")
                }
                try {
                    coroutineScope {
                        launch {
                            // #2
                            delay(100)
                            println("in coroutine #2")

                        }
                        launch {
                            // #3
                            println("in coroutine #3")
                            throw Exception("OOOPS!!!")

                        }
                    }
                } catch (e: Exception) {
                    println("exception $e caught")
                }
            }
        }
    }

    @Test
    fun exceptionNotHandlingInAsync() {
        runBlocking {
            launch {
                val request1 = async {
                    delay(100)
                    throw Exception("OOOPS")
                }
                val request2 = async {
                    delay(100)
                    throw Exception("OOOPS")
                }
                launch {
                    val result = try {
                        request1.await() as Int + request2.await() as Int
                    } catch (e: Exception) {
                        println("exception $e caught")
                    }
                    withContext(Dispatchers.Main) {
                        println("in with context")
                    }
                }
            }
        }
    }

    @Test
    fun exceptionHandlingInAsync() {
        runBlocking {
            launch {
                launch {
                    val result = try {
                        coroutineScope {
                            delay(1000)
                            val request1 = async { throw Exception("AAAAA!") }
                            val request2 = async { throw Exception("AAAAA!") }
                            return@coroutineScope request1.await() as Int + request2.await() as Int
                        }
                    } catch (e: Exception) {
                        println("exception $e caught")
                    }
                    withContext(Dispatchers.Unconfined) {
                        println("in with context")
                    }
                }
            }
        }
    }

    @Test
    fun exceptionHandlingInAsync2() {
        runBlocking {
            launch {
                val request1 = async { kotlin.runCatching { throw Exception("AAAAA!") }.getOrDefault(1) }
                val request2 = async {
                    kotlin.runCatching {
                        delay(1000)
                        return@runCatching 2
                    }.getOrDefault(1)
                }
                launch {
                    val result1 = request1.await()
                    val result2 = request2.await()
                    System.out.println(result1 + result2)
                }
            }
        }
    }

    @Test
    fun exceptionHandlingInAsyncExample() {
        val result = runBlocking {

            supervisorScope {
                val request1 = async { throw Exception("AAAAA!") }
                val request2 = async {
                    delay(1000)
                    return@async 2
                }
                request1.runCatching { await() }.getOrDefault(1) + request2.runCatching { await() }.getOrDefault(1)
            }

        }
        System.out.println(result)
    }

    @Test
    fun exceptionInLaunch() {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        scope.launch { // отменяет детей и сам обрабатывает exception, так как прокидывать больше некуда
            launch {
                delay(100)
                println("in launch #1")
            }
            launch {
                println("in launch #2")
                launch {
                    println("in launch #3")
                    throw Exception("AAAAA!!")
                }
            }
        }
        Thread.sleep(1000)
    }

    @Test
    fun exceptionInAsync() {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        scope.async { // отменяет детей, но сам обрабатывать не умеет (но умеет прокидывать, но здесь уже некому, так как не парента), поэтому тут не падает
            launch {
                delay(100)
                println("in launch #1")
            }
            launch {
                println("in launch #2")
                launch {
                    println("in launch #3")
                    throw Exception("AAAAA!!")
                }
            }
        }
        Thread.sleep(1000)
    }

    @Test
    fun exceptionInAsyncInLaunch() {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        scope.launch {
            async { // отменяет детей, но сам обрабатывать не умеет (но умеет прокидывать)
                launch {
                    delay(100)
                    println("in launch #1")
                }
                launch {
                    println("in launch #2")
                    launch {
                        println("in launch #3")
                        throw Exception("AAAAA!!")
                    }
                }
            }
        }
        Thread.sleep(1000)
    }

    @Test
    fun exceptionInAsyncWhenAwaitCalled() {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        val result =
            scope.async { // отменяет детей, но сам обрабатывать не умеет (но умеет прокидывать, но здесь уже некому, так как нет парента), поэтому тут не падает
                launch {
                    delay(100)
                    println("in launch #1")
                }
                launch {
                    println("in launch #2")
                    launch {
                        println("in launch #3")
                        throw Exception("AAAAA!!")
                    }
                }
            }
        Thread.sleep(1000)
        runBlocking {
            try {
                result.await()
            } catch (e: java.lang.Exception) {
                println("exception $e caught")
            }
        }
    }

    @Test
    fun launchAndAsync() {
        val scope = CoroutineScope(CoroutineName("test"))
        scope.launch {
            println("LOADING....")
            testCall()
            println("STOP LOADING!!!")
        }

        Thread.sleep(10000)
    }

    private suspend fun testCall(): Unit {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        // withContext(Dispatchers.Unconfined) {
        val deferredResult = scope.async {
            println("in async")
            loadSmth()
        }
        scope.launch {
            println("in launch")
            calculateSmth1()
            calculateSmth2()
            deferredResult.await().also { sendSmth() }
        }
        println("am ende")
        deferredResult.await()

        //     return@withContext deferredResult.await()
        // }
    }

    @Test
    fun testLaunchAndAsync(): Unit = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))

        withContext(Dispatchers.Unconfined) {
            println("Before")
            val deferredResult = async {
                println("in async")
                loadSmth()
            }
                .also {
                    launch {
                        println("in launch")
                        calculateSmth1()
                        calculateSmth2()
                        sendSmth()
                        // deferredResult.await().also { sendSmth() }
                    }
                }
            println("After")
        }
    }

    private suspend fun loadSmth() {
        println("in loadSmth")
        delay(1000)
        println("in loadSmth end")
    }

    private suspend fun calculateSmth1() {
        println("in calculateSmth1")
        delay(4000)
    }

    private suspend fun calculateSmth2() {
        println("in calculateSmth2")
        delay(2000)
    }

    private suspend fun sendSmth() {
        println("in sendSmth")
        delay(2000)
    }

    @Test
    fun supervisorJobWithException() {
        val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            println("in launch #1")
            throw Exception("aaaa")
        }
        scope.launch {
            println("in launch #2")
        }
    }

    @Test
    fun supervisorJobWithCancel() {
        val scope = CoroutineScope(SupervisorJob())
        scope.launch {
            println("in launch #1")
        }
        scope.launch {
            println("in launch #2")
        }

        scope.cancel()

        scope.launch {
            println("in launch #3")
        }
    }

    private suspend fun suspendFun() {
        println("in suspend fun")
    }

    @Test
    fun supervisorJobForTwoLaunch() {
        val exceptionHandler =
            CoroutineExceptionHandler { _, throwable -> println(throwable.localizedMessage.orEmpty()) }
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            launch {
                // #1
                delay(100)
                println("in launch #1")
            }
            supervisorScope {
                launch {
                    // #2
                    delay(100)
                    println("in launch #2")
                }
                launch(exceptionHandler) {
                    // #3
                    println("in launch #3")
                    throw Exception("aaaa")
                }
            }
        }
        Thread.sleep(1000)
    }

    @Test
    fun catchExceptionInExceptionHandler() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("exception \"${throwable.message}\" caught")
        }
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))

        scope.launch(exceptionHandler) {
            try {
                println("start coroutine")
                throw Exception("oops")
            } finally {
                println("in finally block")
            }
        }
    }
}


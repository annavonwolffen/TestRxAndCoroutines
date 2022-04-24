package com.annevonwolffen.testrxandcoroutines

import android.util.Log
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class RandomCoroutinesTests {

    @Test
    fun launchCoroutines() {
        runBlocking {
            var resultOne = "Hardstyle"
            var resultTwo = "Minions"
            println("Launch: Before")
            launch() {
                println("In first launch on thread ${Thread.currentThread().name}")
                resultOne = function1()
            }
            launch() {
                println("In second launch on thread ${Thread.currentThread().name}")
                resultTwo = function2()
            }
            println("Launch: After")
            val resultText = resultOne + resultTwo
            println("Launch: $resultText")
        }

        Thread.sleep(2000L)
    }

    suspend fun function1(): String {
        println("in function1")
        delay(1000L)
        val message = "function1"
        println("Launch: $message")
        return message
    }

    suspend fun function2(): String {
        println("in function2")
        delay(100L)
        val message = "function2"
        println("Launch: $message")
        return message
    }

    @Test
    fun testAsync(): Unit = runBlocking {
        // Launch a concurrent coroutine to check if the main thread is blocked
        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(100)
            }
        }
        println("Async: Before")
        val resultOne = async { function3() }
        val resultTwo = async { function4() }
        println("Async: After")
        val resultText = resultOne.await() + resultTwo.await()
        println("Async: $resultText")
    }

    @Test
    fun rxWithCoroutines() {
        val stringsObservable = Observable.fromArray("ALLES").observeOn(Schedulers.computation())
            .doOnNext { println(Thread.currentThread().name) }
        runBlocking {
            stringsObservable.asFlow()
                // .flowOn(Dispatchers.IO)
                .collect {
                    println(Thread.currentThread().name)
                    println(it)
                }
        }
        Thread.sleep(1000)
    }

    @Test
    fun cancellingJob() {
        val scope = CoroutineScope(Dispatchers.Unconfined + CoroutineName("test"))
        val job = scope.launch {
            delay(1000)
            println("coroutine completed")
        }
        scope.cancel()
    }

    @Test
    fun distributedSumTest() = runBlocking {
        distributedSum()
    }

    @Test
    fun doNotDoThisTest() = runBlocking {
        doNotDoThis()
    }

    @Test
    fun measureTime() {
        val time = measureTimeMillis {
            runBlocking {
                for (i in 1..2) {
                    launch(Dispatchers.Default) {
                        work(i)
                    }
                }
            }
        }
        println("Done in $time ms")
    }

    @Test
    fun measureTimeAlt() {
        val time = measureTimeMillis {
            runBlocking {
                for (i in 1..2) {
                    launch {
                        workWithContext(i)
                    }
                }
            }
        }
        println("Done in $time ms")
    }

    // https://www.thedevtavern.com/blog/posts/why-are-my-coroutines-slow/
    @Test
    fun blockingCallInCoroutine(): Unit = runBlocking {
        launch {
            println("coroutine #1")
            fun1()
            fun2()
        }

        launch {
            println("coroutine #2")
            delay(6000)
            launch {
                println("coroutine #3")
            }
        }

    }

    @Test
    fun blockingCallInCoroutineFixed(): Unit = runBlocking {
        launch {
            println("coroutine #1")
            fun1()
            withContext(Dispatchers.IO) {
                fun2()
            }
        }

        launch {
            println("coroutine #2")
            delay(6000)
            launch {
                println("coroutine #3")
            }
        }

    }
}

private suspend fun fun1() {
    println("in fun1")
    delay(5000)
}

private fun fun2() {
    println("in fun2")
    Thread.sleep(7000)
}

suspend fun function3(): String {
    delay(1000L)
    val message = "function3"
    println("Async: $message")
    return message
}

suspend fun function4(): String {
    delay(100L)
    val message = "function4"
    println("Async: $message")
    return message
}

suspend fun distributedSum() {
    val sum = AtomicInteger(0)

    // The coroutine scope acts like a parent that keeps track of
    // all the child coroutines created inside it
    coroutineScope {
        // Create 3 coroutines that compute the total sum concurrently
        repeat(3) {
            launch {
                coroutineContext.job
                println("coroutine #$it")
                val data: Int = fetchDataAsync()
                sum.addAndGet(data)
            }
        }
    }

    println("The final sum is: $sum")
}

private fun fetchDataAsync(): Int {
    return Random.nextInt()
}

suspend fun doNotDoThis() {
    val coroutineContext = coroutineContext
    CoroutineScope(coroutineContext).launch {
        println("outer: $coroutineContext, \ncurrent: ${this.coroutineContext}")
        println("I'm confused")
    }
}

fun work(i: Int) {
    Thread.sleep(1000)
    println("Work $i done")
}

suspend fun workWithContext(i: Int) = withContext(Dispatchers.Default) {
    Thread.sleep(1000)
    println("Work $i done")
}
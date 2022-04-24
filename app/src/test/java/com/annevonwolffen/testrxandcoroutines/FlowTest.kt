package com.annevonwolffen.testrxandcoroutines

import io.reactivex.Observable
import io.reactivex.ObservableSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

class FlowTest {

    @Test
    fun testSequence() {
        simpleSequence().forEach { value -> println(value) }
    }

    @Test
    fun testFlow() = runBlocking {
        // Launch a concurrent coroutine to check if the main thread is blocked
        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(100)
            }
        }
        // Collect the flow
        simpleFlow().collect { value -> println(value) }
        // simpleSequence().forEach { value -> println(value) }
    }

    @Test
    fun testFlow2() = runBlocking<Unit> {
        launch {
            simpleFlow().collect { value -> println(value) }
            println("Flow collected")
        }
    }

    @Test
    fun flowCancel() = runBlocking<Unit> {
        withTimeoutOrNull(250) { // Timeout after 250ms
            simpleFlow().collect { value -> println(value) }
        }
        println("Done")
    }

    @Test
    fun catch() = runBlocking {
        simpleFlowWithException()
            .catch { e -> emit("Caught $e") } // emit on exception
            .collect { value -> println(value) }
    }

    @Test
    fun combineFlows() = runBlocking {
        val flow1 = flowOf("Red", "Green", "Blue", "Black", "White")
        val flow2 = flowOf("Circle", "Square", "Triangle")
        flow1.zip(flow2) { first, second ->
            Pair(first, second)
        }
            .collect { println(it.toList()) }
    }

    @Test
    fun observableToFlow() {
        val stringsObservable: Observable<Int> = Observable.fromArray(1, 2, 3, 4, 5)
        val stringsFlow = stringsObservable.asFlow()
        runBlocking {
            stringsFlow
                .collect {
                    println(it) }
        }


    }

    fun simpleSequence(): Sequence<Int> = sequence { // sequence builder
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it
            yield(i) // yield next value
        }
    }

    fun simpleFlow(): Flow<Int> = flow { // flow builder
        for (i in 1..3) {
            delay(100) // pretend we are doing something useful here
            emit(i) // emit next value
        }
    }

    fun simpleFlowWithException(): Flow<String> =
        flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i) // emit next value
            }
        }

            .map { value ->
                check(value != 2) { "Crashed on $value" }
                "string $value"
            }
}
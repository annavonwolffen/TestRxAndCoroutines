package com.annevonwolffen.testrxandcoroutines

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import java.util.concurrent.TimeUnit

class RandomRxTests {
    @Test
    fun timer() {
        Single.timer(5L, TimeUnit.SECONDS)
            .map { "timer: $it" }
            .doFinally { println("timeout") }
            .subscribe { it -> println(it) }

        Thread.sleep(10000)
    }

    @Test
    fun interval() {
        Observable.interval(0, 2L, TimeUnit.SECONDS)
            .map { ": $it" }
            .doFinally { println("timeout") }
            .subscribe { println(it) }

        Thread.sleep(10000)
    }

    @Test
    fun intervalStream() {
        Observable.interval(0, 2L, TimeUnit.SECONDS)
            .flatMapSingle {
                exceptionOrValue(it.toInt())
                    .onErrorReturnItem(-1)
            }
            .subscribe { println(it) }

        Thread.sleep(10000)
    }

    @Test
    fun singleNull() {
        // Single.fromCallable {
            getSomeSingle()
                .map { convert(it)?.let { s -> listOf(s) } }
        // }
            // .flatMap { it }
            // .doOnError { println("ERROR!!: ${it}") }
            .subscribe({ println(it) }, { println("ERROR: $it")})


        Thread.sleep(1000)
    }

    private fun convert(s: String): String? {
        return null
    }

    private fun getSomeSingle(): Single<String> {
        return Single.just("some string")
    }

    private fun exceptionOrValue(value: Int): Single<Int> {
        return Single.fromCallable { if (value % 2 != 0) throw Exception("OOOPS!!") else value }
    }
}
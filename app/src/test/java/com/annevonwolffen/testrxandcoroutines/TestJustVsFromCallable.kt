package com.annevonwolffen.testrxandcoroutines

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TestJustVsFromCallable {
    @Test
    fun `test fromCallable`() {
        Observable.fromIterable(listOf(1, 2, 3, 4, 5))
            .flatMapMaybe { i ->
                Single.fromCallable { testJustTimeout(i) }
                    .subscribeOn(Schedulers.io())
                    .flatMap { it }
                    .timeout(3L, TimeUnit.SECONDS)
                    .toMaybe()
                    .doOnError { println("in flat map ${it.message}") }
                    .onErrorResumeNext(Maybe.empty())
            }
            .toList()
            .doOnError { println(it.message) }
            .onErrorReturnItem(emptyList())
            .subscribe({ onSuccess(it) }, {})

        Thread.sleep(6000L)
    }

    private fun testFromCallable(i: Int): Single<Int> {
        return Single.fromCallable {
            if (i == 2) {
                throw Exception(EXCEPTION)
            } else {
                i
            }
        }
    }

    private fun testFromCallableTimeout(i: Int): Single<Int> {
        return if (i == 2) {
            Single.fromCallable {
                Thread.sleep(5000L)
                i
            }
        } else {
            Single.fromCallable { i }
        }
    }

    private fun testJust(i: Int): Single<Int> {
        return if (i == 2) {
            throw Exception(EXCEPTION)
        } else {
            Single.just(i)
        }
    }

    private fun testJustTimeout(i: Int): Single<Int> {
        return if (i == 2) {
            Single.just(i).delay(5L, TimeUnit.SECONDS)
        } else if (i == 3) {
            throw Exception(EXCEPTION)
        } else {
            Single.just(i)
        }
    }

    private fun onSuccess(integers: List<Int>) {
        println(integers)
    }

    private companion object {
        const val EXCEPTION = "OOOPPS!!!1"
    }
}
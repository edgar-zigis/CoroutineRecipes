import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

fun main() {
    runSerial()
    //runSerialRxJava()
    //runConcurrent()
    //runConcurrentRxJava()
    //runWithoutCriticalProtection()
    //runWithCriticalProtection()
    //syncSomething()
    Thread.sleep(10000)
}

//  ****
//  Task definitions
//  ****

private suspend fun someAsyncTask(iteration: Int): Int {
    delay(3000L / iteration)
    println("[Coroutines] Executed iteration: $iteration")
    return iteration
}

private fun someAsyncTaskRxJava(iteration: Int): Single<Int> {
    return Single.timer(3000L / iteration, TimeUnit.MILLISECONDS).map {
        println("[RxJava] Executed iteration: $iteration")
        iteration
    }
}

//  ****
//  Serial tasks
//  ****

private fun runSerial() = GlobalScope.launch(Dispatchers.IO) {
    async { someAsyncTask(2) }.await()
    async { someAsyncTask(1) }.await()
    async { someAsyncTask(3) }.await()
}

private fun runSerialRxJava() {
    someAsyncTaskRxJava(2).flatMap {
        someAsyncTaskRxJava(1)
    }.flatMap {
        someAsyncTaskRxJava(3)
    }
        .subscribeOn(Schedulers.io())
        .subscribe()
}

//  ****
//  Concurrent tasks
//  ****

private fun runConcurrent() = GlobalScope.launch(Dispatchers.IO) {
    awaitAll(
        async { someAsyncTask(2) },
        async { someAsyncTask(1) },
        async { someAsyncTask(3) }
    )
}

private fun runConcurrentRxJava() {
    Observable.merge(
        someAsyncTaskRxJava(2).toObservable(),
        someAsyncTaskRxJava(1).toObservable(),
        someAsyncTaskRxJava(3).toObservable()
    )
        .subscribeOn(Schedulers.io())
        .subscribe()
}

//  ****
//  Multithreading
//  ****

private var sharedValue = 0

private fun runWithoutCriticalProtection() = GlobalScope.launch {
    someErrorProneThreadLoop {
        sharedValue++
    }
    println("Counter = $sharedValue")
}

private fun runWithCriticalProtection() = GlobalScope.launch {
    val mutex = Mutex()
    someErrorProneThreadLoop {
        mutex.withLock {
            sharedValue++
        }
    }
    println("Counter = $sharedValue")
}

private suspend fun someErrorProneThreadLoop(action: suspend () -> Unit) {
    val n = 100
    val k = 100
    val time = measureTimeMillis {
        coroutineScope {
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}

//  ****
//  Synchronization examples
//  ****

var syncDeferred: Deferred<Boolean>? = null
val syncMutex = Mutex()

private suspend fun synchronizeSomething(value: Int): Deferred<Boolean> = coroutineScope {
    syncMutex.withLock {
        syncDeferred?.let {
            return@coroutineScope it
        }
        return@coroutineScope async {
            delay(1000)
            println("I return things nicely $value")
            syncDeferred = null
            true
        }.also {
            syncDeferred = it
        }
    }
}

private fun syncSomething() = GlobalScope.launch {
    launch {
        synchronizeSomething(1).await()
    }
    launch {
        synchronizeSomething(2).await()
    }
    launch {
        delay(1500)
        synchronizeSomething(3).await()
    }
}
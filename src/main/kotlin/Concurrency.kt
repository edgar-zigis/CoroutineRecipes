
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/*
    Concurrency handling, serial & concurrent tasks, critical sections,
    synchronization handling
 */

fun main() {
    runSerial()
    //runSerialRxJava()
    //runConcurrent()
    //runConcurrentRxJava()
    //runWithoutCriticalProtection()
    //runWithCriticalProtection()
    //syncSomething()
    //Thread.sleep(10000)
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

/*
    This will execute all 3 tasks sequentially in specified order.
    It should complete all tasks in 5.5 seconds in order 2, 1, 3

    NOTE! This example was created to compare Deferred to Single.
    The correct (and simple) usage of serial tasks would be:

    private fun runSerial() = runBlocking {
        someAsyncTask(2)
        someAsyncTask(1)
        someAsyncTask(3)
    }
 */

private fun runSerial() = runBlocking {
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
    Thread.sleep(6000)
}

//  ****
//  Concurrent tasks
//  ****

/*
    This will execute all 3 tasks concurrently without specified order.
    It should complete all tasks in 3 seconds in order of 3, 2, 1

    NOTE! This example was created to compare Deferred to Single.
    We do not need any result here, so
    the correct (and simple) usage of concurrent tasks would be:

    private fun runConcurrent() = runBlocking {
        launch { someAsyncTask(2) }
        launch { someAsyncTask(1) }
        launch { someAsyncTask(3) }
    }
 */
private fun runConcurrent() = runBlocking {
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
    Thread.sleep(4000)
}

//  ****
//  Multithreading
//  ****

private var sharedValue = 0

/*
    This will output inconsistent Counter value
 */
@OptIn(DelicateCoroutinesApi::class)
private fun runWithoutCriticalProtection() = runBlocking {
    GlobalScope.launch {
        someErrorProneThreadLoop {
            sharedValue++
        }
        println("Counter = $sharedValue")
    }
    delay(500)
}

/*
    This will always output 10000
    Keep in mind not to nest mutexes like the synchronized blocks as it can result in easy deadlock
 */
@OptIn(DelicateCoroutinesApi::class)
private fun runWithCriticalProtection() = runBlocking {
    GlobalScope.launch {
        val mutex = Mutex()
        someErrorProneThreadLoop {
            mutex.withLock {
                sharedValue++
            }
        }
        println("Counter = $sharedValue")
    }
    delay(500)
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

/*
    The aim is not to call the synchronization code until the previous call is finished
    and reuse the result of the previous call
 */
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

/*
    This shall output:
    I return things nicely 1
    I return things nicely 3

    Removing mutex from synchronizeSomething will result in:
    I return things nicely 1
    I return things nicely 2
    I return things nicely 3
 */
private fun syncSomething() = runBlocking {
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
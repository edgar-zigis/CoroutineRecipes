import kotlinx.coroutines.*

/*
    Basic examples on differences between async/launch,
    cancellations etc.
 */

/*
    NOTE! All of the examples use runBlocking which blocks the current thread. I never recommend using this as the
    aim of the coroutines is to handle asynchronous tasks in the non-blocking way. But for testing purposes here
    we will use runBlocking. Alternative would be using CoroutineScope.launch with Thread.sleep().
    Normally, use CoroutineScope.launch for creating new tasks when you are working in the looper threads.
 */

fun main() {
    asyncVsLaunchSerial()
    //asyncVsLaunchConcurrent()
    //deferredCompletionInvocation()
    //deferredCancellation()
    //jobCompletionInvocation()
    //jobCancellation()
    //contextSwitching()
    //contextPoolTest()
}

//  ****
//  Task definitions
//  ****

private suspend fun someAsyncTask(iteration: Int): Int {
    delay(3000L / iteration)
    println("[Coroutines] Executed iteration: $iteration")
    return iteration
}

//  ****
//  Async vs Launch
//  ****

/*
    This shall output:
    [Coroutines] Executed iteration: 1
    [Coroutines] Executed iteration: 2
    [Coroutines] Executed iteration: 1
    [Coroutines] Executed iteration: 2

    as the await() for Deferred and join() for Job both suspend the current scope and wait for completion
 */
private fun asyncVsLaunchSerial() = runBlocking {
    async { someAsyncTask(1) }.await()
    launch { someAsyncTask(2) }.join()

    launch { someAsyncTask(1) }.join()
    async { someAsyncTask(2) }.await()
}

/*
    This shall output:
    [Coroutines] Executed iteration: 2
    [Coroutines] Executed iteration: 2
    [Coroutines] Executed iteration: 1
    [Coroutines] Executed iteration: 1

    as async without await() will perform in the same way like a simple launch
 */
private fun asyncVsLaunchConcurrent() = runBlocking {
    async { someAsyncTask(1) }
    launch { someAsyncTask(2) }

    launch { someAsyncTask(1) }
    async { someAsyncTask(2) }
}

/*
    The key differences are here:
    a -> is Integer
    b -> is Unit
    c -> is Job
    d -> is Deferred
 */
private fun asyncVsLaunchResults() = runBlocking {
    val a = async { someAsyncTask(1) }.await()
    val b = launch { someAsyncTask(2) }.join()

    val c = launch { someAsyncTask(1) }
    val d = async { someAsyncTask(2) }
}

//  ****
//  Async
//  ****

/*
    Example on how to know about Deferred completion
    exception will be null
 */
private fun deferredCompletionInvocation() = runBlocking {
    val someDeferred = async { someAsyncTask(1) }
    someDeferred.invokeOnCompletion {
        println("Deferred has been completed.")
    }
    someDeferred.await()
}

/*
    Example on how to know about Deferred completion when the exception is called
    exception will be CancellationException which is the default one when the cancel() is called
    You can supply any other exception extending the CancellationException
    cancelAndJoin() can be used as alternative if waiting for cancellation completion is needed
 */
private fun deferredCancellation() = runBlocking {
    val someDeferred = async { someAsyncTask(1) }
    someDeferred.cancel()
    someDeferred.invokeOnCompletion {
        println("Exception: $it")
    }
    someDeferred.await()
}

//  ****
//  Launch
//  ****

/*
    Example on how to know about Job completion
    exception will be null
 */
private fun jobCompletionInvocation() = runBlocking {
    val someJob = launch { someAsyncTask(1) }
    someJob.invokeOnCompletion {
        println("Job has been completed.")
    }
}

/*
    Example on how to know about Job completion when the exception is called
    exception will be CancellationException which is the default one when the cancel() is called
    You can supply any other exception extending the CancellationException.
    cancelAndJoin() can be used as alternative if waiting for cancellation completion is needed
 */
private fun jobCancellation() = runBlocking {
    val someJob = launch { someAsyncTask(1) }
    someJob.cancel()
    someJob.invokeOnCompletion {
        println("Exception: $it")
    }
}

//  ****
//  Contexts
//  ****

/*
    Just some generic context switching
 */
private fun contextSwitching() = runBlocking {
    val value = withContext(Dispatchers.IO) {
        someAsyncTask(2)
    }
    withContext(Dispatchers.Default) {
        println("Context value: $value")
    }
}

/*
    Deceiving example context switching
    In this case 1,2,3,4,5 will be printed every time, because when using the same IO thread pool
    The application does not switch threads. If there would be any other dispatcher (e.g. Unconfined)
    then the result might be different
 */
@OptIn(DelicateCoroutinesApi::class)
private fun contextPoolTest() = runBlocking {
    GlobalScope.launch(Dispatchers.IO) {
        println("1")
        withContext(Dispatchers.IO) {
            println("2")
            withContext(Dispatchers.IO) {
                println("3")
            }
            println("4")
        }
        println("5")
    }
    delay(100)
}
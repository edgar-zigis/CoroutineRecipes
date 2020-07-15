import kotlinx.coroutines.*

fun main() {
    asyncVsLaunchSerial()
    //asyncVsLaunchConcurrent()
    //deferredCompletionInvocation()
    //deferredCancellation()
    //jobCompletionInvocation()
    //jobCancellation()
    //contextSwitching()
    //contextPoolTest()
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

//  ****
//  Async vs Launch
//  ****

private fun asyncVsLaunchSerial() = GlobalScope.launch(Dispatchers.IO) {
    async { someAsyncTask(1) }.await()
    launch { someAsyncTask(2) }.join()

    launch { someAsyncTask(1) }.join()
    async { someAsyncTask(2) }.await()
}

private fun asyncVsLaunchConcurrent() = GlobalScope.launch(Dispatchers.IO) {
    async { someAsyncTask(1) }
    launch { someAsyncTask(2) }

    launch { someAsyncTask(1) }
    async { someAsyncTask(2) }
}

private fun asyncVsLaunchResults() = GlobalScope.launch(Dispatchers.IO) {
    val a = async { someAsyncTask(1) }.await()
    val b = launch { someAsyncTask(2) }.join()

    val c = launch { someAsyncTask(1) }
    val d = async { someAsyncTask(2) }
}

//  ****
//  Async
//  ****

private fun deferredCompletionInvocation() = GlobalScope.launch(Dispatchers.IO) {
    val someDeferred = async { someAsyncTask(1) }
    someDeferred.invokeOnCompletion {
        println("Deferred has been completed.")
    }
    someDeferred.await()
}

private fun deferredCancellation() = GlobalScope.launch(Dispatchers.IO) {
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

private fun jobCompletionInvocation() = GlobalScope.launch(Dispatchers.IO) {
    val someJob = launch { someAsyncTask(1) }
    someJob.invokeOnCompletion {
        println("Job has been completed.")
    }
}

private fun jobCancellation() = GlobalScope.launch(Dispatchers.IO) {
    val someJob = launch { someAsyncTask(1) }
    someJob.cancel()
    someJob.invokeOnCompletion {
        println("Exception: $it")
    }
}

//  ****
//  Contexts
//  ****

private fun contextSwitching() = GlobalScope.launch(Dispatchers.IO) {
    val value = withContext(Dispatchers.IO) {
        someAsyncTask(2)
    }
    withContext(Dispatchers.Default) {
        println("Context value: $value")
    }
}

private fun contextPoolTest() = GlobalScope.launch(Dispatchers.IO) {
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
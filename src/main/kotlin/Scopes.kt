import kotlinx.coroutines.*

/*
    Coroutine scope related examples
 */

fun main() {
    GlobalScope.launch {
        runDefaultScope()
        //runSupervisorScope()
        //testScopeCancellation()
        //testScopeCancellationWithSupervisor()
    }
    Thread.sleep(10000)
}

//  ****
//  Task definitions
//  ****

private suspend fun someAsyncTask(iteration: Int): Int {
    delay(3000L / iteration)
    println("[Coroutines] Executed iteration: $iteration")
    if (iteration % 2 == 0) {
        throw IllegalStateException()
    }
    return iteration
}

//  ****
//  Default scope
//  ****

/*
    Example with generic scope.
    All tasks after first exception will be cancelled
 */
private suspend fun runDefaultScope() = coroutineScope {
    launch {
        someAsyncTask(1)
    }
    launch {
        someAsyncTask(2)
    }
    launch {
        someAsyncTask(3)
    }
}

//  ****
//  Supervisor scope
//  ****

/*
    Example with supervisor scope.
    All tasks will be executed despite the exceptions
 */
private suspend fun runSupervisorScope() = supervisorScope {
    launch {
        someAsyncTask(1)
    }
    launch {
        someAsyncTask(2)
    }
    launch {
        someAsyncTask(3)
    }
}

//  ****
//  Scope cancellation
//  ****

/*
    Example with generic scope.
    Scope will be cancelled after method with iteration 3 will be executed.
    Method with iteration 1 won't be executed
 */
private suspend fun testScopeCancellation() {
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch {
        someAsyncTask(1)
    }
    scope.launch {
        someAsyncTask(3)
        scope.cancel()
    }
    scope.launch {
        someAsyncTask(5)
    }
}

/*
    Deceiving example with supplied supervisor job.
    No tasks will be cancelled. For proper cancellation the job itself should be cancelled.
 */
private suspend fun testScopeCancellationWithSupervisor() {
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch(job) {
        someAsyncTask(1)
    }
    scope.launch(job) {
        someAsyncTask(3)
        scope.cancel()
    }
    scope.launch(job) {
        someAsyncTask(5)
    }
}
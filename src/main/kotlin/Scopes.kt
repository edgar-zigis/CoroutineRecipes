import kotlinx.coroutines.*

/*
    Coroutine scope related examples
 */

fun main() {
    runDefaultScope()
    //runSupervisorScope()
    //testScopeCancellation()
    //testScopeCancellationWithSupervisor()
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
private fun runDefaultScope() = runBlocking {
    coroutineScope {
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
}

//  ****
//  Supervisor scope
//  ****

/*
    Example with supervisor scope.
    All tasks will be executed despite the exceptions
 */
private fun runSupervisorScope() = runBlocking {
    supervisorScope {
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
}

//  ****
//  Scope cancellation
//  ****

/*
    Example with generic scope.
    Scope will be cancelled after method with iteration 3 will be executed.
    Method with iteration 1 won't be executed
 */
private fun testScopeCancellation() = runBlocking {
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

    delay(1500)
}

/*
    Deceiving example with supplied supervisor job.
    No tasks will be cancelled. For proper cancellation the job itself should be cancelled.
 */
private fun testScopeCancellationWithSupervisor() = runBlocking {
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

    delay(4000)
}
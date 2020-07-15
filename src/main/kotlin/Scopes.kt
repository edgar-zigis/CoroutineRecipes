import kotlinx.coroutines.*

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
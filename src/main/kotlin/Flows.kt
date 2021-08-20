import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun main() {
    testBasicFlowOperators()
    //testBasicFlowCollect()
    //testBasicFlowDistinctCollection()
    //testBasicFlowDebounceCollection()
    //testBasicFlowTakeCollection()
    //testBasicFlowGargantuanCollection()
    //testSharedFlow()
    //testStateFlow()
    //testDummyFlowWithConcurrentEmissions()
    //testCallbackFlowWithConcurrentEmissions()
    //testChannelFlowWithConcurrentEmissions()
}

//  ****
//  Basic Flow operators
//  ****

fun basicFlow(): Flow<Int> = flow {
    for (i in 0 until 3) {
        emit(i)
        delay(100)
        emit(i)
    }
}.flowOn(Dispatchers.IO)

fun basicFlowWithException(): Flow<Int> = flow {
    for (i in 0 until 3) {
        emit(i)
        delay(100)
        emit(i)
        if (i == 1) throw Error()
    }
}.flowOn(Dispatchers.IO)

/*
    This will output 'started', 0 0 1 1 'finished' 'exception'
    if catch is not used -> process will crash and exception will be passed upstream
 */
fun testBasicFlowOperators() = runBlocking {
    basicFlowWithException().onEach {
        println("BasicFlow emitted value: $it")
    }.onStart {
        println("BasicFlow has started its work")
    }.onCompletion {
        println("BasicFlow has finished its work")
    }.catch {
        println("BasicFlow has an exception $it")
    }.launchIn(this)
}

/*
    This will output 0 0 1 1 and then process will crash
    Exception is passed upstream
 */
fun testBasicFlowCollect() = runBlocking {
    basicFlowWithException().collect {
        println("BasicFlowCollect emitted value: $it")
    }
}

/*
    This will output 0 1 2
    distinctUntilChanged ignores any duplicates
 */
fun testBasicFlowDistinctCollection() = runBlocking {
    basicFlow().distinctUntilChanged().collect {
        println("BasicFlowDistinctCollect emitted value: $it")
    }
}

/*
    This will output 0 1 2 2
    debounce is ignoring any values emitted faster than the previous one
 */
@OptIn(FlowPreview::class)
fun testBasicFlowDebounceCollection() = runBlocking {
    basicFlow().debounce(80).collect {
        println("BasicFlowDebounceCollect emitted value: $it")
    }
}

/*
    This will output 0 0 1
    take will just take first 3 elements from the flow and finish
 */
fun testBasicFlowTakeCollection() = runBlocking {
    basicFlow().take(3).collect {
        println("BasicFlowDebounceCollect emitted value: $it")
    }
}

/*
    This will output 0 0 1 1 2 2 sum 12
    This is just a simple operator display
 */
fun testBasicFlowGargantuanCollection() = runBlocking {
    val someWeirdSum = basicFlow().onEach {
        delay(200)
        println("Computing $it")
    }.filter {
        it % 2 == 0
    }.map {
        it + 2
    }.reduce { accumulator, value ->
        accumulator + value
    }
    println("SomeGargantuanSum: $someWeirdSum")
}

//  ****
//  Shared/State Flow
//  ****

/*
    replay represents the number of values emitted to new subscribers
 */
val sharedFlow = MutableSharedFlow<Int>(replay = 1)
val stateFlow = MutableStateFlow(69)

/*
    This will output 0 0 1 1 2 2
    Basically this acts like a channel and outputs everything
    Try removing delays though and it will output less values,
    because subscriber thread is too slow to listen to changes.
    SharedFlow is a hot replacement for BroadcastChannel
 */
fun testSharedFlow() = runBlocking {
    sharedFlow.asSharedFlow().onEach {
        println("SharedFlow value: $it")
    }.launchIn(this)

    for (i in 0 until 3) {
        sharedFlow.tryEmit(i)
        delay(100)
        sharedFlow.tryEmit(i)
        delay(100)
    }
}

/*
    This will output 0 1 2
    StateFlow is used to specifically work with states as it will only emit new states.
    StateFlow == SharedFlow with distinctUntilChanged applied.
    This is a perfect replacement for LiveData
 */
fun testStateFlow() = runBlocking {
    stateFlow.asStateFlow().onEach {
        println("StateFlow value: $it")
    }.launchIn(this)

    for (i in 0 until 3) {
        stateFlow.value = i
        delay(100)
        stateFlow.value = i
        delay(100)
    }
}

//  ****
//  Callback/Channel Flow
//  ****

/*
    The following methods represent an attempt to provide some callback values from different threads
    1. Using generic Flow
    2. CallbackFlow
    3. ChannelFlow
 */
@OptIn(DelicateCoroutinesApi::class)
fun dummyFlow(): Flow<Int> = flow {
    GlobalScope.launch {
        emit(1)
        emit(2)
    }
    delay(100)
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun callbackFlow(): Flow<Int> = callbackFlow {
    GlobalScope.launch {
        trySend(1)
        trySend(2)
        close()
    }
    delay(100)
    awaitClose { cancel() }
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun channelFlow(): Flow<Int> = channelFlow {
    GlobalScope.launch {
        trySend(1)
        trySend(2)
        close()
    }
    delay(100)
    awaitClose { cancel() }
}

/*
    This will crash. Because emission from any other or coroutine is not allowed.
    This can't be used for callbacks.
 */
fun testDummyFlowWithConcurrentEmissions() = runBlocking {
    dummyFlow().collect {
        println("DummyFlow value: $it")
    }
}

/*
    This will work fine and emit 1 2. CallbackFlow is designed to deal specifically with callbacks.
    Great replacement for custom Rx Callback Observables
 */
fun testCallbackFlowWithConcurrentEmissions() = runBlocking {
    callbackFlow().collect {
        println("CallbackFlow value: $it")
    }
}

/*
    This will work fine and emit 1 2, same like callbackFlow
    What's the difference? Try removing awaitClose from callbackFlow and it will crash
    channelFlow - won't. Basically the difference is very minor, callbackFlow is more secure
    awaitClose should be used to close any resources (like observers).
    Also, do not forget to call close() which notifies flow that there is nothing to listen to.
    close() should be called when the final action was performed.
 */
fun testChannelFlowWithConcurrentEmissions() = runBlocking {
    channelFlow().collect {
        println("ChannelFlow value: $it")
    }
}
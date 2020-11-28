import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun main() {
    //testBasicFlowOperators()
    //testBasicFlowCollect()
    //testBasicFlowDistinctCollection()
    //testBasicFlowDebounceCollection()
    //testSharedFlow()
    //testStateFlow()
    //testDummyFlowWithConcurrentEmissions()
    //testCallbackFlowWithConcurrentEmissions()
    //testChannelFlowWithConcurrentEmissions()
}

//  ****
//  Basic Flow operators
//  ****

fun testBasicFlowOperators() = runBlocking {
    basicFlow().onEach {
        println("BasicFlow emitted value: $it")
    }.onStart {
        println("BasicFlow has started its work")
    }.onCompletion {
        println("BasicFlow has finished its work")
    }.catch {
        println("BasicFlow has an exception $it")
    }.launchIn(this)
}

fun testBasicFlowCollect() = runBlocking {
    basicFlow().collect {
        println("BasicFlowCollect emitted value: $it")
    }
}

fun testBasicFlowDistinctCollection() = runBlocking {
    basicFlow().distinctUntilChanged().collect {
        println("BasicFlowDistinctCollect emitted value: $it")
    }
}

fun testBasicFlowDebounceCollection() = runBlocking {
    basicFlow().debounce(100).collect {
        println("BasicFlowDebounceCollect emitted value: $it")
    }
}

fun basicFlow(): Flow<Int> = flow {
    for (i in 0 until 3) {
        emit(i)
        emit(i)
        if (i == 3) throw Error()
    }
}

//  ****
//  Shared/State Flow
//  ****

val sharedFlow = MutableSharedFlow<Int>(replay = 1)
val stateFlow = MutableStateFlow(69)

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

fun testDummyFlowWithConcurrentEmissions() = runBlocking {
    dummyFlow().collect {
        println("DummyFlow value: $it")
    }
}

fun testCallbackFlowWithConcurrentEmissions() = runBlocking {
    callbackFlow().collect {
        println("CallbackFlow value: $it")
    }
}

fun testChannelFlowWithConcurrentEmissions() = runBlocking {
    channelFlow().collect {
        println("ChannelFlow value: $it")
    }
}

fun dummyFlow(): Flow<Int> = flow {
    GlobalScope.launch {
        emit(1)
    }
}

fun callbackFlow(): Flow<Int> = callbackFlow {
    GlobalScope.launch {
        offer(1)
        close()
    }
    awaitClose { cancel() }
}

fun channelFlow(): Flow<Int> = channelFlow {
    GlobalScope.launch {
        offer(1)
        close()
    }
    awaitClose { cancel() }
}
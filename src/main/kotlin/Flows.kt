import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

fun main() {
    testBasicFlowOperators()
    //testBasicFlowCollect()
    //testBasicFlowDistinctCollection()
    //testBasicFlowDebounceCollection()
    //testSharedFlow()
    //testStateFlow()
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

fun basicFlow() : Flow<Int> = flow {
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
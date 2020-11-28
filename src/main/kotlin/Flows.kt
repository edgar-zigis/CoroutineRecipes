import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

fun main() {
    testBasicFlowOperators()
    //testBasicFlowCollect()
    //testBasicFlowDistinctCollection()
    //testBasicFlowDebounceCollection()
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
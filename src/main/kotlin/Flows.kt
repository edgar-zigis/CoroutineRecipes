import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

fun main() {
    testBasicFlowOperators()
}

fun testBasicFlowOperators() = runBlocking {
    basicFlow().onEach {
        println("BasicFlow emitted value: $it")
    }.onCompletion {
        println("BasicFlow has finished its work")
    }.catch {
        println("BasicFlow has an exception $it")
    }.launchIn(this)
}

fun basicFlow() : Flow<Int> = flow {
    for (i in 0 until 5) {
        emit(i)
        if (i == 3) throw Error()
    }
}
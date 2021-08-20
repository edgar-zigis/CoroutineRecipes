import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.consumeEach

fun main() {
    simulateChannelWithoutBuffer()
    //simulateChannelWithBuffer()
    //simulateChannel()
    //simulateBroadcastChannel()
    //simulateConflatedBroadcastChannel()
}

//  ****
//  Buffering examples
//  ****

/*
    This method attempts to send 10 values, but it will send only single, because the capacity of the buffer is zero
 */
fun simulateChannelWithoutBuffer() = runBlocking {
    val channel = Channel<Int>(capacity = Channel.RENDEZVOUS)
    val sender = launch { // launch sender coroutine
        repeat(10) {
            println("Sending $it") // print before sending each element
            channel.send(it) // will suspend when buffer is full
        }
    }
    delay(1000)
    sender.cancel()
}

/*
    This method attempts to send 10 values, but it will send the 5 values because of the set buffer
 */
fun simulateChannelWithBuffer() = runBlocking {
    val channel = Channel<Int>(capacity = 4)
    val sender = launch {
        repeat(10) {
            println("Sending $it")
            channel.send(it)
        }
    }
    delay(1000)
    sender.cancel()
}

//  ****
//  Difference between Channel, BroadcastChannel, ConflatedBroadcastChannel
//  ****

/*
    This shall print 1 and 3, because capacity is 1 and the element 2 will be ignored
 */
fun simulateChannel() = runBlocking {
    val channel = Channel<Int>(capacity = 1)

    launch {
        channel.trySend(1)
        channel.trySend(2)
    }

    for (i in 0..1) {
        launch {
            channel.consumeEach {
                println("Receiving something $it")
            }
        }
    }

    launch {
        channel.offer(3)
    }
}

/*
    This shall print 3 and 3, because subscriber started listening after the values were sent to the channel
    This works in the same way like RxJava PublishSubject
 */
@OptIn(ObsoleteCoroutinesApi::class)
fun simulateBroadcastChannel() = runBlocking {
    val channel = BroadcastChannel<Int>(capacity = 1)

    launch {
        channel.trySend(1)
        channel.trySend(2)
    }

    for (i in 0..1) {
        launch {
            channel.consumeEach {
                println("Receiving something $it")
            }
        }
    }

    launch {
        channel.trySend(3)
    }
}

/*
    This shall print 2 and 3, because after subscription it will always emit the last value which was pushed to
    the channel first.
    This works in the same way like RxJava BehaviourSubject
 */
@OptIn(ObsoleteCoroutinesApi::class)
fun simulateConflatedBroadcastChannel() = runBlocking {
    val channel = ConflatedBroadcastChannel<Int>()

    launch {
        channel.trySend(1)
        channel.trySend(2)
    }

    launch {
        channel.consumeEach {
            println("Receiving something $it")
        }
    }

    launch {
        channel.trySend(3)
    }
}
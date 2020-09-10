package mi.lab

import kotlinx.coroutines.*
import mi.lab.tasks.TaskFactory
import mi.lab.tasks.TaskFactoryCache
import org.junit.Assert.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class TaskFactoryCacheTest {
    private val taskFactoryCache = TaskFactoryCache()

    @Test
    fun justWrap() {
        val wrapped = taskFactoryCache.wrap(buildTaskFactory { it * 2 })

        runBlocking {
            assertEquals(4, wrapped.compute(this, 2))
        }
    }

    @RepeatedTest(10)
    @Test
    fun callShouldBeCached() {
        val firstLock = CompletableDeferred<Unit>()
        val secondLock = CompletableDeferred<Unit>()
        val counter = AtomicInteger(0)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            firstLock.await()
            counter.incrementAndGet()
            secondLock.await()
            it * 2
        })

        runBlocking {
            val firstTask = GlobalScope.async { wrapped.compute(this, 3) }
            val secondTask = GlobalScope.async { wrapped.compute(this, 3) }
            firstLock.complete(Unit)
            delay(100)
            secondLock.complete(Unit)
            assertEquals(6, firstTask.await())
            assertEquals(6, secondTask.await())
            assertEquals(1, counter.get())
        }
    }

    @RepeatedTest(10)
    @Test
    fun cacheShouldBeClearedAfterCall() = runBlocking {
        val counter = AtomicInteger(0)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            counter.incrementAndGet()
            it * 2
        })


        assertEquals(8, coroutineScope { wrapped.compute(this, 4) })
        //cleaning is also async
        delay(100)
        assertEquals(8, coroutineScope { wrapped.compute(this, 4) })
        assertEquals(2, counter.get())
    }

    @RepeatedTest(10)
    @Test
    fun taskShouldBeCanceledIfEveryClientIsCanceled() = runBlocking {
        val firstLock = CompletableDeferred<Unit>()
        val secondLock = CompletableDeferred<Unit>()
        val canceled = AtomicBoolean(false)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            try {
                firstLock.await()
                it * 2
            } catch (e: CancellationException) {
                canceled.set(true)
                throw e
            } finally {
                secondLock.complete(Unit)
            }
        })

        val firstTask = GlobalScope.async {
            wrapped.compute(this, 6)
        }
        val secondTask = GlobalScope.async {
            wrapped.compute(this, 6)
        }
        firstTask.cancel()
        secondTask.cancel()
        //cancelling is also async
        delay(100)

        firstLock.complete(Unit)

        secondLock.await()
        assertTrue(canceled.get())
    }

    @RepeatedTest(10)
    @Test
    fun taskShouldNotBeCanceledIfOneClientIsCanceled() = runBlocking {
        val firstLock = CompletableDeferred<Unit>()
        val secondLock = CompletableDeferred<Unit>()
        val canceled = AtomicBoolean(false)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            try {
                firstLock.await()
                it * 2
            } catch (e: CancellationException) {
                canceled.set(true)
                throw e
            } finally {
                secondLock.complete(Unit)
            }
        })


        val firstTask = GlobalScope.async {
            wrapped.compute(this, 7)
        }
        val secondTask = GlobalScope.async {
            wrapped.compute(this, 7)
        }
        firstTask.cancel()
        //cancelling is also async
        delay(100)

        firstLock.complete(Unit)

        secondLock.await()
        assertEquals(14, secondTask.await())
        assertFalse(canceled.get())
    }

    private fun buildTaskFactory(block: suspend (Int) -> Int) = object : TaskFactory<Int, Int> {
        override suspend fun compute(coroutineScope: CoroutineScope, input: Int): Int =
                withContext(coroutineScope.coroutineContext) { block(input) }
    }
}

package mi.lab

import kotlinx.coroutines.*
import mi.lab.tasks.TaskFactory
import mi.lab.tasks.TaskFactoryCache
import org.junit.Assert.*
import org.junit.jupiter.api.Test
import org.springframework.test.annotation.Repeat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class TaskFactoryCacheTest {
    private val taskFactoryCache = TaskFactoryCache()

    @Test
    fun justWrap() {
        val wrapped = taskFactoryCache.wrap(buildTaskFactory { it * 2 })

        runBlocking {
            assertEquals(4, wrapped.computeAsync(2).await())
        }
    }

    @Repeat(10)
    @Test
    fun callShouldBeCached() {
        val lock = CompletableDeferred<Unit>()
        val counter = AtomicInteger(0)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            lock.await()
            counter.incrementAndGet()
            it * 2
        })

        runBlocking {
            val firstTask = wrapped.computeAsync(3)
            val secondTask = wrapped.computeAsync(3)
            lock.complete(Unit)
            assertEquals(6, firstTask.await())
            assertEquals(6, secondTask.await())
            assertEquals(1, counter.get())
        }
    }

    @Repeat(10)
    @Test
    fun cacheShouldBeClearedAfterCall() {
        val counter = AtomicInteger(0)

        val wrapped = taskFactoryCache.wrap(buildTaskFactory {
            counter.incrementAndGet()
            it * 2
        })

        runBlocking {
            assertEquals(8, wrapped.computeAsync(4).await())
            //cleaning is also async
            delay(50)
            assertEquals(8, wrapped.computeAsync(4).await())
            assertEquals(2, counter.get())
        }
    }

    @Repeat(10)
    @Test
    fun taskShouldBeCanceledIfEveryClientIsCanceled() {
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

        runBlocking {
            val firstTask = wrapped.computeAsync(6)
            val secondTask = wrapped.computeAsync(6)
            firstTask.cancel()
            secondTask.cancel()
            //cancelling is also async
            delay(50)

            firstLock.complete(Unit)

            secondLock.await()
            assertTrue(canceled.get())
        }
    }

    @Repeat(10)
    @Test
    fun taskShouldNotBeCanceledIfOneClientIsCanceled() {
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

        runBlocking {
            val firstTask = wrapped.computeAsync(7)
            val secondTask = wrapped.computeAsync(7)
            firstTask.cancel()
            //cancelling is also async
            delay(50)

            firstLock.complete(Unit)

            secondLock.await()
            assertFalse(canceled.get())
            assertEquals(14, secondTask.await())
        }
    }

    private fun buildTaskFactory(block: suspend CoroutineScope.(Int) -> Int) = object : TaskFactory<Int, Int> {
        override fun computeAsync(input: Int) = GlobalScope.async {
            block.invoke(this, input)
        }
    }
}

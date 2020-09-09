package mi.lab.tasks

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PreDestroy

@Component
class TaskFactoryCache {
    private val cache: MutableMap<Any, Pair<Deferred<Any>, AtomicInteger>> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, R : Any> wrap(taskFactory: TaskFactory<T, R>) = object : TaskFactory<T, R> {
        override fun computeAsync(input: T): Deferred<R> {
            val (fromCache, counter) = cache.getOrPut(input) {
                taskFactory.computeAsync(input) to AtomicInteger(0)
            } as Pair<Deferred<R>, AtomicInteger>
            counter.incrementAndGet()

            val forClient = GlobalScope.async {
                fromCache.await()
            }
            forClient.invokeOnCompletion {
                if (counter.decrementAndGet() == 0) {
                    fromCache.cancel()
                    cache.remove(input)
                }
            }
            return forClient
        }
    }

    @PreDestroy
    fun tearDown() {
        cache.values.map { it.first }.forEach { it.cancel() }
    }
}

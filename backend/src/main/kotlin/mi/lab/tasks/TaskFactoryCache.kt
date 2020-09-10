package mi.lab.tasks

import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PreDestroy
import kotlin.coroutines.EmptyCoroutineContext

@Component
class TaskFactoryCache {
    private val cache: MutableMap<Any, Pair<Deferred<Any>, AtomicInteger>> = ConcurrentHashMap()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any, R : Any> wrap(taskFactory: TaskFactory<T, R>) = object : TaskFactory<T, R> {
        override suspend fun compute(coroutineScope: CoroutineScope, input: T): R {
            val (fromCache, counter) = cache.getOrPut(input) {
                CoroutineScope(EmptyCoroutineContext).async {
                    taskFactory.compute(this, input)
                } to AtomicInteger(0)
            } as Pair<Deferred<R>, AtomicInteger>
            counter.incrementAndGet()

            coroutineScope.coroutineContext[Job.Key]!!.invokeOnCompletion {
                if (counter.decrementAndGet() == 0) {
                    fromCache.cancel()
                    cache.remove(input)
                }
            }
            return withContext(coroutineScope.coroutineContext) { fromCache.await() }
        }
    }

    @PreDestroy
    fun tearDown() {
        cache.values.map { it.first }.forEach { it.cancel() }
    }
}

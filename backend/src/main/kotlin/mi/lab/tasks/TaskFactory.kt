package mi.lab.tasks

import kotlinx.coroutines.Deferred

interface TaskFactory<T : Any, R : Any> {
    fun computeAsync(input: T): Deferred<R>
}

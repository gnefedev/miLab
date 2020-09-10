package mi.lab.tasks

import kotlinx.coroutines.CoroutineScope

interface TaskFactory<T : Any, R : Any> {
    suspend fun compute(coroutineScope: CoroutineScope, input: T): R
}

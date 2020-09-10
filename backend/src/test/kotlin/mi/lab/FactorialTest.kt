package mi.lab

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mi.lab.process.ProcessRunner
import mi.lab.tasks.FactorialTask
import mi.lab.tasks.TaskFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.comparesEqualTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext

@SpringBootTest
class FactorialTest @Autowired constructor(
        private val factorialTaskFactory: TaskFactory<FactorialTask.Params, BigDecimal>,
        private val processRunner: ProcessRunner
) {
    @Test
    fun factorial() = runBlocking {
        val params = FactorialTask.Params(123L, 1231312.toBigDecimal())

        val result = factorialTaskFactory.compute(this, params)

        assertThat(result, comparesEqualTo(418528.toBigDecimal()));
    }

    @Test
    fun factorialCancelling() = runBlocking {
        val params = FactorialTask.Params(32345L, 1231312123.toBigDecimal())

        val ended = AtomicBoolean(false)

        val result = CoroutineScope(EmptyCoroutineContext).async {
            factorialTaskFactory.compute(this, params)
            ended.set(false)
        }

        result.cancel()
        delay(100)
        assertFalse(ended.get())
        assertEquals(0, processRunner.processCounter.get())
    }
}

package mi.lab

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import mi.lab.tasks.FactorialTask
import mi.lab.tasks.TaskFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.comparesEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class FactorialTest @Autowired constructor(
        private val factorialTaskFactory: TaskFactory<FactorialTask.Params, BigDecimal>
) {
    @Test
    fun factorial() {
        val params = FactorialTask.Params(123L, 1231312.toBigDecimal())

        val result: Deferred<BigDecimal> = factorialTaskFactory.computeAsync(params)

        runBlocking {
            assertThat(result.await(), comparesEqualTo(418528.toBigDecimal()));
        }
    }
}

package mi.lab

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mi.lab.tasks.FactorialTask
import mi.lab.tasks.TaskFactory
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@SpringBootTest
class Benchmark @Autowired constructor(
        private val factorialTaskFactory: TaskFactory<FactorialTask.Params, BigDecimal>
) {
    private companion object : KLogging()

    //    @Test
    fun runManyRequests() {
        runBlocking {
            val cpuCount = Runtime.getRuntime().availableProcessors()

            //cpuCount * 6 takes 12293 ms
            val params = (0..cpuCount * 6)
                    .map { i -> FactorialTask.Params(32345L + i, BigDecimal.valueOf(2).pow(512) + BigDecimal.ONE) }

            val begin = Instant.now()

            params.map { async { factorialTaskFactory.compute(this, it) } }
                    .map { it.await() }

            val duration = Duration.between(begin, Instant.now())

            logger.info { "takes ${duration.toMillis()} ms" }
        }
    }
}

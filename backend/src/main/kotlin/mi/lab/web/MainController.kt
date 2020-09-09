package mi.lab.web

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.asMono
import mi.lab.tasks.FactorialTask
import mi.lab.tasks.TaskFactory
import mu.KLogging
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.math.BigDecimal
import javax.validation.constraints.Min


@Validated
@RestController
class MainController(
        private val factorialTaskFactory: TaskFactory<FactorialTask.Params, BigDecimal>
) {
    private companion object : KLogging()

    @ExperimentalCoroutinesApi
    @GetMapping("{A}/{B}")
    fun factorial(
            @Min(1) @PathVariable("A") a: Long,
            @Min(1) @PathVariable("B") b: BigDecimal
    ): Mono<BigDecimal> {
        logger.info { "handling factorial A: $a, B: $b" }
        val task = factorialTaskFactory.computeAsync(FactorialTask.Params(a, b))
        return task.asMono(GlobalScope.coroutineContext).doOnCancel { task.cancel() }
    }
}

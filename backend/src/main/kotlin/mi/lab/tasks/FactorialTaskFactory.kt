package mi.lab.tasks

import kotlinx.coroutines.Deferred
import mi.lab.process.ProcessDescription
import mi.lab.process.ProcessRunner
import java.math.BigDecimal

class FactorialTaskFactory(
        private val processRunner: ProcessRunner
) : TaskFactory<FactorialTask.Params, BigDecimal> {
    override fun computeAsync(input: FactorialTask.Params): Deferred<BigDecimal> =
            processRunner.computeInProcessAsync { onComplete ->
                ProcessDescription(
                        "mi.lab.FactorialMainKt",
                        "1g",
                        onProcessStart = {
                            it.write("${input.a}\n")
                            it.write("${input.b}\n")
                        },
                        onOutput = { line, _ ->
                            onComplete(line.toBigDecimal())
                        }
                )
            }

}

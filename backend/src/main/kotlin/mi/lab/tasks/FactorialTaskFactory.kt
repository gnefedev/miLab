package mi.lab.tasks

import kotlinx.coroutines.CoroutineScope
import mi.lab.process.ProcessDescription
import mi.lab.process.ProcessRunner
import java.math.BigDecimal

class FactorialTaskFactory(
        private val processRunner: ProcessRunner
) : TaskFactory<FactorialTask.Params, BigDecimal> {
    override suspend fun compute(coroutineScope: CoroutineScope, input: FactorialTask.Params): BigDecimal =
            processRunner.computeInProcess(coroutineScope) { onComplete ->
                ProcessDescription(
                        "mi.lab.FactorialMainKt",
                        "256m",
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

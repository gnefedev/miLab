package mi.lab.process

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.*
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

@Component
class ProcessRunner {
    private companion object : KLogging()

    private val executorService = Executors.newSingleThreadExecutor();

    @PreDestroy
    fun shutdown() {
        executorService.shutdown()
    }

    fun <R> computeInProcessAsync(processDescription: (onComplete: (R) -> Unit) -> ProcessDescription): Deferred<R> {
        val result = CompletableDeferred<R>()

        val process = runProcess(processDescription { result.complete(it) })

        result.invokeOnCompletion {
            if (process.isAlive) {
                logger.warn { "canceling process" }
                process.destroy()
            }
        }

        return result
    }

    private fun runProcess(processDescription: ProcessDescription): Process {
        val builder: ProcessBuilder = processBuilder(processDescription)
        val process = builder.start()


        val systemIn = BufferedWriter(OutputStreamWriter(process.outputStream))
        val systemOut = BufferedReader(InputStreamReader(process.inputStream))

        executorService.submit(Runnable {
            while (true) {
                val nextLine = systemOut.readLine() ?: return@Runnable
                processDescription.onOutput(nextLine, systemIn)
                systemIn.flush()
            }
        })

        processDescription.onProcessStart(systemIn)
        systemIn.flush()

        return process;
    }

    private fun processBuilder(processDescription: ProcessDescription): ProcessBuilder {
        val javaHome = System.getProperty("java.home")
        val javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java"
        val classpath = System.getProperty("java.class.path")
        val builder = ProcessBuilder(
                javaBin, "-cp", classpath,
                "-Xmx${processDescription.memoryLimit}",
                "-Dfile.encoding=UTF8",
                processDescription.clazzName
        )
        builder.redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        return builder
    }
}

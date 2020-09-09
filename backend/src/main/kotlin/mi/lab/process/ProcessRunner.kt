package mi.lab.process

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.*
import java.lang.Runnable
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val cpuCount = Runtime.getRuntime().availableProcessors()

@Component
class ProcessRunner {
    private companion object : KLogging()

    private val executorService = Executors.newSingleThreadExecutor();
    private val channel = Channel<Pair<ProcessDescription, CompletableDeferred<*>>>(capacity = cpuCount)

    @PreDestroy
    fun shutdown() {
        executorService.shutdown()
        channel.close()
    }

    @PostConstruct
    fun init() {
        repeat(cpuCount) {
            GlobalScope.launch {
                for ((processDescription, result) in channel) {
                    if (result.isCancelled) continue

                    try {
                        val process = runProcess(processDescription)

                        result.invokeOnCompletion {
                            if (process.isAlive) {
                                logger.warn { "canceling process" }
                                process.destroy()
                            }
                        }
                        result.join()
                    } catch (e: Exception) {
                        result.completeExceptionally(e)
                    }
                }
            }
        }
    }

    fun <R> computeInProcessAsync(processDescriptionSupplier: (onComplete: (R) -> Unit) -> ProcessDescription): Deferred<R> {
        val result = CompletableDeferred<R>()
        return GlobalScope.async {

            val processDescription = processDescriptionSupplier { result.complete(it) }

            channel.send(processDescription to result)

            result.await()
        }.also {
            it.invokeOnCompletion {
                result.cancel()
            }
        }
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

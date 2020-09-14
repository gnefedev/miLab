package mi.lab.process

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KLogging
import org.springframework.stereotype.Component
import java.io.*
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val cpuCount = Runtime.getRuntime().availableProcessors()

//trade of between latency and throughput
private val parallelism = cpuCount

@Component
class ProcessRunner {
    private companion object : KLogging()

    private val executorService = Executors.newSingleThreadExecutor();
    private val channel = Channel<suspend () -> Unit>()

    //only for tests. May be used for stats
    val processCounter = AtomicInteger(0)

    @PreDestroy
    fun shutdown() {
        executorService.shutdown()
        channel.close()
    }

    @PostConstruct
    fun init() {
        repeat(parallelism) {
            GlobalScope.launch {
                for (block in channel) {
                    block()
                }
            }
        }
    }

    fun <R> computeInProcessAsync(
            processDescriptionSupplier: (onComplete: (R) -> Unit) -> ProcessDescription
    ): Deferred<R> {
        val result = CompletableDeferred<R>()
        return GlobalScope.async {
            channel.send {
                if (result.isCancelled) return@send

                try {
                    val process = runProcess(processDescriptionSupplier { result.complete(it) })
                    processCounter.incrementAndGet()

                    result.invokeOnCompletion {
                        processCounter.decrementAndGet()
                        //it == null when job is ended normally, process will end normally
                        if (process.isAlive && it != null) {
                            logger.warn { "canceling process $it" }
                            process.destroy()
                        }
                    }
                    result.join()
                } catch (e: Exception) {
                    result.completeExceptionally(e)
                }
            }
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

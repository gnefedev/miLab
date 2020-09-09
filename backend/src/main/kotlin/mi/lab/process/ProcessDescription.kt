package mi.lab.process

import java.io.Writer

data class ProcessDescription(
        val clazzName: String,
        val memoryLimit: String,
        val onProcessStart: (systemIn: Writer) -> Unit,
        val onOutput: (line: String, systemIn: Writer) -> Unit
)

package mi.lab

import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal


fun main() {
    Thread.sleep(2000)
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val a = reader.readLine().toLong()
    val b = reader.readLine().toBigDecimal()
    println(factorial(a) % b)
}

private fun factorial(a: Long): BigDecimal {
    var result = BigDecimal.ONE
    for (i in 2..a) {
        result *= i.toBigDecimal()
    }
    return result
}

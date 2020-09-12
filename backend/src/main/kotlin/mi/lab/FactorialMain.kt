package mi.lab

import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger


fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val a = reader.readLine().toLong()
    val b = reader.readLine().toBigInteger()
    var result = BigInteger.ONE
    for (i in 2..a) {
        result = (result * i.toBigInteger()) % b
    }
    println(result)
}


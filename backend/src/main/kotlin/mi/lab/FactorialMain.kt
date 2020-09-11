package mi.lab

import com.google.common.math.BigIntegerMath
import java.io.BufferedReader
import java.io.InputStreamReader


fun main() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val a = reader.readLine().toInt()
    val b = reader.readLine().toBigInteger()
    println(BigIntegerMath.factorial(a) % b)
}

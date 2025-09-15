import kotlin.math.*
import kotlin.random.Random

data class Human(
    var fullName: String,
    var age: Int,
    var speed: Double,
    var x: Double = 0.0,
    var y: Double = 0.0
) {

    fun setPosition(nx: Double, ny: Double) { x = nx; y = ny }

    fun move(dt: Double = 1.0): Pair<Double, Double> {
        if (dt <= 0.0) return x to y
        val theta = Random.nextDouble(0.0, 2 * Math.PI)
        val distance = speed * dt
        val dx = distance * cos(theta)
        val dy = distance * sin(theta)
        x += dx
        y += dy
        return x to y
    }

    override fun toString(): String {
        return "Human(name='$fullName', age=$age, speed=${"%.2f".format(speed)}, x=${"%.3f".format(x)}, y=${"%.3f".format(y)})"
    }
}

fun main() {
    val numHumans = 22
    val simulationTimeSeconds = 5
    val dt = 1.0

    val humans = Array(numHumans) { i ->
        val speed = Random.nextDouble(0.5, 2.5)
        Human(
            fullName = "People ${i+1}",
            age = 18 + i,
            speed = speed
        )
    }

    println("Старт симуляции Random Walk: n=$numHumans, totalTime=${simulationTimeSeconds}s, dt=${dt}s\n")
    println("Начальные состояния:")
    humans.forEach { println(it) }
    println()

    var t = 0.0
    while (t < simulationTimeSeconds) {
        t += dt
        println("Время: ${t.toInt()} s")
        for (i in humans.indices) {
            val h = humans[i]
            h.move(dt)
            println("[$i] ${h.fullName}: pos=(${String.format("%.3f", h.x)}, ${String.format("%.3f", h.y)})")
        }
        println()
    }

    println("Итоговые состояния:")
    humans.forEach { println(it) }
}
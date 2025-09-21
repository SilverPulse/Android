import kotlin.math.*
import kotlin.random.Random

open class Human(
    var fullName: String,
    var age: Int,
    var speed: Double,
    var x: Double = 0.0,
    var y: Double = 0.0
) {

    open fun move(dt: Double = 1.0): Pair<Double, Double> {
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
        return "${this::class.simpleName}(name='$fullName', age=$age, speed=${"%.2f".format(speed)}, x=${"%.3f".format(x)}, y=${"%.3f".format(y)})"
    }
}


class Driver(
    fullName: String,
    age: Int,
    speed: Double,
    x: Double = 0.0,
    y: Double = 0.0
) : Human(fullName, age, speed, x, y) {


    override fun move(dt: Double): Pair<Double, Double> {
        val distance = speed * dt
        x += distance
        return x to y
    }
}

fun main() {
    val dt = 1.0
    val simulationTime = 5

    // Создаем 3 Human и 1 Driver
    val humans = List(3) { i ->
        Human("Human ${i+1}", 20 + i, Random.nextDouble(0.5, 2.0))
    }
    val driver = Driver("Driver 1", 30, 1.5)

    val allActors = humans + driver

    println("=== Начальные состояния ===")
    allActors.forEach { println(it) }

    println("\n=== Запуск симуляции ===\n")

    // Запуск потоков
    val threads = allActors.map { actor ->
        Thread {
            var t = 0.0
            while (t < simulationTime) {
                Thread.sleep((dt * 1000).toLong()) // имитируем времени
                t += dt
                actor.move(dt)
                println("[${Thread.currentThread().name}] $actor")
            }
        }
    }

    // Стартуем потоки
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    println("\n=== Итоговые состояния ===")
    allActors.forEach { println(it) }
}

import kotlin.math.*
import kotlin.random.Random

open class Human(
    val fullName: String,
    val age: Int,
    override var speed: Double,
    override var x: Double = 0.0,
    override var y: Double = 0.0
) : Movable {

    override fun move(dt: Double): Pair<Double, Double> {
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

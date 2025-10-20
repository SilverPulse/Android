import Movable
class Driver(
    val fullName: String,
    val age: Int,
    override var speed: Double,
    override var x: Double = 0.0,
    override var y: Double = 0.0
) : Movable {

    override fun move(dt: Double): Pair<Double, Double> {
        val distance = speed * dt
        x += distance   // прямолинейное движение вдоль X
        return x to y
    }

    override fun toString(): String {
        return "${this::class.simpleName}(name='$fullName', age=$age, speed=${"%.2f".format(speed)}, x=${"%.3f".format(x)}, y=${"%.3f".format(y)})"
    }
}

interface Movable {
    var speed: Double
    var x: Double
    var y: Double

    fun move(dt: Double = 1.0): Pair<Double, Double>
}

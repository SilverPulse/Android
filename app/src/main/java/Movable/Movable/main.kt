fun main() {
    val dt = 1.0
    val simulationTime = 5

    val humans = List(3) { i ->
        Human("Human ${i+1}", 20 + i, kotlin.random.Random.nextDouble(0.5, 2.0))
    }
    val driver = Driver("Driver 1", 30, 1.5)

    val allActors: List<Movable> = humans + driver

    println("=== Начальные состояния ===")
    allActors.forEach { println(it) }

    println("\n=== Запуск симуляции ===\n")

    val threads = allActors.map { actor ->
        Thread {
            var t = 0.0
            while (t < simulationTime) {
                Thread.sleep((dt * 1000).toLong())
                t += dt
                actor.move(dt)
                println("[${Thread.currentThread().name}] $actor")
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    println("\n=== Итоговые состояния ===")
    allActors.forEach { println(it) }
}

#Выполнил Шаимов Богдан ИКС-431
Описание
Проект реализует модель случайного блуждания (Random Walk) для объекта Particle в двумерной декартовой системе координат с использованием Kotlin.
Принцип работы
Объект Particle имеет текущие координаты ((x, y)) и скорость (v).На каждом шаге времени (\Delta t) выбирается случайное направление — угол (\theta), равномерно распределённый в диапазоне от (0) до (2\pi).Объект перемещается на расстояние:
[d = v \cdot \Delta t]
где:  

(v) — скорость объекта,  
(\Delta t) — шаг времени.

Формулы для координат
Координаты обновляются по формулам:
[x_{new} = x_{old} + d \cdot \cos(\theta)]
[y_{new} = y_{old} + d \cdot \sin(\theta)]
где:  

(\theta) — случайный угол,  
(x_{old}, y_{old}) — текущие координаты,  
(x_{new}, y_{new}) — новые координаты.

Реализация
Функция move в классе Particle отвечает за перемещение объекта.
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

Описание функции move

Параметр: dt: Double = 1.0 — шаг времени (в секундах). Если (\Delta t \leq 0), координаты не изменяются.
Логика:
Если (\Delta t \leq 0), возвращает текущие координаты ((x, y)).
Генерирует случайный угол (\theta) от (0) до (2\pi) с помощью Random.nextDouble.
Вычисляет расстояние: (d = v \cdot \Delta t).
Обновляет координаты: (x += d \cdot \cos(\theta)), (y += d \cdot \sin(\theta)).
Возвращает новые координаты ((x, y)) как Pair<Double, Double>.



Зависимости
Для работы функции требуются импорты:
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

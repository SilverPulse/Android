import java.util.Random;

public class Human {
    private String fullName;   // ФИО
    private int age;           // Возраст
    private double speed;      // Текущая скорость
    private double x;          // координата X
    private double y;          // координата Y

    private Random rnd = new Random();

    public Human(String fullName, int age, double speed, double x, double y) {
        this.fullName = fullName;
        this.age = age;
        this.speed = speed;
        this.x = x;
        this.y = y;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getX() { return x; }
    public double getY() { return y; }

    public void move() {
        double angle = rnd.nextDouble() * 2 * Math.PI;
        double dx = speed * Math.cos(angle);
        double dy = speed * Math.sin(angle);
        this.x += dx;
        this.y += dy;
    }

    @Override
    public String toString() {
        return String.format("%s (age=%d) pos=(%.2f, %.2f) v=%.2f",
                fullName, age, x, y, speed);
    }
}

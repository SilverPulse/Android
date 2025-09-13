import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int groupNumber = 22;
        int simTimeSeconds = 20;

        Human[] humans = new Human[groupNumber];
        Random rnd = new Random();

        for (int i = 0; i < groupNumber; i++) {
            String name = "Person_" + (i + 1);
            int age = 18 + i;
            double speed = 0.5 + rnd.nextDouble() * 1.5;
            humans[i] = new Human(name, age, speed, 0.0, 0.0);
        }

        for (int t = 0; t < simTimeSeconds; t++) {
            System.out.println("Time: " + t + "s");
            for (Human h : humans) {
                h.move();
                System.out.println(h);
            }
            System.out.println();

            try { Thread.sleep(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println("Simulation finished.");
    }
}

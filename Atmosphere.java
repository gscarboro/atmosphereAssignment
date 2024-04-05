import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

class Sensor extends Thread {
    private final ConcurrentLinkedQueue<Reading> queue;
    private final AtomicInteger currentTime;
    private final AtomicInteger reportCount;
    private final Random random = new Random();
    private final int id;

    public Sensor(ConcurrentLinkedQueue<Reading> queue, AtomicInteger currentTime, AtomicInteger reportCount, int id) {
        this.queue = queue;
        this.currentTime = currentTime;
        this.reportCount = reportCount;
        this.id = id;
    }

    @Override
    public void run() {
        // Use 5 as max reports/hours so it doesn't run forever
        while (!Thread.currentThread().isInterrupted() && reportCount.get() < 5) {
            int localTime = this.currentTime.get();
            // Different sensors (threads) take turns reading
            if (localTime % 8 == id) {  
                // Using random numbers for temps
                int temperature = random.nextInt(171) - 100;
                queue.add(new Reading(localTime, temperature));
                while (this.currentTime.get() == localTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}

class Reading {
    int time;
    int temperature;

    public Reading(int time, int temperature) {
        this.time = time;
        this.temperature = temperature;
    }
}

public class Atmosphere {
    private static final int numSensors = 8;
    private static final int hour = 60;

    private static void generateReport(List<Reading> readings, int startTime, int endTime, AtomicInteger reportCount) {
        System.out.println("Generating report for time interval: " + startTime + " to " + endTime);

        // Sort temperatures to get lowest and highest easily
        for (int i = 0; i < readings.size(); i++) {
            for (int j = 1; j < (readings.size() - i); j++) {
                if (readings.get(j - 1).temperature > readings.get(j).temperature) {
                    Reading temp = readings.get(j - 1);
                    readings.set(j - 1, readings.get(j));
                    readings.set(j, temp);
                }
            }
        }

        System.out.println("5 lowest temperatures this 'hour':");
        for (int i = 0; i < Math.min(5, readings.size()); i++) {
            System.out.println(readings.get(i).temperature + "F");
        }

        System.out.println("5 highest temperatures this 'hour':");
        for (int i = Math.max(0, readings.size() - 5); i < readings.size(); i++) {
            System.out.println(readings.get(i).temperature + "F");
        }

        // Find largest difference in order to display it
        int maxDiff = 0;
        int maxDiffStart = 0;
        for (int i = 0; i < readings.size(); i++) {
            for (int j = i + 1; j < readings.size() && (readings.get(j).time - readings.get(i).time <= 10); j++) {
                int diff = Math.abs(readings.get(j).temperature - readings.get(i).temperature);
                if (diff > maxDiff) {
                    maxDiff = diff;
                    maxDiffStart = readings.get(i).time;
                }
            }
        }

        System.out.println("10 'minute' interval with the largest difference starts at " + maxDiffStart + " with difference " + maxDiff + "F");
        reportCount.incrementAndGet();
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedQueue<Reading> queue = new ConcurrentLinkedQueue<>();
        AtomicInteger currentTime = new AtomicInteger(0);
        AtomicInteger reportCount = new AtomicInteger(0);
        List<Thread> sensors = new ArrayList<>();

        for (int i = 0; i < numSensors; i++) {
            Sensor s = new Sensor(queue, currentTime, reportCount, i);
            sensors.add(s);
            s.start();
        }

        int nextReportTime = 60;

        // Stop at 5 reports, and report once every hour at the end of the hour
        while (reportCount.get() < 5) {
            if (currentTime.get() >= nextReportTime) {
                synchronized (queue) {
                    generateReport(new ArrayList<>(queue), nextReportTime - hour, nextReportTime, reportCount);
                    queue.clear();
                    nextReportTime += hour;
                }
            }
            currentTime.incrementAndGet();
            Thread.sleep(10);
        }

        for (Thread s : sensors) {
            s.interrupt();
            s.join();
        }
    }
}

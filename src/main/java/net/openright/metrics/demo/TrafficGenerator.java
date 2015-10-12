package net.openright.metrics.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.codahale.metrics.MetricRegistry;

public class TrafficGenerator {

    private static Random random = new Random();
    private static List<Worker> workers = new ArrayList<>();

    public static void start(MetricRegistry metrics) {
        for (String worker : new String[] { "foo", "bar", "users", "services", "data", "weather" }) {
            workers.add(new Worker(worker));
        }

        for (Worker worker : workers) {
            worker.addMetrics(metrics);
        }

        startRandomThreads();
    }

    private static void startRandomThreads() {
        for (int i = 0; i < 10; i++) {
            Runnable runnable = () -> {
                while (true) generateRequest();
            };

            Thread workerThread = new Thread(runnable, "work-thread");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    protected static void generateRequest() {
        Worker worker = workers.get(random.nextInt(workers.size()));
        worker.executeRequest();

        try {
            Thread.sleep((long) (10000 * Math.random()));
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

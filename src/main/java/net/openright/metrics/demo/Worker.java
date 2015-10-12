package net.openright.metrics.demo;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;

public class Worker {

    private String endpointName;
    private Timer requestTimer;
    private Meter errorsMeter;

    public Worker(String endpointName) {
        this.endpointName = endpointName;
    }

    public void addMetrics(MetricRegistry metrics) {
        requestTimer = metrics.timer(MetricRegistry.name(Worker.class, endpointName, "time"));
        errorsMeter = metrics.meter(MetricRegistry.name(Worker.class, endpointName, "error"));
        metrics.register(MetricRegistry.name(Worker.class, endpointName, "errorRate"),
            new RatioGauge() {
                @Override
                protected Ratio getRatio() {
                    return Ratio.of(100 * errorsMeter.getFifteenMinuteRate(), requestTimer.getFifteenMinuteRate());
                }
            });
    }

    public void executeRequest() {
        try {
            requestTimer.time(() -> doWork());
        } catch (Exception e) {
            errorsMeter.mark();
        }
    }

    private Object doWork() {
        long millis = (long) (Math.pow(Math.random() * 1000, Math.random() * 2) / 100.0);
        try {
            Thread.sleep(millis);
            if (millis % 50 == 1) {
                throw new RuntimeException("request to " + endpointName + " failed");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}

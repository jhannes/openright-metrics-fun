package net.openright.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

public class ChartingReporter extends ScheduledReporter {

    private int capacity;
    private int size = 0;

    public ChartingReporter(MetricRegistry metricRegistry, int capacity) {
        super(metricRegistry, "charting", MetricFilter.ALL, TimeUnit.MINUTES, TimeUnit.SECONDS);
        this.capacity = capacity;
    }

    @Override
    public synchronized void report(
            @SuppressWarnings("rawtypes") SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {


        long time = System.currentTimeMillis();
        nextSnapshot();
        setDataPoint("x", time);

        for (@SuppressWarnings("rawtypes") Entry<String, Gauge> entry : gauges.entrySet()) {
            if (entry.getValue().getValue() instanceof Number) {
                setDataPoint(entry.getKey(), ((Number)entry.getValue().getValue()).doubleValue());
            }
        }

        for (Entry<String, Counter> entry : counters.entrySet()) {
            setDataPoint(entry.getKey(), entry.getValue().getCount());
        }

        for (Entry<String, Histogram> entry : histograms.entrySet()) {
            setDataPoints(entry.getKey(), entry.getValue().getSnapshot());
        }

        for (Entry<String, Meter> entry : meters.entrySet()) {
            setDataPoints(entry.getKey(), entry.getValue());
        }

        for (Entry<String, Timer> entry : timers.entrySet()) {
            setDataPoints(entry.getKey(), entry.getValue());
            setConvertedDataPoints(entry.getKey(), entry.getValue().getSnapshot());
        }
    }

    private void setDataPoints(String key, Metered value) {
        setDataPoint(key, "m1_rate", convertRate(value.getOneMinuteRate()));
        setDataPoint(key, "m5_rate", convertRate(value.getFiveMinuteRate()));
        setDataPoint(key, "m15_rate", convertRate(value.getFifteenMinuteRate()));
    }

    private void setDataPoints(String key, Snapshot snapshot) {
        setDataPoint(key, "p50", snapshot.getMean());
        setDataPoint(key, "p75", snapshot.get75thPercentile());
        setDataPoint(key, "p98", snapshot.get98thPercentile());
        setDataPoint(key, "p99", snapshot.get99thPercentile());
    }

    private void setConvertedDataPoints(String key, Snapshot snapshot) {
        setDataPoint(key, "p50", convertDuration(snapshot.getMean()));
        setDataPoint(key, "p75", convertDuration(snapshot.get75thPercentile()));
        setDataPoint(key, "p98", convertDuration(snapshot.get98thPercentile()));
        setDataPoint(key, "p99", convertDuration(snapshot.get99thPercentile()));
    }

    private void setDataPoint(String key, String subkey, Number value) {
        setDataPoint(key + "." + subkey, value);
    }

    private int next = -1;
    private Map<String, Number[]> values = new HashMap<>();

    public Set<String> getAxes() {
        return values.keySet();
    }

    void nextSnapshot() {
        next = (next + 1) % capacity;
        size = Math.min(size+1, capacity);
    }

    void setDataPoint(String string, Number value) {
        Number[] axis = values.computeIfAbsent(string, (key) -> new Number[capacity]);
        axis[next] = value;
    }

    public Collection<Object> getAxis(String axis) {
        ArrayList<Object> result = new ArrayList<>();
        result.add(axis);
        Number[] numbers = values.getOrDefault(axis, new Number[capacity]);
        int first = (next - size + capacity + 1)%capacity;
        for (int i=0; i<size; i++) {
            int index = (first+i)%capacity;
            result.add(numbers[index]);
        }
        return result;
    }

}

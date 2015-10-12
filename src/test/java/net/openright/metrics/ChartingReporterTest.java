package net.openright.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

public class ChartingReporterTest {

    private ChartingReporter reporter = new ChartingReporter(new MetricRegistry(), 100);

    @Test
    public void shouldSupportInitialSetting() {
        assertThat(reporter.getAxis("foo")).containsExactly("foo");
    }

    @Test
    public void shouldCreateSeries() throws Exception {
        reporter.nextSnapshot();
        reporter.setDataPoint("foo", 124.0);
        reporter.nextSnapshot();
        reporter.setDataPoint("foo", 125.0);
        reporter.setDataPoint("bar", 125.0);
        assertThat(reporter.getAxes()).containsOnly("foo", "bar");

        assertThat(reporter.getAxis("foo"))
            .containsExactly("foo", 124.0, 125.0);
    }


    @Test
    public void shouldSupportMissingSeries() throws Exception {
        reporter.nextSnapshot();
        reporter.setDataPoint("x", System.currentTimeMillis());

        assertThat(reporter.getAxis("bar"))
            .containsExactly("bar", null);
    }

    @Test
    public void shouldWorkWithoneDatapoint() throws Exception {
        reporter.nextSnapshot();
        reporter.setDataPoint("foo", 125.0);

        assertThat(reporter.getAxis("foo"))
            .containsExactly("foo", 125.0);
    }

    @Test
    public void shouldRolloverFullSeries() throws Exception {
        try(ChartingReporter reporter = new ChartingReporter(new MetricRegistry(), 3)) {
            for (int i=0; i<10; i++) {
                reporter.nextSnapshot();
                reporter.setDataPoint("foo", i);
            }

            assertThat(reporter.getAxis("foo"))
                .containsExactly("foo", 7, 8, 9);
        }

    }
}

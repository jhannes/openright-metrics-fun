package net.openright.metrics;

import javax.servlet.ServletContextEvent;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

public class OpenrightMetricsListener extends MetricsServlet.ContextListener {

    private MetricRegistry metrics = new MetricRegistry();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        metrics = (MetricRegistry) event.getServletContext().getAttribute("metrics");
        super.contextInitialized(event);
    }

    @Override
    protected MetricRegistry getMetricRegistry() {
        return metrics;
    }
}

package net.openright.metrics;

import javax.servlet.ServletContextEvent;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.HealthCheckServlet;

public class OpenrightHealthCheckListener extends HealthCheckServlet.ContextListener {

    private HealthCheckRegistry healthChecks = new HealthCheckRegistry();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        healthChecks = (HealthCheckRegistry) event.getServletContext().getAttribute("healthChecks");
        super.contextInitialized(event);
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return healthChecks;
    }
}

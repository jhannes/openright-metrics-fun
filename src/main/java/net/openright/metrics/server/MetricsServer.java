package net.openright.metrics.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import net.openright.metrics.demo.TrafficGenerator;

public class MetricsServer {
    private static Logger log = LoggerFactory.getLogger(MetricsServer.class);


    private MetricRegistry metricRegistry;

    public MetricsServer(int port, MetricRegistry metrics, HealthCheckRegistry healthChecks) {
        metricRegistry = metrics;
        server = new Server(port);
        webAppContext = createWebAppContext(metrics, healthChecks);
        server.setHandler(createHandler());
    }

    public void start() throws Exception {
        server.start();

        if (webAppContext.getUnavailableException() != null) {
            log.error("Stopping server");
            server.stop();
        } else {
            System.out.println(this.getURI());
        }
    }

    public String getURI() {
        return server.getURI().toString();
    }

    private Handler createHandler() {
        handlerList.addHandler(webAppContext);
        handlerList.addHandler(new ShutdownHandler("sdgmsldkgnslkn", false, true));
        InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry);
        instrumentedHandler.setHandler(handlerList);
        return instrumentedHandler;
    }

    private static WebAppContext createWebAppContext(MetricRegistry metricRegistry, HealthCheckRegistry healthChecks) {
        WebAppContext handler = new WebAppContext();
        handler.setContextPath("/chart");
        handler.setBaseResource(Resource.newClassPathResource("/webapp"));
        handler.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        handler.getServletContext().setAttribute("metrics", metricRegistry);
        handler.getServletContext().setAttribute("healthChecks", healthChecks);
        return handler;
    }

    private Server server;
    private HandlerList handlerList = new HandlerList();
    private WebAppContext webAppContext;

    public static void main(String args[]) throws Exception {
        MetricRegistry metrics = new MetricRegistry();
        HealthCheckRegistry healthChecks = new HealthCheckRegistry();
        metrics.register("jvm/gc", new GarbageCollectorMetricSet());
        metrics.register("jvm/memory", new MemoryUsageGaugeSet());
        metrics.register("jvm/thread-states", new ThreadStatesGaugeSet());
        metrics.register("jvm/fd/usage", new FileDescriptorRatioGauge());

        TrafficGenerator.start(metrics);

        MetricsServer metricsServer = new MetricsServer(3000, metrics, healthChecks);
        metricsServer.start();
    }

}
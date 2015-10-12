package net.openright.metrics;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.codahale.metrics.MetricRegistry;

public class MetricsChartServlet extends HttpServlet {

    public ChartingReporter chartingReporter;

    @Override
    public void init() throws ServletException {
        MetricRegistry metrics = (MetricRegistry)getServletContext().getAttribute("metrics");
        this.chartingReporter = new ChartingReporter(metrics, 100);
        this.chartingReporter.start(15, TimeUnit.SECONDS);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String prefix;
        if (req.getParameter("prefix") != null) {
            prefix = req.getParameter("prefix");
        } else if (req.getParameter("client") != null) {
            prefix = "net.openright.metrics.demo.Worker." + req.getParameter("client");
        } else {
            prefix = "net.openright.metrics.demo.Worker.data";
        }

        JSONArray columns = new JSONArray();
        columns.put(new JSONArray(chartingReporter.getAxis("x")));

        JSONObject data = new JSONObject()
                .put("x", "x")
                .put("columns", columns);

        if (prefix.equals("jvm/memory")) {
            columns.put(getAxis("jvm/memory.heap.used"));
            columns.put(getAxis("jvm/memory.pools.PS-Old-Gen.usage"));
        } else if (prefix.equals("jvm/threads")) {
            columns.put(getAxis("jvm/thread-states.runnable.count"));
            columns.put(getAxis("jvm/thread-states.blocked.count"));
            columns.put(getAxis("jvm/thread-states.waiting.count"));
            columns.put(getAxis("jvm/thread-states.deadlock.count"));
        } else if ("jetty/responses".equals(prefix)) {
            columns.put(getAxis("org.eclipse.jetty.webapp.WebAppContext.1xx-responses.m1_rate"));
            columns.put(getAxis("org.eclipse.jetty.webapp.WebAppContext.2xx-responses.m1_rate"));
            columns.put(getAxis("org.eclipse.jetty.webapp.WebAppContext.3xx-responses.m1_rate"));
            columns.put(getAxis("org.eclipse.jetty.webapp.WebAppContext.4xx-responses.m1_rate"));
            columns.put(getAxis("org.eclipse.jetty.webapp.WebAppContext.5xx-responses.m1_rate"));
            data.put("names", new JSONObject()
                    .put("org.eclipse.jetty.webapp.WebAppContext.1xx-responses.m1_rate", "1xx responses")
                    .put("org.eclipse.jetty.webapp.WebAppContext.2xx-responses.m1_rate", "2xx responses")
                    .put("org.eclipse.jetty.webapp.WebAppContext.3xx-responses.m1_rate", "3xx responses")
                    .put("org.eclipse.jetty.webapp.WebAppContext.4xx-responses.m1_rate", "4xx responses")
                    .put("org.eclipse.jetty.webapp.WebAppContext.5xx-responses.m1_rate", "5xx responses"));
            data.put("colors", new JSONObject()
                    .put("org.eclipse.jetty.webapp.WebAppContext.1xx-responses.m1_rate", "blue")
                    .put("org.eclipse.jetty.webapp.WebAppContext.2xx-responses.m1_rate", "green")
                    .put("org.eclipse.jetty.webapp.WebAppContext.3xx-responses.m1_rate", "lime")
                    .put("org.eclipse.jetty.webapp.WebAppContext.4xx-responses.m1_rate", "orange")
                    .put("org.eclipse.jetty.webapp.WebAppContext.5xx-responses.m1_rate", "red"));
        } else if ("p75".equals(prefix)) {
            JSONObject names = new JSONObject();
            for (String axis : chartingReporter.getAxes()) {
                if (axis.startsWith("net.openright.metrics.demo.Worker") && axis.endsWith(".time.p75")) {
                    columns.put(getAxis(axis));
                    names.put(axis, axis.split("\\.")[2]);
                }
            }
            data.put("names", names);
        } else if ("m1".equals(prefix)) {
            JSONObject names = new JSONObject();
            for (String axis : chartingReporter.getAxes()) {
                if (axis.startsWith("net.openright.metrics.demo.Worker") && axis.endsWith(".time.m1_rate")) {
                    columns.put(getAxis(axis));
                    names.put(axis, axis.split("\\.")[2]);
                }
            }
            data.put("names", names);
        } else if ("errors".equals(prefix)) {
            JSONObject types = new JSONObject();
            for (String axis : chartingReporter.getAxes()) {
                if (axis.startsWith("net.openright.metrics.demo.Worker") && axis.endsWith(".errorRate")) {
                    columns.put(getAxis(axis));
                }
            }
            data.put("types", types);
        } else if ("timing".equals(req.getParameter("aspect"))) {
            columns.put(getAxis(prefix + ".time.p99"));
            columns.put(getAxis(prefix + ".time.p98"));
            columns.put(getAxis(prefix + ".time.p75"));
            columns.put(getAxis(prefix + ".time.p50"));
            data.put("names", new JSONObject()
                    .put(prefix + ".time.p99", "99%")
                    .put(prefix + ".time.p98", "98%")
                    .put(prefix + ".time.p75", "75%")
                    .put(prefix + ".time.p50", "50%"));
        } else if ("load".equals(req.getParameter("aspect"))) {
            columns.put(getAxis(prefix + ".time.m1_rate"));
            columns.put(getAxis(prefix + ".time.m5_rate"));
            columns.put(getAxis(prefix + ".time.m15_rate"));
        } else if ("errors".equals(req.getParameter("aspect"))) {
            columns.put(getAxis(prefix + ".time.m1_rate"));
            columns.put(getAxis(prefix + ".error.m1_rate"));
            columns.put(getAxis(prefix + ".errorRate"));
            data.put("names", new JSONObject()
                    .put(prefix + ".time.m1_rate", "Requests")
                    .put(prefix + ".error.m1_rate", "Errors")
                    .put(prefix + ".errorRate", "Error rate"));
            data.put("axes", new JSONObject().put(prefix + ".errorRate", "y2"));
            data.put("types", new JSONObject().put(prefix + ".errorRate", "scatter"));
        } else {
            for (String axis : chartingReporter.getAxes()) {
                if (axis.startsWith(prefix)) {
                    columns.put(getAxis(axis));
                }
            }
        }

        JSONObject axisJSON = new JSONObject()
                .put("x", new JSONObject()
                        .put("type", "timeseries")
                        .put("tick", new JSONObject().put("format", "%H:%M")));
        if (data.has("axes")) {
            axisJSON.put("y2", new JSONObject().put("show", true));
        }

        JSONObject result = new JSONObject()
                .put("menu", createMenu())
                .put("data", data)
                .put("axis", axisJSON);

        try (PrintWriter writer = resp.getWriter()) {
            result.write(writer);
        }
    }

    private JSONObject createMenu() {
        List<String> endpoints = new ArrayList<>(chartingReporter.getAxes().stream()
            .filter(s -> s.startsWith("net.openright.metrics.demo.Worker."))
            .map(s -> s.split("\\.")[5])
            .collect(Collectors.toSet()));
        Collections.sort(endpoints);

        JSONObject menu = new JSONObject()
                .put("endpoints", endpoints)
                .put("jvm", Arrays.asList("jvm/memory", "jvm/thread-states", "jvm/threads"))
                .put("jetty", Arrays.asList("org.eclipse.jetty.webapp.WebAppContext.requests", "jetty/responses"));
        return menu;
    }

    private JSONArray getAxis(String axis) {
        return new JSONArray(chartingReporter.getAxis(axis));
    }

}

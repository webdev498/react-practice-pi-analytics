/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package pi.analytics.admin.serviceaddress.server;

import com.pi.common.config.PiConfig;
import com.pi.common.config.PiKubeServicePort;

import java.io.IOException;
import java.io.StringWriter;

import fi.iki.elonen.NanoHTTPD;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class MetricsWebServer extends NanoHTTPD {

  MetricsWebServer() {
    super(PiConfig.get().getInteger(PiKubeServicePort.PROMETHEUS_SCRAPE_PORT));
  }

  @Override
  public Response serve(IHTTPSession session) {
    switch (session.getUri()) {
      case "/":
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", createRootHtml());
      case "/healthz":
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", createRootHtml());
      case "/metrics":
        try {
          final String metricsPage = getMetricsAsString();
          return newFixedLengthResponse(Response.Status.OK, TextFormat.CONTENT_TYPE_004, metricsPage);
        } catch (IOException e) {
          return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain; charset=utf-8", e.getMessage());
        }
      default:
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "plain/text", "Not found");
    }
  }

  private String createRootHtml() {
    return "<html><body>Server is running.<br /><br />"
        + "<a href=\"/metrics\"> /metrics</a><br />"
        + "</body></html>";
  }

  private String getMetricsAsString() throws IOException {
    StringWriter sw = new StringWriter();
    TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
    return sw.toString();
  }

}

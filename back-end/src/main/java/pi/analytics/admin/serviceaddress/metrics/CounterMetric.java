/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package pi.analytics.admin.serviceaddress.metrics;

import com.google.common.base.Preconditions;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import io.prometheus.client.Counter;

/**
 * @author paul.sitnikov@practiceinsight.io
 */
public class CounterMetric {

  private final Counter counter;
  private final MetricSpec metricSpec;

  CounterMetric(Counter counter, MetricSpec metricSpec) {
    this.counter = counter;
    this.metricSpec = Preconditions
        .checkNotNull(metricSpec, "Can't create CounterMetric, metric spec should not be null");
  }

  public void inc(String... labelValues) {
    incBy(1, labelValues);
  }

  public void incBy(double amt, String... labelValues) {
    if (CollectionUtils.isEmpty(metricSpec.labels())) {
      Preconditions.checkArgument(
          ArrayUtils.isEmpty(labelValues),
          String.format("%s contains no labels but call to incCounter has label values!", metricSpec.getMetricName())
      );
      counter.inc(amt);
    } else {
      Preconditions.checkArgument(
          labelValues != null && labelValues.length == metricSpec.labels().size(),
          String.format("%s contains labels but call to incCounter has no label values!", metricSpec.getMetricName())
      );
      counter.labels(labelValues).inc(amt);
    }
  }


}

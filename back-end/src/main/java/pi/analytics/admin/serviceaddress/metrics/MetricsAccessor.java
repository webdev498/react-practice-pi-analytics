/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package pi.analytics.admin.serviceaddress.metrics;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;

/**
 * A loading cache is used to only publish given metrics once we have a value for them, rather than registering a series
 * of zero-value metrics on startup.
 *
 * @author paul.sitnikov@practiceinsight.io
 */
public class MetricsAccessor {

  private final LoadingCache<MetricSpec, CounterMetric> counterStore = CacheBuilder.newBuilder()
      .build(CacheLoader.from(metricSpec -> {
        metricSpec = Preconditions
            .checkNotNull(metricSpec, "Can't get counter metric, metric spec should not be null");

        final String metricName = metricSpec.getMetricName();

        Counter.Builder countBuilder = Counter.build()
            .name(metricName)
            .help(metricName);

        List<String> metricIdLabels = metricSpec.labels();

        if (CollectionUtils.isNotEmpty(metricIdLabels)) {
          countBuilder.labelNames(metricIdLabels.toArray(new String[metricIdLabels.size()]));
        }

        return new CounterMetric(countBuilder.create().register(), metricSpec);
      }));

  @Inject
  public MetricsAccessor() {
    CollectorRegistry.defaultRegistry.clear();
  }

  public CounterMetric getCounter(MetricSpec metricSpec) {
    try {
      return counterStore.get(metricSpec);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}

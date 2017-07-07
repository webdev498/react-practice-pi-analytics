/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package pi.analytics.admin.serviceaddress.metrics;

import com.google.common.base.Joiner;

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author paul.sitnikov@practiceinsight.io
 */
@Value.Immutable
@Value.Style(jdkOnly = true)
public abstract class MetricSpec {

  static final String BASE_METRIC_PATH = "service_address_admin";

  abstract String action();

  abstract List<String> labels();

  String getMetricName() {
    return makeStringPrometheusFriendly(Joiner.on("_").join(
        BASE_METRIC_PATH,
        makeStringPrometheusFriendly(action())
    ));
  }

  private String makeStringPrometheusFriendly(String metricName) {
    Matcher matcher = Pattern.compile("[A-Z]").matcher(metricName);
    if (matcher.find()) {
      metricName = toPrometheusFriendlyLowerCase(metricName);
    }
    return metricName.replaceAll("[^a-zA-Z0-9_:]", "_");
  }

  /**
   * @return format of metric name part consistent with prometheus formatting.
   */
  private String toPrometheusFriendlyLowerCase(String input) {
    if (StringUtils.isBlank(input)) {
      return "";
    }
    if (input.contains("_") || input.contains("-")) {
      return input.toLowerCase();
    }

    // convert TitleCase to prometheus format
    return Joiner.on("_").join(
        Arrays
            .stream(StringUtils.splitByCharacterTypeCamelCase(input))
            .map(String::toLowerCase)
            .collect(Collectors.toList())
    );
  }

}

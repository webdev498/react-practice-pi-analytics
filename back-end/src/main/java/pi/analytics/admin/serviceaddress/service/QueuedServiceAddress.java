/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service;

import com.google.auto.value.AutoValue;

import java.util.Optional;

import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@AutoValue
public abstract class QueuedServiceAddress {
  public static QueuedServiceAddress create(final String queueId, final Optional<ServiceAddress> serviceAddress) {
    return new AutoValue_QueuedServiceAddress(queueId, serviceAddress);
  }
  public abstract String queueId();
  public abstract Optional<ServiceAddress> serviceAddress();
}


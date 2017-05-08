/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc;
import pi.ip.generated.queue.QueueOnPremGrpc;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressSortingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(QueueOnPremGrpc.QueueOnPremBlockingStub.class)
        .toProvider(QueueOnPremBlockingStubProvider.class)
        .in(Singleton.class);
    bind(DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub.class)
        .toProvider(DatastoreSg3BlockingStubProvider.class)
        .in(Singleton.class);
    bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
        .toProvider(ServiceAddressServiceBlockingStubProvider.class)
        .in(Singleton.class);
  }
}

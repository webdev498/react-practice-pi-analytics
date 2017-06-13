/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.es.ESMutationServiceGrpc;
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
    bind(LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub.class)
        .toProvider(LawFirmDbServiceBlockingStubProvider.class)
        .in(Singleton.class);
    bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
        .toProvider(ServiceAddressServiceBlockingStubProvider.class)
        .in(Singleton.class);
    bind(ESMutationServiceGrpc.ESMutationServiceBlockingStub.class)
        .toProvider(EsMutationServiceBlockingStubProvider.class)
        .in(Singleton.class);
    bind(ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub.class)
        .toProvider(ServiceAddressServiceBlockingStubProvider.class)
        .in(Singleton.class);
    bind(NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceBlockingStub.class)
        .toProvider(LawFirmSearchServiceBlockingStubProvider.class)
        .in(Singleton.class);
  }
}

/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service;

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.pi.common.test.type.DisabledTests;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import pi.admin.service_address_sorting.generated.NextUnsortedServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SearchLawFirmsRequest;
import pi.admin.service_address_sorting.generated.SearchResults;
import pi.admin.service_address_sorting.generated.ServiceAddressBundle;
import pi.analytics.admin.serviceaddress.guice.ServiceAddressSortingModule;

/**
 * @author shane.xie@practiceinsight.io
 *
 * To run these integration tests locally in IDEA, set the appropriate VM options. E.g.:
 *
 *   -Dpi_custom_property_file_path=/Users/shane/work/config/onprem-prod.properties
 *   -Dconf_default_service_dns_suffix=.hatchedpi.com
 */
@Category(DisabledTests.class)
public class ServiceAddressSortingServiceImplIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSortingServiceImplIntegrationTest.class);
  private ServiceAddressSortingServiceImpl serviceAddressSortingService;

  @Before
  public void setUp() throws Exception {
    final Injector injector = Guice.createInjector(new ServiceAddressSortingModule());
    serviceAddressSortingService = injector.getInstance(ServiceAddressSortingServiceImpl.class);
  }

  @Test
  public void nextUnsortedServiceAddress() throws Exception {
    final NextUnsortedServiceAddressRequest request =
        NextUnsortedServiceAddressRequest
            .newBuilder()
            .setRequestedBy("shane")
            .build();

    serviceAddressSortingService.nextUnsortedServiceAddress(request, new StreamObserver<ServiceAddressBundle>() {
      @Override
      public void onNext(ServiceAddressBundle bundle) {
        log.info("onNext: {}", bundle);
      }

      @Override
      public void onError(Throwable throwable) {
        log.error("onError: {}", throwable);
      }

      @Override
      public void onCompleted() {
        log.info("onCompleted");
      }
    });
  }

  @Test
  public void searchLawFirms() throws Exception {
    final SearchLawFirmsRequest request = SearchLawFirmsRequest.newBuilder().setSearchTerm("spruson").build();
    serviceAddressSortingService.searchLawFirms(request, new StreamObserver<SearchResults>() {
      @Override
      public void onNext(SearchResults searchResults) {
        log.info("onNext: {}", searchResults);
      }

      @Override
      public void onError(Throwable throwable) {
        log.error("onError: {}", throwable);
      }

      @Override
      public void onCompleted() {
        log.info("onCompleted");
      }
    });
  }
}
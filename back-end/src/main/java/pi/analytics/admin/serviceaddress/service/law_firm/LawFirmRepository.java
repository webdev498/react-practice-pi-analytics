/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.law_firm;

import com.google.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchRequest;
import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceBlockingStub;
import pi.admin.service_address_sorting.generated.Agent;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class LawFirmRepository {

  @Inject
  private NakedLawFirmSearchServiceBlockingStub lawFirmSearchServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public List<Agent> searchLawFirms(final String searchTerm, final String countryCode) {
    final LawFirmSearchRequest searchRequest =
        LawFirmSearchRequest
            .newBuilder()
            .setLawFirmPrefix(searchTerm)
            .setCountryCode(countryCode)
            .build();

    return lawFirmSearchServiceBlockingStub
        .search(searchRequest)
        .getLawFirmsList()
        .stream()
        .map(lawFirm -> {
          final GetServiceAddressesForLawFirmRequest getServiceAddressRequest =
              GetServiceAddressesForLawFirmRequest
                  .newBuilder()
                  .setLawFirmId(lawFirm.getLawFirmId())
                  .build();

          final List<ServiceAddress> serviceAddresses =
              serviceAddressServiceBlockingStub
                  .getServiceAddressesForLawFirm(getServiceAddressRequest)
                  .getServiceAddressesList();

          return Agent
              .newBuilder()
              .setLawFirm(lawFirm)
              .addAllServiceAddresses(serviceAddresses)
              .build();
        })
        .collect(Collectors.toList());
  }
}

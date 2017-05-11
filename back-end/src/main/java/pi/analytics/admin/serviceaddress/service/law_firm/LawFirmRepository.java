/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.law_firm;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;
import java.util.stream.Collectors;

import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchRequest;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc.LawFirmSearchServiceBlockingStub;
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class LawFirmRepository {

  @Inject
  LawFirmSearchServiceBlockingStub lawFirmSearchServiceBlockingStub;

  @Inject
  ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public List<Agent> searchLawFirms(final String searchTerm) {
    final LawFirmSearchRequest searchRequest =
        LawFirmSearchRequest
            .newBuilder()
            .setLawFirmPrefix(searchTerm)
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

  public long createLawFirm(final CreateLawFirmRequest request) {
    // TODO(SX)

    // Create a new law firm and assign the service address to it (Cloud SQL)
    // Update ES indexes at http://es-1.hatchedpi.com:9200/_cat/indices
    // * lawfirm
    // * lawfirm_service_address_v1
    // Also see rpc UpsertThinLawFirmServiceAddress RPC endpoint from DatastoreSg3Service
    // Remember to delete from queue
    // Also record staff user action (IPFLOW-786)?

    throw new NotImplementedException();
  }
}

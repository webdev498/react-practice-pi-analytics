/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.law_firm;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchRequest;
import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchServiceGrpc.LawFirmSearchServiceBlockingStub;
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3.ThinServiceAddress;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.QueueOnPremGrpc.QueueOnPremBlockingStub;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class LawFirmRepository {

  @Inject
  private LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub lawFirmDbServiceBlockingStub;

  @Inject
  private LawFirmSearchServiceBlockingStub lawFirmSearchServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private DatastoreSg3ServiceBlockingStub datastoreSg3ServiceBlockingStub;

  @Inject
  private QueueOnPremBlockingStub queueOnPremBlockingStub;

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
    // Create the new law firm record in MySQL
    final LawFirm lawFirmToBeCreated =
        LawFirm
            .newBuilder()
            .setName(request.getName())
            .setStateStr(request.getState())
            .setCountry(request.getCountryCode())
            .setWebsiteUrl(request.getWebsiteUrl())
            .build();

    final long lawFirmId = lawFirmDbServiceBlockingStub.createLawFirm(
        pi.ip.data.relational.generated.CreateLawFirmRequest
            .newBuilder()
            .setLawFirm(lawFirmToBeCreated)
            .setCreatedBy(request.getRequestedBy())
            .build()
    ).getLawFirmId();

    final LawFirm newLawFirm =
        LawFirm
            .newBuilder(lawFirmToBeCreated)
            .setLawFirmId(lawFirmId)
            .build();

    // Assign the service address to the new law firm (MySQL)
    serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm(
        AssignServiceAddressToLawFirmRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddress().getServiceAddressId())
            .setLawFirmId(lawFirmId)
            .build()
    );

    // Update Elasticsearch caches
    datastoreSg3ServiceBlockingStub.upsertIntoLawFirmCaches(newLawFirm);  // Used by law firm search by name
    datastoreSg3ServiceBlockingStub.upsertThinLawFirmServiceAddress(
        createThinServiceAddressForLawFirm(request.getServiceAddress(), lawFirmId)
    );

    // Remove service address from unsorted queue
    queueOnPremBlockingStub.deleteQueueUnit(
        DeleteUnitRequest
            .newBuilder()
            .setDbId(request.getUnsortedServiceAddressQueueItemId())
            .build()
    );

    return lawFirmId;
  }

  private ThinServiceAddress createThinServiceAddressForLawFirm(final ServiceAddress serviceAddress, final long lawFirmId) {
    return ThinServiceAddress
        .newBuilder(createPartialThinServiceAddress(serviceAddress))
        .setLawFirmId(lawFirmId)
        .build();
  }

  private ThinServiceAddress createThinServiceAddressForNonLawFirm(final ServiceAddress serviceAddress) {
    return ThinServiceAddress
        .newBuilder(createPartialThinServiceAddress(serviceAddress))
        .setNotALawFirm(true)
        .build();
  }

  private ThinServiceAddress createPartialThinServiceAddress(final ServiceAddress serviceAddress) {
    return ThinServiceAddress
        .newBuilder()
        .setServiceAddressId(serviceAddress.getServiceAddressId())
        .setName(serviceAddress.getName())
        .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
        .setCountry(serviceAddress.getCountry())
        .setLongitude(serviceAddress.getLongitude())
        .setLatitude(serviceAddress.getLatitude())
        .build();
  }
}

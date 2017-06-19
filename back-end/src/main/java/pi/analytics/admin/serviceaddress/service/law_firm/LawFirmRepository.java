/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.law_firm;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import io.practiceinsight.licensingalert.citationsearch.generated.LawFirmSearchRequest;
import io.practiceinsight.licensingalert.citationsearch.generated.NakedLawFirmSearchServiceGrpc.NakedLawFirmSearchServiceBlockingStub;
import pi.admin.service_address_sorting.generated.Agent;
import pi.admin.service_address_sorting.generated.CreateLawFirmRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressesForLawFirmRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.generated.es.ESMutationServiceGrpc.ESMutationServiceBlockingStub;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ThinLawFirmRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressRecord;
import pi.ip.generated.es.ThinServiceAddressRecord;
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
  private NakedLawFirmSearchServiceBlockingStub lawFirmSearchServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private ESMutationServiceBlockingStub esMutationServiceBlockingStub;

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
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getName()), "Law firm name is required");
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getCountryCode()), "Country code is required");
    Preconditions.checkArgument(request.hasServiceAddress(), "Service address is required");

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
    esMutationServiceBlockingStub.upsertLawFirm(newLawFirm);  // Used by law firm search by name
    esMutationServiceBlockingStub.upsertThinLawFirmServiceAddressRecord(
        createThinLawFirmServiceAddressRecordForLawFirm(request.getServiceAddress(), newLawFirm)
    );

    return lawFirmId;
  }

  private ThinLawFirmServiceAddressRecord createThinLawFirmServiceAddressRecordForLawFirm(
      final ServiceAddress serviceAddress, final LawFirm lawFirm) {
    return ThinLawFirmServiceAddressRecord
        .newBuilder()
        .setLawFirm(
            ThinLawFirmRecord.newBuilder()
                .setId(lawFirm.getLawFirmId())
                .setName(lawFirm.getName())
        )
        .setLawFirmFlag(true)
        .setServiceAddress(
            ThinServiceAddressRecord
                .newBuilder()
                .setId(serviceAddress.getServiceAddressId())
                .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                .setCountry(serviceAddress.getCountry())
                .setLoc(
                    LocationRecord
                        .newBuilder()
                        .setLat((float) serviceAddress.getLatitude())
                        .setLon((float) serviceAddress.getLongitude())
                )
        )
        .build();
  }
}

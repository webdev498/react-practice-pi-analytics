/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.Int64Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.SetSortingImpossibleRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetLawFirmByIdRequest;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.LawFirmDbServiceGrpc.LawFirmDbServiceBlockingStub;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.es.ESMutationServiceGrpc.ESMutationServiceBlockingStub;
import pi.ip.generated.es.LocationRecord;
import pi.ip.generated.es.ThinLawFirmRecord;
import pi.ip.generated.es.ThinLawFirmServiceAddressRecord;
import pi.ip.generated.es.ThinServiceAddressRecord;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 * @author pavel.sitnikov
 */
public class ServiceAddressSorter {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSorter.class);

  @Inject
  LawFirmDbServiceBlockingStub lawFirmDbServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private ESMutationServiceBlockingStub esMutationServiceBlockingStub;

  public void assignServiceAddress(final AssignServiceAddressRequest request) {
    Preconditions.checkArgument(request.getLawFirmId() != 0, "Law firm ID is required");
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");

    serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm(
        AssignServiceAddressToLawFirmRequest
            .newBuilder()
            .setLawFirmId(request.getLawFirmId())
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    // Also update Elasticsearch index
    final LawFirm lawFirm = lawFirmDbServiceBlockingStub.getLawFirmById(
        GetLawFirmByIdRequest
            .newBuilder()
            .setLawFirmId(request.getLawFirmId())
            .build()
    ).getLawFirm();
    final ServiceAddress serviceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );
    final ThinLawFirmServiceAddressRecord thinLawFirmServiceAddressRecord =
        ThinLawFirmServiceAddressRecord
            .newBuilder()
            .setLawFirm(
                ThinLawFirmRecord
                    .newBuilder()
                    .setId(lawFirm.getLawFirmId())
                    .setName(lawFirm.getName())
            )
            .setServiceAddress(
                ThinServiceAddressRecord
                    .newBuilder()
                    .setId(serviceAddress.getServiceAddressId())
                    .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                    .setCountry(serviceAddress.getCountry())
                    .setLoc(
                        LocationRecord
                            .newBuilder()
                            .setLon((float) serviceAddress.getLongitude())
                            .setLat((float) serviceAddress.getLatitude())
                    )
            )
            .setLawFirmFlag(true)
            .build();
    esMutationServiceBlockingStub.upsertThinLawFirmServiceAddressRecord(thinLawFirmServiceAddressRecord);
  }

  public void unsortServiceAddress(final UnsortServiceAddressRequest request) {
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");
    serviceAddressServiceBlockingStub.unassignServiceAddressFromLawFirm(
        UnassignServiceAddressFromLawFirmRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );
    esMutationServiceBlockingStub.deleteThinLawFirmServiceAddress(
        Int64Value
            .newBuilder()
            .setValue(request.getServiceAddressId())
            .build()
    );
  }

  public void setServiceAddressAsNonLawFirm(final SetServiceAddressAsNonLawFirmRequest request) {
    Preconditions.checkArgument(request.getServiceAddressId() != 0, "Service address ID is required");
    serviceAddressServiceBlockingStub.setServiceAddressAsNonLawFirm(
        pi.ip.data.relational.generated.SetServiceAddressAsNonLawFirmRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );

    // Also update Elasticsearch index
    final ServiceAddress serviceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(request.getServiceAddressId())
            .build()
    );
    final ThinLawFirmServiceAddressRecord thinLawFirmServiceAddressRecord =
        ThinLawFirmServiceAddressRecord
            .newBuilder()
            .setLawFirmFlag(false)
            .setServiceAddress(
                ThinServiceAddressRecord
                    .newBuilder()
                    .setId(serviceAddress.getServiceAddressId())
                    .setNameAddress(serviceAddress.getName() + " " + serviceAddress.getAddress())
                    .setCountry(serviceAddress.getCountry())
                    .setLoc(
                        LocationRecord
                            .newBuilder()
                            .setLon((float) serviceAddress.getLongitude())
                            .setLat((float) serviceAddress.getLatitude())
                    )
            )
            .build();
    esMutationServiceBlockingStub.upsertThinLawFirmServiceAddressRecord(thinLawFirmServiceAddressRecord);
  }

  public void setSortingImpossible(final SetSortingImpossibleRequest request) {
    // TODO: This needs to be implemented. Current behaviour is to skip sorting the service address.
  }
}

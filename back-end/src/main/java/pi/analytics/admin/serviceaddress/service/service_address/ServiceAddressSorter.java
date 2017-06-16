/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.Int64Value;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.SetSortingImpossibleRequest;
import pi.admin.service_address_sorting.generated.SkipServiceAddressRequest;
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
import pi.ip.generated.queue.AddUnitRequestOnPrem;
import pi.ip.generated.queue.DelayUnitRequest;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.MsgUnit;
import pi.ip.generated.queue.QueueNameOnPrem;
import pi.ip.generated.queue.QueueOnPremGrpc.QueueOnPremBlockingStub;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 * @author pavel.sitnikov
 */
public class ServiceAddressSorter {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSorter.class);

  private static final int SORTING_IMPOSSIBLE_DELAY_MINUTES = 30 * 24 * 60;

  @Inject
  LawFirmDbServiceBlockingStub lawFirmDbServiceBlockingStub;

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private ESMutationServiceBlockingStub esMutationServiceBlockingStub;

  @Inject
  private QueueOnPremBlockingStub queueOnPremBlockingStub;

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

    deleteQueueItem(request.getUnsortedServiceAddressQueueItemId());
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
    addQueueItem(request.getServiceAddressId());
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

    deleteQueueItem(request.getUnsortedServiceAddressQueueItemId());
  }

  public void skipServiceAddress(final SkipServiceAddressRequest request) {
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getUnsortedServiceAddressQueueItemId()),
        "Unsorted service address queue item ID is required");
    Preconditions.checkArgument(request.getDelayMinutes() != 0, "Delay (in minutes) is required");
    delayQueueItem(request.getUnsortedServiceAddressQueueItemId(), request.getDelayMinutes());
  }

  public void setSortingImpossible(final SetSortingImpossibleRequest request) {
    Preconditions.checkArgument(StringUtils.isNotBlank(request.getUnsortedServiceAddressQueueItemId()),
        "Unsorted service address queue item ID is required");

    delayQueueItem(request.getUnsortedServiceAddressQueueItemId(), SORTING_IMPOSSIBLE_DELAY_MINUTES);
  }

  private void addQueueItem(final long serviceAddressId) {
    final AddUnitRequestOnPrem addUnitRequestOnPrem =
        AddUnitRequestOnPrem.newBuilder()
            .setQueueNameOnPrem(QueueNameOnPrem.ServiceAddrSort_en)
            .setDelaySeconds((int) TimeUnit.DAYS.toSeconds(1))
            .addMsgUnit(
                MsgUnit
                    .newBuilder()
                    .setUniqueMsgKey(String.valueOf(serviceAddressId))
            )
            .build();
    queueOnPremBlockingStub.addUnit(addUnitRequestOnPrem);
  }

  private void deleteQueueItem(final String queueId) {
    final DeleteUnitRequest deleteUnitRequest =
        DeleteUnitRequest
            .newBuilder()
            .setDbId(queueId)
            .build();
    queueOnPremBlockingStub.deleteQueueUnit(deleteUnitRequest);
  }

  private void delayQueueItem(final String queueId, final int delayInMinutes) {
    DelayUnitRequest delayUnitRequest =
        DelayUnitRequest
            .newBuilder()
            .setDbId(queueId)
            .setDelaySeconds((int) TimeUnit.MINUTES.toSeconds(delayInMinutes))
            .build();
    queueOnPremBlockingStub.delayQueueUnit(delayUnitRequest);
  }
}

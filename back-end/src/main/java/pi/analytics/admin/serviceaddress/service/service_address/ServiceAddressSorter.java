/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.SkipServiceAddressRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.ip.data.relational.generated.AssignServiceAddressToLawFirmRequest;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.data.relational.generated.UnassignServiceAddressFromLawFirmRequest;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub;
import pi.ip.generated.datastore_sg3.IpDatastoreSg3;
import pi.ip.generated.queue.AddUnitRequestOnPrem;
import pi.ip.generated.queue.DelayUnitRequest;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.MsgUnit;
import pi.ip.generated.queue.QueueNameOnPrem;
import pi.ip.generated.queue.QueueOnPremGrpc.QueueOnPremBlockingStub;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 * @author pavel.sitnikov
 */
public class ServiceAddressSorter {

  private static final Logger log = LoggerFactory.getLogger(ServiceAddressSorter.class);

  @Inject
  private ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  @Inject
  private DatastoreSg3ServiceBlockingStub datastoreSg3ServiceBlockingStub;

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
    datastoreSg3ServiceBlockingStub.upsertThinLawFirmServiceAddress(getThinServiceAddress(request.getServiceAddressId()));
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
    datastoreSg3ServiceBlockingStub.deleteThinLawFirmServiceAddress(getThinServiceAddress(request.getServiceAddressId()));
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
    deleteQueueItem(request.getUnsortedServiceAddressQueueItemId());
  }

  public void skipServiceAddress(final SkipServiceAddressRequest request) {
    Preconditions.checkArgument(!request.getUnsortedServiceAddressQueueItemId().isEmpty(),
        "Unsorted service address queue item ID is required");
    Preconditions.checkArgument(request.getDelayMinutes() != 0, "Delay (in minutes) is required");
    delayQueueItem(request.getUnsortedServiceAddressQueueItemId(), request.getDelayMinutes());
  }

  private IpDatastoreSg3.ThinServiceAddress getThinServiceAddress(long serviceAddressId) {
    ServiceAddress serviceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(serviceAddressId)
            .build()
    );
    IpDatastoreSg3.ThinServiceAddress.Builder thinServiceAddressBuilder = IpDatastoreSg3.ThinServiceAddress.newBuilder();
    if (serviceAddress.getLawFirmId() != null) {
      thinServiceAddressBuilder
          .setLawFirmId(serviceAddress.getLawFirmId().getValue())
          .setNotALawFirm(false);
    } else {
      thinServiceAddressBuilder.setNotALawFirm(true);
    }
    thinServiceAddressBuilder
        .setServiceAddressId(serviceAddress.getServiceAddressId())
        .setNameAddress(serviceAddress.getAddress())
        .setName(serviceAddress.getName())
        .setCountry(serviceAddress.getCountry())
        .setLatitude(serviceAddress.getLatitude())
        .setLongitude(serviceAddress.getLongitude());
    return thinServiceAddressBuilder.build();
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

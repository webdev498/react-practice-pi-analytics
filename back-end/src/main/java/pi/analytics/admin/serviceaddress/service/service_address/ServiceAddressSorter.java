/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.concurrent.TimeUnit;

import pi.admin.service_address_sorting.generated.AssignServiceAddressRequest;
import pi.admin.service_address_sorting.generated.SetServiceAddressAsNonLawFirmRequest;
import pi.admin.service_address_sorting.generated.SkipServiceAddressRequest;
import pi.admin.service_address_sorting.generated.UnsortServiceAddressRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub;
import pi.ip.generated.datastore_sg3.DatastoreSg3ServiceGrpc.DatastoreSg3ServiceBlockingStub;
import pi.ip.generated.queue.AddUnitRequestOnPrem;
import pi.ip.generated.queue.DelayUnitRequest;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.MsgUnit;
import pi.ip.generated.queue.QueueNameOnPrem;
import pi.ip.generated.queue.QueueOnPremGrpc.QueueOnPremBlockingStub;

/**
 * @author shane.xie@practiceinsight.io
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
    // TODO

    // serviceAddressServiceBlockingStub.assignServiceAddressToLawFirm()
    // datastoreSg3ServiceBlockingStub.upsertThinLawFirmServiceAddress()
    // deleteQueueItem(request.getUnsortedServiceAddressQueueItemId())

    throw new NotImplementedException();
  }

  public void unsortServiceAddress(final UnsortServiceAddressRequest request) {
    // TODO

    // serviceAddressServiceBlockingStub.unassignServiceAddressFromLawFirm()
    // datastoreSg3ServiceBlockingStub.deleteThinLawFirmServiceAddress()
    // addQueueItem(request.getServiceAddressId())

    throw new NotImplementedException();
  }

  public void setServiceAddressAsNonLawFirm(final SetServiceAddressAsNonLawFirmRequest request) {
    // TODO

    // serviceAddressServiceBlockingStub.setServiceAddressAsNonLawFirm()
    // deleteQueueItem(request.getUnsortedServiceAddressQueueItemId())

    throw new NotImplementedException();
  }

  public void skipServiceAddress(final SkipServiceAddressRequest request) {
    delayQueueItem(request.getUnsortedServiceAddressQueueItemId(), request.getDelayMinutes());
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

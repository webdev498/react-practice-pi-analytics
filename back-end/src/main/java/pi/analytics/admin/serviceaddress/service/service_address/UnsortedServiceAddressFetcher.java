/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.pi.common.core.queue.OfficeCodeListsActive;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.DeleteUnitRequest;
import pi.ip.generated.queue.QueueNameOnPrem;
import pi.ip.generated.queue.QueueOnPremGrpc;
import pi.ip.generated.queue.StoredMsgUnit;
import pi.ip.generated.queue.UnitRequestOnPrem;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class UnsortedServiceAddressFetcher {

  @Inject
  private QueueOnPremGrpc.QueueOnPremBlockingStub queueOnPremBlockingStub;

  @Inject
  private ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public Optional<QueuedServiceAddress> fetchNext(final String username) {
    return getQueueNamesForUser(username)
        .stream()
        .map(this::fetchNextValidQueuedServiceAddress)
        // Skip empty queues
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  private List<QueueNameOnPrem> getQueueNamesForUser(final String userName) {
    if (StringUtils.equalsIgnoreCase(userName, "hellen")) {
      return ImmutableList.of(
          QueueNameOnPrem.ServiceAddrSort_zh,
          QueueNameOnPrem.ServiceAddrSort_ja
      );
    } else {
      return ImmutableList.of(
          QueueNameOnPrem.ServiceAddrSort_en,
          QueueNameOnPrem.ServiceAddrSort_de,
          QueueNameOnPrem.ServiceAddrSort_kr,
          QueueNameOnPrem.ServiceAddrSort_ja
      );
    }
  }

  private Optional<QueuedServiceAddress> fetchNextValidQueuedServiceAddress(final QueueNameOnPrem queueNameOnPrem) {
    Optional<StoredMsgUnit> nextQueueItem = Optional.of(StoredMsgUnit.getDefaultInstance());
    Optional<QueuedServiceAddress> validQueuedServiceAddress = Optional.empty();

    while (nextQueueItem.isPresent() && !validQueuedServiceAddress.isPresent()) {
      nextQueueItem = fetchNextQueueItem(queueNameOnPrem);
      validQueuedServiceAddress =
          nextQueueItem
              .map(this::fetchServiceAddress)
              .map(this::pruneUnhandledQueueItem)
              .filter(queuedServiceAddress -> queuedServiceAddress.serviceAddress().isPresent());
    }
    return validQueuedServiceAddress;
  }

  private Optional<StoredMsgUnit> fetchNextQueueItem(final QueueNameOnPrem queueNameOnPrem) {
    UnitRequestOnPrem unitRequestOnPrem =
        UnitRequestOnPrem
            .newBuilder()
            .setQueueNameOnPrem(queueNameOnPrem)
            .setLockTimeSeconds(240)
            .build();
    StoredMsgUnit storedMsgUnit = queueOnPremBlockingStub.getNextQueueUnit(unitRequestOnPrem);
    if (StringUtils.isEmpty(storedMsgUnit.getDbId())) {
      // End of this queue
      return Optional.empty();
    } else {
      return Optional.of(storedMsgUnit);
    }
  }

  private QueuedServiceAddress fetchServiceAddress(final StoredMsgUnit storedMsgUnit) {
    final GetServiceAddressByIdRequest fetchRequest =
        GetServiceAddressByIdRequest
            .newBuilder()
            .setServiceAddressId(getServiceAddressId(storedMsgUnit))
            .build();
    try {
      final ServiceAddress serviceAddress = serviceAddressServiceBlockingStub.getServiceAddressById(fetchRequest);
      return QueuedServiceAddress.create(storedMsgUnit.getDbId(), Optional.of(serviceAddress));
    } catch (StatusRuntimeException sre) {
      if (sre.getStatus().equals(Status.NOT_FOUND)) {
        return QueuedServiceAddress.create(storedMsgUnit.getDbId(), Optional.empty());
      }
      // Any other status is an error
      throw sre;
    }
  }

  /**
   * Delete queue items that we can't, or aren't interested in handling
   * Contains Optional<ServiceAddress>.empty() if the service address is to be skipped
   */
  private QueuedServiceAddress pruneUnhandledQueueItem(final QueuedServiceAddress queuedServiceAddress) {
    if (!queuedServiceAddress.serviceAddress().isPresent()) {
      // Service address doesn't exist and we can't process it
      deleteQueueItem(queuedServiceAddress.queueId());
    } else if (queuedServiceAddress.serviceAddress().get().getLawFirmStatusDetermined()) {
      // Service address has already been sorted
      deleteQueueItem(queuedServiceAddress.queueId());
      // Indicate that the service address should be skipped
      return QueuedServiceAddress.create(queuedServiceAddress.queueId(), Optional.empty());
    } else {
      final List<String> activeOfficeCodes =
          Arrays
              .stream(OfficeCodeListsActive.PatentOfficeCode.values())
              .map(patentOfficeCode -> patentOfficeCode.name().toUpperCase())
              .collect(Collectors.toList());
      if (!activeOfficeCodes.contains(queuedServiceAddress.serviceAddress().get().getCountry().toUpperCase())) {
        // We are not interested in this office code
        deleteQueueItem(queuedServiceAddress.queueId());
        // Indicate that the service address should be skipped
        return QueuedServiceAddress.create(queuedServiceAddress.queueId(), Optional.empty());
      }
    }
    return queuedServiceAddress;
  }

  private long getServiceAddressId(final StoredMsgUnit storedMsgUnit) {
    return Long.parseLong(storedMsgUnit.getMsgUnit().getUniqueMsgKey());
  }

  private void deleteQueueItem(final String queueId) {
    final DeleteUnitRequest deleteUnitRequest = DeleteUnitRequest.newBuilder().setDbId(queueId).build();
    queueOnPremBlockingStub.deleteQueueUnit(deleteUnitRequest);
  }
}

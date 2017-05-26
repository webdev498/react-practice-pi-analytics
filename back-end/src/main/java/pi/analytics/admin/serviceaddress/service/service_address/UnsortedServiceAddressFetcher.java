/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pi.analytics.admin.serviceaddress.service.QueuedServiceAddress;
import pi.ip.data.relational.generated.GetServiceAddressByIdRequest;
import pi.ip.data.relational.generated.IsHighValueServiceAddressRequest;
import pi.ip.data.relational.generated.ServiceAddressServiceGrpc;
import pi.ip.generated.queue.DelayUnitRequest;
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

  private static final Logger log = LoggerFactory.getLogger(UnsortedServiceAddressFetcher.class);
  private static final String SG_ONLY_USER = "sgonly";

  @Inject
  private QueueOnPremGrpc.QueueOnPremBlockingStub queueOnPremBlockingStub;

  @Inject
  private ServiceAddressServiceGrpc.ServiceAddressServiceBlockingStub serviceAddressServiceBlockingStub;

  public Optional<QueuedServiceAddress> fetchNext(final String username) {
    return getQueueNamesForUser(username)
        .stream()
        .map(queueNameOnPrem -> fetchNextValidQueuedServiceAddress(queueNameOnPrem, username))
        // Skip empty queues
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  private List<QueueNameOnPrem> getQueueNamesForUser(final String userName) {
    if (StringUtils.equalsIgnoreCase(userName, "hellen")) {
      return ImmutableList.of(
          QueueNameOnPrem.ServiceAddrSort_zh,
          QueueNameOnPrem.ServiceAddrSort_en,
          QueueNameOnPrem.ServiceAddrSort_ja
      );
    } else if (StringUtils.equalsIgnoreCase(userName, SG_ONLY_USER)) {
      return ImmutableList.of(
          QueueNameOnPrem.ServiceAddrSort_en
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

  private Optional<QueuedServiceAddress> fetchNextValidQueuedServiceAddress(final QueueNameOnPrem queueNameOnPrem,
                                                                            final String username) {
    Optional<StoredMsgUnit> nextQueueItem = Optional.of(StoredMsgUnit.getDefaultInstance());
    Optional<QueuedServiceAddress> validQueuedServiceAddress = Optional.empty();

    while (nextQueueItem.isPresent() && !validQueuedServiceAddress.isPresent()) {
      nextQueueItem = fetchNextQueueItem(queueNameOnPrem);
      validQueuedServiceAddress =
          nextQueueItem
              .map(this::fetchServiceAddress)
              .map(this::pruneUnhandledQueueItem)
              .map(queuedServiceAddress -> skipLowPriorityServiceAddress(queuedServiceAddress, username))
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

  private QueuedServiceAddress filterSgOnlyServiceAddresses(final QueuedServiceAddress queuedServiceAddress,
                                                            final String username) {
    if (username.equals(SG_ONLY_USER)
          && queuedServiceAddress.serviceAddress().isPresent()
          && !queuedServiceAddress.serviceAddress().get().getCountry().equalsIgnoreCase("sg")) {
      return indicateToBeSkipped(queuedServiceAddress);
    }
    return queuedServiceAddress;
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
      return indicateToBeSkipped(queuedServiceAddress);
    } else if (!activeCountryCodes().contains(queuedServiceAddress.serviceAddress().get().getCountry().toUpperCase())) {
      // We are not interested in this country code
      deleteQueueItem(queuedServiceAddress.queueId());
      return indicateToBeSkipped(queuedServiceAddress);
    }
    return queuedServiceAddress;
  }

  private QueuedServiceAddress skipLowPriorityServiceAddress(final QueuedServiceAddress queuedServiceAddress,
                                                             final String username) {
    final boolean skip =
        queuedServiceAddress
            .serviceAddress()
            .flatMap(serviceAddress -> {
              if (username.equals(SG_ONLY_USER) && !serviceAddress.getCountry().equalsIgnoreCase("sg")) {
                return Optional.of(true);
              }
              if (serviceAddress.getCountry().equals("TW")) {
                return Optional.of(true);  // Skip Taiwan for now
              }
              try {
                final IsHighValueServiceAddressRequest request =
                    IsHighValueServiceAddressRequest
                        .newBuilder()
                        .setServiceAddressId(serviceAddress.getServiceAddressId())
                        .build();
                return Optional.of(
                    !serviceAddressServiceBlockingStub.isHighValueServiceAddress(request).getIsHighValue()
                );
              } catch (Exception e) {
                // Only skip if we're certain that the service address isn't of high value
                return Optional.of(false);
              }
            })
            .orElse(false);
    if (skip) {
      DelayUnitRequest delayUnitRequest =
          DelayUnitRequest
              .newBuilder()
              .setDbId(queuedServiceAddress.queueId())
              .setDelaySeconds((int) TimeUnit.MINUTES.toSeconds(30))
              .build();
      queueOnPremBlockingStub.delayQueueUnit(delayUnitRequest);
      log.info("Skipping unsorted service address id {}", queuedServiceAddress.serviceAddress().get().getServiceAddressId());
      return indicateToBeSkipped(queuedServiceAddress);
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

  private Set<String> activeCountryCodes() {
    return ImmutableSet.of("AE", "AT", "AU", "BE", "BG", "BR", "CA", "CH", "CL", "CN",
        "CO", "CU", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GB", "GR", "HK", "HR", "HU", "IE", "IL", "IN", "IS", "IT",
        "JP", "KR", "LI", "LU", "LV", "MP", "MX", "MY", "NL", "NO", "NZ", "PL", "PR", "PT", "RO", "RU", "SE", "SG", "SI",
        "SK", "TH", "TR", "TW", "US", "ZA"
    );
  }

  private QueuedServiceAddress indicateToBeSkipped(final QueuedServiceAddress queuedServiceAddress) {
    return QueuedServiceAddress.create(queuedServiceAddress.queueId(), Optional.empty());
  }
}

/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;

import java.util.function.Predicate;

import pi.ip.data.relational.generated.SortResult;
import pi.ip.data.relational.generated.SortStatus;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressUtils {

  public static boolean isSorted(final ServiceAddress serviceAddress) {
    return serviceAddress.getLawFirmStatusDetermined();
  }

  public static boolean isAssignedToLawFirm(final ServiceAddress serviceAddress) {
    return isSorted(serviceAddress) && serviceAddress.hasLawFirmId();
  }

  public static boolean isNonLawFirm(final ServiceAddress serviceAddress) {
    return isSorted(serviceAddress) && !serviceAddress.hasLawFirmId();
  }

  public static SortResult getSortResult(final ServiceAddress preSort, final ServiceAddress postSort) {
    Preconditions.checkArgument(preSort.getServiceAddressId() == postSort.getServiceAddressId(),
        "Pre-sort and post-sort service address ids are different");

    if (!isSorted(postSort)) {
      return SortResult.NONE;
    }
    if (!isSorted(preSort) && isSorted(postSort)) {
      return SortResult.NEW_SORT;
    }
    if (preSort.getLawFirmId() == postSort.getLawFirmId()) {
      return SortResult.SAME;
    }
    if (preSort.getLawFirmId() != postSort.getLawFirmId()) {
      return SortResult.DIFFERENT;
    }
    return SortResult.NONE;
  }

  public static SortResult getAssignToLawFirmSortResult(final ServiceAddress preSort, final long assignedLawFirmId) {
    Preconditions.checkArgument(assignedLawFirmId != 0, "Invalid assigned law firm id");

    if (!preSort.getLawFirmStatusDetermined()) {
      return SortResult.NEW_SORT;
    }
    if (preSort.getLawFirmId().getValue() == assignedLawFirmId) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult getCreateLawFirmAndAssignSortResult(final ServiceAddress preSort, final LawFirm newLawFirm,
                                                               final Predicate<LawFirm> similarLawFirmExists) {
    if (!preSort.getLawFirmStatusDetermined()) {
      return SortResult.NEW_SORT;
    }
    if (similarLawFirmExists.test(newLawFirm)) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult getSetAsNonLawFirmSortResult(final ServiceAddress preSort) {
    if (!preSort.getLawFirmStatusDetermined()) {
      return SortResult.NEW_SORT;
    }
    if (!preSort.hasLawFirmId()) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortStatus getDesiredSortStatus(final ServiceAddress preSort, final String username,
                                                final Predicate<String> canPerformRealSort) {
    if (canPerformRealSort.test(username)) {
      if (!isSorted(preSort)) {
        return SortStatus.SORT_APPLIED;
      }
      return SortStatus.SORT_SCORE_UPDATED;
    }
    return SortStatus.DRY_RUN_NOT_UPDATED;
  }
}

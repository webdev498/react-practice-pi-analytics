/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;

import java.util.function.Predicate;

import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;
import pi.ip.proto.generated.SortResult;
import pi.ip.proto.generated.SortStatus;

import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.isNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.needsSorting;

/**
 * @author shane.xie@practiceinsight.io
 */
public class SortResultUtils {

  public static SortResult resultOfAssignToLawFirm(final ServiceAddress preSort, final long assignedLawFirmId) {
    Preconditions.checkArgument(assignedLawFirmId != 0, "Invalid assigned law firm id");

    if (needsSorting(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (preSort.getLawFirmId().getValue() == assignedLawFirmId) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult resultOfCreateLawFirmAndAssign(final ServiceAddress preSort, final LawFirm newLawFirm,
                                                          final Predicate<LawFirm> similarLawFirmExists) {
    if (needsSorting(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (isNonLawFirm(preSort)) {
      return SortResult.DIFFERENT;
    }
    if (similarLawFirmExists.test(newLawFirm)) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult resultOfSetAsNonLawFirm(final ServiceAddress preSort) {
    if (needsSorting(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (isNonLawFirm(preSort)) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult resultOfSetInsufficientInfoToSort(final ServiceAddress preSort) {
    if (needsSorting(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (preSort.getSortStatus() == SortStatus.INSUFFICIENT_INFO) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }
}

/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;

import java.util.function.Predicate;

import pi.ip.data.relational.generated.SortResult;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.isNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.ServiceAddressUtils.isSorted;

/**
 * @author shane.xie@practiceinsight.io
 */
public class SortResultUtils {

  public static SortResult resultOfAssignToLawFirm(final ServiceAddress preSort, final long assignedLawFirmId) {
    Preconditions.checkArgument(assignedLawFirmId != 0, "Invalid assigned law firm id");

    if (!isSorted(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (preSort.getLawFirmId().getValue() == assignedLawFirmId) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }

  public static SortResult resultOfCreateLawFirmAndAssign(final ServiceAddress preSort, final LawFirm newLawFirm,
                                                          final Predicate<LawFirm> similarLawFirmExists) {
    if (!isSorted(preSort)) {
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
    if (!isSorted(preSort)) {
      return SortResult.NEW_SORT;
    }
    if (isNonLawFirm(preSort)) {
      return SortResult.SAME;
    }
    return SortResult.DIFFERENT;
  }
}

/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.common.base.Preconditions;

import pi.ip.data.relational.generated.SortResult;
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
}

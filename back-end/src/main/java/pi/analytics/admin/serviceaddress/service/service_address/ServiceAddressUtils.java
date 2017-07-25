/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import pi.ip.proto.generated.ServiceAddress;
import pi.ip.proto.generated.SortStatus;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressUtils {

  public static boolean needsSorting(final ServiceAddress serviceAddress) {
    return !serviceAddress.getLawFirmStatusDetermined() || serviceAddress.getSortStatus() == SortStatus.SORT_PENDING;
  }

  public static boolean isAssignedToLawFirm(final ServiceAddress serviceAddress) {
    return (!needsSorting(serviceAddress) || serviceAddress.getSortStatus() == SortStatus.LAW_FIRM_SORTED)
        && serviceAddress.hasLawFirmId();
  }

  public static boolean isNonLawFirm(final ServiceAddress serviceAddress) {
    return !needsSorting(serviceAddress)
        && !serviceAddress.hasLawFirmId()
        && serviceAddress.getSortStatus() == SortStatus.APPLICANT_FILED_SORTED;
  }
}

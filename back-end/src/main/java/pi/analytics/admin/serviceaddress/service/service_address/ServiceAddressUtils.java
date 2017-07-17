/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

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
}

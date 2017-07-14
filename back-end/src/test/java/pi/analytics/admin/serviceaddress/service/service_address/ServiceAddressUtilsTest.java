/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.github.javafaker.Faker;

import org.junit.Test;

import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper.createLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressUtilsTest {

  private Faker faker = new Faker();

  @Test
  public void isSorted() throws Exception {
    final ServiceAddress unsortedServiceAddress = createUnsortedServiceAddress(faker.company().name());
    assertThat(ServiceAddressUtils.isSorted(unsortedServiceAddress)).isFalse();

    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress sortedServiceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    assertThat(ServiceAddressUtils.isSorted(sortedServiceAddress)).isTrue();
  }

  @Test
  public void isAssignedToLawFirm() throws Exception {
    final ServiceAddress nonLawFirmServiceAddress = createServiceAddressForNonLawFirm(faker.company().name());
    assertThat(ServiceAddressUtils.isAssignedToLawFirm(nonLawFirmServiceAddress)).isFalse();

    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress lawFirmServiceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    assertThat(ServiceAddressUtils.isAssignedToLawFirm(lawFirmServiceAddress)).isTrue();
  }

  @Test
  public void isNonLawFirm() throws Exception {
    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress lawFirmServiceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    assertThat(ServiceAddressUtils.isNonLawFirm(lawFirmServiceAddress)).isFalse();

    final ServiceAddress nonLawFirmServiceAddress = createServiceAddressForNonLawFirm(faker.company().name());
    assertThat(ServiceAddressUtils.isNonLawFirm(nonLawFirmServiceAddress)).isTrue();
  }
}
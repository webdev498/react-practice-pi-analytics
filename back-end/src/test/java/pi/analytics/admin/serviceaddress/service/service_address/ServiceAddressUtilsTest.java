/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.github.javafaker.Faker;

import org.junit.Test;

import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;
import pi.ip.proto.generated.SortStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper.createLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddress;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressUtilsTest {

  private Faker faker = new Faker();

  @Test
  public void needsSorting() throws Exception {
    final ServiceAddress unsortedServiceAddress = createUnsortedServiceAddress(faker.company().name());
    assertThat(ServiceAddressUtils.needsSorting(unsortedServiceAddress))
        .as("This service address is unsorted")
        .isTrue();

    assertThat(ServiceAddressUtils.needsSorting(createServiceAddressForNonLawFirm(faker.name().fullName())))
        .as("This service address is already sorted as a non law firm")
        .isFalse();

    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress lawFirmSortedServiceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    assertThat(ServiceAddressUtils.needsSorting(lawFirmSortedServiceAddress))
        .as("This service address is already assigned to a law firm")
        .isFalse();

    assertThat(ServiceAddressUtils.needsSorting(createServiceAddress(SortStatus.INSUFFICIENT_INFO)))
        .as("This service address cannot be sorted")
        .isFalse();

    assertThat(ServiceAddressUtils.needsSorting(createServiceAddress(SortStatus.SMALL_APPLICANT)))
        .as("This service address cannot be sorted")
        .isFalse();

    assertThat(ServiceAddressUtils.needsSorting(createServiceAddress(SortStatus.UNTRACKED_COUNTRY)))
        .as("This service address cannot be sorted")
        .isFalse();
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
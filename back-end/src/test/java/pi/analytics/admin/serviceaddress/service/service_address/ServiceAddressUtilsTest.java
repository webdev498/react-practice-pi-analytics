/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.protobuf.Int64Value;

import com.github.javafaker.Faker;

import org.junit.Test;

import pi.ip.data.relational.generated.SortResult;
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

  @Test
  public void sortResult_none() throws Exception {
    final ServiceAddress preSort = createServiceAddressForNonLawFirm(faker.company().name());
    final ServiceAddress postSort =
        ServiceAddress
            .newBuilder(preSort)
            .setLawFirmStatusDetermined(false)
            .build();
    assertThat(ServiceAddressUtils.getSortResult(preSort, postSort))
        .as("The post sort service address is unsorted")
        .isEqualTo(SortResult.NONE);
  }

  @Test
  public void sortResult_new_non_law_firm() throws Exception {
    final ServiceAddress preSort = createUnsortedServiceAddress(faker.company().name());
    final ServiceAddress postSort =
        ServiceAddress
            .newBuilder(preSort)
            .setLawFirmStatusDetermined(true)
            .build();
    assertThat(ServiceAddressUtils.getSortResult(preSort, postSort))
        .as("The pre sort service address is unsorted and the post sort service address is sorted")
        .isEqualTo(SortResult.NEW_SORT);
  }

  @Test
  public void sortResult_new_assigned_to_law_firm() throws Exception {
    final ServiceAddress preSort = createUnsortedServiceAddress(faker.company().name());
    final ServiceAddress postSort =
        ServiceAddress
            .newBuilder(preSort)
            .setLawFirmId(Int64Value.newBuilder().setValue(faker.number().randomNumber()))
            .setLawFirmStatusDetermined(true)
            .build();
    assertThat(ServiceAddressUtils.getSortResult(preSort, postSort))
        .as("The pre sort service address is unsorted and the post sort service address is sorted")
        .isEqualTo(SortResult.NEW_SORT);
  }

  @Test
  public void sortResult_same() throws Exception {
    final LawFirm lawFirm = createLawFirm();
    final ServiceAddress lawFirmServiceAddress = createServiceAddressToMatchLawFirm(lawFirm);
    assertThat(ServiceAddressUtils.getSortResult(lawFirmServiceAddress, lawFirmServiceAddress))
        .as("The pre sort service address and the post sort service address are assigned to the same law firm")
        .isEqualTo(SortResult.SAME);

    final ServiceAddress nonLawFirmServiceAddress = createServiceAddressForNonLawFirm(faker.company().name());
    assertThat(ServiceAddressUtils.getSortResult(nonLawFirmServiceAddress, nonLawFirmServiceAddress))
        .as("The pre sort service address and the post sort service address are both set as non law firm")
        .isEqualTo(SortResult.SAME);
  }

  @Test
  public void sortResult_different() throws Exception {
    final ServiceAddress nonLawFirmServiceAddress = createServiceAddressForNonLawFirm(faker.company().name());
    final ServiceAddress lawFirmServiceAddress =
        ServiceAddress
            .newBuilder(nonLawFirmServiceAddress)
            .setLawFirmId(Int64Value.newBuilder().setValue(faker.number().randomNumber()))
            .build();
    assertThat(ServiceAddressUtils.getSortResult(lawFirmServiceAddress, nonLawFirmServiceAddress))
        .as("The pre sort service address and the post sort service address are sorted differently")
        .isEqualTo(SortResult.DIFFERENT);
    assertThat(ServiceAddressUtils.getSortResult(nonLawFirmServiceAddress, lawFirmServiceAddress))
        .as("The pre sort service address and the post sort service address are sorted differently")
        .isEqualTo(SortResult.DIFFERENT);
  }

  @Test
  public void getAssignToLawFirmSortResult_different() throws Exception {
    // TODO(SX)
    assert(false);
  }

  @Test
  public void getAssignToLawFirmSortResult_same() throws Exception {
    // TODO(SX)
    assert(false);
  }
}
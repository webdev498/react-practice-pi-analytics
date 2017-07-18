/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.github.javafaker.Faker;

import org.junit.Test;

import java.util.function.Predicate;

import pi.ip.data.relational.generated.SortResult;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.SortStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static pi.analytics.admin.serviceaddress.service.helpers.LawFirmTestHelper.createLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddress;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressForNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createServiceAddressToMatchLawFirm;
import static pi.analytics.admin.serviceaddress.service.helpers.ServiceAddressTestHelper.createUnsortedServiceAddress;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfAssignToLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfCreateLawFirmAndAssign;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfSetAsNonLawFirm;
import static pi.analytics.admin.serviceaddress.service.service_address.SortResultUtils.resultOfSetInsufficientInfoToSort;

/**
 * @author shane.xie@practiceinsight.io
 */
public class SortResultUtilsTest {

  private Faker faker = new Faker();

  @Test
  public void testResultOfAssignToLawFirm() throws Exception {
    final LawFirm lawFirm = createLawFirm();

    assertThat(resultOfAssignToLawFirm(createUnsortedServiceAddress(faker.company().name()), lawFirm.getLawFirmId()))
        .as("The service address is unsorted")
        .isEqualTo(SortResult.NEW_SORT);

    assertThat(resultOfAssignToLawFirm(createServiceAddressToMatchLawFirm(lawFirm), lawFirm.getLawFirmId()))
        .as("The service address is assigned to the same law firm")
        .isEqualTo(SortResult.SAME);

    assertThat(resultOfAssignToLawFirm(createServiceAddressToMatchLawFirm(lawFirm), lawFirm.getLawFirmId() + 1))
        .as("The service address is assigned a different law firm")
        .isEqualTo(SortResult.DIFFERENT);

    assertThat(resultOfAssignToLawFirm(createServiceAddressForNonLawFirm(faker.company().name()), lawFirm.getLawFirmId()))
        .as("The service address is not assigned to a law firm")
        .isEqualTo(SortResult.DIFFERENT);
  }

  @Test
  public void testResultOfCreateLawFirmAndAssign() throws Exception {
    final LawFirm lawFirm = createLawFirm();

    final Predicate<LawFirm> lawFirmFound = lf -> true;
    final Predicate<LawFirm> lawFirmNotFound = lf -> false;

    assertThat(resultOfCreateLawFirmAndAssign(createUnsortedServiceAddress(faker.company().name()), lawFirm, lawFirmFound))
        .as("The service address is unsorted")
        .isEqualTo(SortResult.NEW_SORT);

    assertThat(resultOfCreateLawFirmAndAssign(createServiceAddressToMatchLawFirm(lawFirm), lawFirm, lawFirmFound))
        .as("The service address is assigned to a law firm created with the same details")
        .isEqualTo(SortResult.SAME);

    assertThat(resultOfCreateLawFirmAndAssign(createServiceAddressToMatchLawFirm(lawFirm), lawFirm, lawFirmNotFound))
        .as("The service address is assigned to a law firm created with different details")
        .isEqualTo(SortResult.DIFFERENT);

    assertThat(resultOfCreateLawFirmAndAssign(
            createServiceAddressForNonLawFirm(faker.company().name()), lawFirm, lawFirmFound))
        .as("The service address is not a law firm")
        .isEqualTo(SortResult.DIFFERENT);
  }

  @Test
  public void testResultOfSetAsNonLawFirm() throws Exception {
    assertThat(resultOfSetAsNonLawFirm(createUnsortedServiceAddress(faker.company().name())))
        .as("The service address is unsorted")
        .isEqualTo(SortResult.NEW_SORT);

    assertThat(resultOfSetAsNonLawFirm(createServiceAddressForNonLawFirm(faker.company().name())))
        .as("The service address is not a law firm")
        .isEqualTo(SortResult.SAME);

    assertThat(resultOfSetAsNonLawFirm(createServiceAddressToMatchLawFirm(createLawFirm())))
        .as("The service address is assigned to a law firm")
        .isEqualTo(SortResult.DIFFERENT);
  }

  @Test
  public void testResultOfSetInsufficientInfoToSort() throws Exception {
    assertThat(resultOfSetInsufficientInfoToSort(createServiceAddress(SortStatus.PENDING)))
        .as("The service address is unsorted")
        .isEqualTo(SortResult.NEW_SORT);

    assertThat(resultOfSetInsufficientInfoToSort(createServiceAddress(SortStatus.INSUFFICIENT_INFO)))
        .as("The service address is already set as insufficient info to sort")
        .isEqualTo(SortResult.SAME);

    assertThat(resultOfSetInsufficientInfoToSort(createServiceAddress(SortStatus.APPLICANT_FILED_SORTED)))
        .as("The service address is sorted")
        .isEqualTo(SortResult.DIFFERENT);
  }
}
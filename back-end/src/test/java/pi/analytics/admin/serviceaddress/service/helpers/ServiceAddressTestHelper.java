/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.helpers;

import com.google.protobuf.Int64Value;

import com.github.javafaker.Faker;

import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;
import pi.ip.proto.generated.SortStatus;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressTestHelper {

  private static Faker faker = new Faker();

  public static ServiceAddress createServiceAddress(final SortStatus sortStatus) {
    return createServiceAddress(faker.name().fullName(), faker.address().countryCode(), sortStatus);
  }

  public static ServiceAddress createServiceAddressToMatchLawFirm(final LawFirm lawFirm) {
    return ServiceAddress
        .newBuilder(createServiceAddress(lawFirm.getName(), lawFirm.getCountry(), SortStatus.LAW_FIRM_SORTED))
        .setLawFirmId(Int64Value.newBuilder().setValue(lawFirm.getLawFirmId()))
        .build();
  }

  public static ServiceAddress createServiceAddressForNonLawFirm(final String name) {
    return ServiceAddress
        .newBuilder(createServiceAddress(name, faker.address().countryCode(), SortStatus.APPLICANT_FILED_SORTED))
        .build();
  }

  public static ServiceAddress createUnsortedServiceAddress(final String name) {
    return createServiceAddress(name, faker.address().countryCode(), SortStatus.PENDING);
  }

  private static ServiceAddress createServiceAddress(final String name, final String country, final SortStatus sortStatus) {
    ServiceAddress.Builder builder =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(faker.number().randomNumber(8, true))
            .setName(name)
            .setAddress(faker.address().streetAddress(true))
            .setCountry(country)
            .setTelephone(faker.phoneNumber().phoneNumber())
            .setLawFirmStatusDetermined(sortStatus != SortStatus.PENDING)
            .setSortStatus(sortStatus)
            .setLanguageType(LangType.WESTERN_SCRIPT);
    return builder.build();
  }
}

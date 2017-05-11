/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.helpers;

import com.google.protobuf.Int64Value;

import com.github.javafaker.Faker;

import java.util.Optional;

import pi.ip.proto.generated.LangType;
import pi.ip.proto.generated.LawFirm;
import pi.ip.proto.generated.ServiceAddress;

/**
 * @author shane.xie@practiceinsight.io
 */
public class ServiceAddressTestHelper {

  private static Faker faker = new Faker();

  public static ServiceAddress createServiceAddressToMatchLawFirm(final LawFirm lawFirm) {
    return createServiceAddress(Optional.of(lawFirm.getLawFirmId()), lawFirm.getName(), lawFirm.getCountry());
  }

  public static ServiceAddress createServiceAddressForNonLawFirm(final String name) {
    return createServiceAddress(Optional.empty(), name, faker.address().countryCode());
  }

  public static ServiceAddress createServiceAddress(final Optional<Long> lawFirmId, final String name, final String
      country) {
    ServiceAddress.Builder builder =
        ServiceAddress
            .newBuilder()
            .setServiceAddressId(faker.number().randomNumber())
            .setName(name)
            .setAddress(faker.address().streetAddress(true))
            .setCountry(country)
            .setTelephone(faker.phoneNumber().phoneNumber())
            .setLawFirmStatusDetermined(true)
            .setLanguageType(LangType.WESTERN_SCRIPT);
    if (lawFirmId.isPresent()) {
      builder.setLawFirmId(Int64Value.newBuilder().setValue(lawFirmId.get()));
    }
    return builder.build();
  }
}

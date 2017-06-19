/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.helpers;

import com.github.javafaker.Faker;

import pi.ip.proto.generated.LawFirm;

/**
 * @author shane.xie@practiceinsight.io
 */
public class LawFirmTestHelper {

  private static Faker faker = new Faker();

  public static LawFirm createLawFirm() {
    return LawFirm
        .newBuilder()
        .setLawFirmId(faker.number().randomDigitNotZero())
        .setName(faker.company().name())
        .setStateStr(faker.address().state())
        .setCountry(faker.address().countryCode())
        .setWebsiteUrl(faker.internet().url())
        .build();
  }
}

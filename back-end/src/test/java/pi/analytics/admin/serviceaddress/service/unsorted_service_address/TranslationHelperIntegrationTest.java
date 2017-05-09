/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */
package pi.analytics.admin.serviceaddress.service.unsorted_service_address;

import com.pi.common.test.type.DisabledTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import pi.ip.proto.generated.LangType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
public class TranslationHelperIntegrationTest {

  @Category(DisabledTests.class)
  @Test()
  public void toEn() throws Exception {
    final TranslationHelper translationHelper = new TranslationHelper();
    assertThat(translationHelper.toEn("武士", LangType.JAPANESE)).isEqualTo("samurai");
  }
}
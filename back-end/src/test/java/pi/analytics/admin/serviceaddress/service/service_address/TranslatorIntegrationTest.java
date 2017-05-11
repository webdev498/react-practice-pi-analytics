/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */
package pi.analytics.admin.serviceaddress.service.service_address;

import com.pi.common.test.type.DisabledTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import pi.ip.proto.generated.LangType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
public class TranslatorIntegrationTest {

  @Category(DisabledTests.class)
  @Test()
  public void toEn() throws Exception {
    final Translator translator = new Translator();
    assertThat(translator.toEn("武士", LangType.JAPANESE)).isEqualTo("samurai");
  }
}
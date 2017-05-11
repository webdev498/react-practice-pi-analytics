/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.service_address;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.inject.Singleton;

import java.io.IOException;

import pi.ip.proto.generated.LangType;

/**
 * Translation service that uses the Google Cloud Translate API
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class Translator {

  private Translate translate;

  public Translator() {
    try {
      final GoogleCredentials credentials =
          GoogleCredentials
              .fromStream(
                  getClass().getClassLoader().getResourceAsStream("pi-gcp-google-translate-service-account.json")
              );
      translate =
          TranslateOptions
              .newBuilder()
              .setCredentials(credentials)
              .build()
              .getService();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public String toEn(final String text, final LangType sourceLanguage) {
      return translate
          .translate(
              text,
              TranslateOption.sourceLanguage(getLanguageString(sourceLanguage)),
              TranslateOption.targetLanguage("en")
          )
          .getTranslatedText();
  }

  private String getLanguageString(final LangType langType) {
    switch (langType) {
      case WESTERN_SCRIPT:
        return "en";
      case JAPANESE:
        return "ja";
      case CHINESE:
        return "zh";
      case KOREAN:
        return "ko";
      case CYRILLIC:
        return "ru";
      default:
        throw new IllegalArgumentException("Unknown LangType");
    }
  }
}

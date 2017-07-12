/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.user;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

import pi.ip.proto.generated.LangType;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class UserService {

  public boolean isStaff(final String username) {
    final Set<String> staffUsers = ImmutableSet.of(
        "floremer",
        "flor",
        "hellen",
        "janel",
        "shane",
        "thomas"
    );
    return staffUsers.contains(username.toLowerCase());
  }

  public float getAlreadySortedWeightedChance(final String username) {
    return isStaff(username) ? 0 : 1;
  }

  public Set<LangType> getLangTypes(final String username) {
    if (StringUtils.equalsIgnoreCase(username, "hellen")) {
      // Hellen specialises in sorting chinese addresses. Provide her with a reduced set that includes chinese.
      return ImmutableSet.of(
          LangType.CHINESE,
          LangType.WESTERN_SCRIPT,
          LangType.JAPANESE
      );
    } else {
      return ImmutableSet.of(
          LangType.WESTERN_SCRIPT,
          LangType.KOREAN,
          LangType.JAPANESE,
          LangType.CYRILLIC
      );
    }
  }
}

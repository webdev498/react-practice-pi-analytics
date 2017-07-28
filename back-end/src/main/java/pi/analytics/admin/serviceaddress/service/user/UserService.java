/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.user;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.Set;

import pi.ip.proto.generated.LangType;

/**
 * @author shane.xie@practiceinsight.io
 */
@Singleton
public class UserService {

  private boolean isStaff(final String username) {
    final Set<String> staffUsers = ImmutableSet.of(
        "floremer",
        "flor",
        "hellen",
        "ivan",
        "janel",
        "thomas"
    );
    return staffUsers.contains(username.toLowerCase());
  }

  private boolean isInterviewCandidate(final String username) {
    final Set<String> interviewCandidates = ImmutableSet.of(
        "shane",
        "doris"
    );
    return interviewCandidates.contains(username.toLowerCase());
  }

  public Optional<String> getPlaylist(final String username) {
    if (isInterviewCandidate(username)) {
      return Optional.of("interview_test");
    }
    return Optional.empty();
  }

  public boolean hasPlaylist(final String username) {
    return getPlaylist(username).isPresent();
  }

  public boolean canPerformRealSort(final String username) {
    return isStaff(username);
  }

  public float getAlreadySortedWeightedChance(final String username) {
    return isStaff(username) ? 0 : 1;
  }

  public Set<LangType> getLangTypes(final String username) {
    if (StringUtils.equalsIgnoreCase(username, "hellen")) {
      // Hellen specialises in sorting chinese addresses. Provide her with a reduced set that includes chinese.
      return ImmutableSet.of(
          LangType.CHINESE,
          LangType.WESTERN_SCRIPT
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

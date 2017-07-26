/*
 * Copyright (c) 2017 Practice Insight Pty Ltd.
 */

package pi.analytics.admin.serviceaddress.service.user;

import org.junit.Test;

import java.util.Optional;

import pi.ip.proto.generated.LangType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
public class UserServiceTest {

  private UserService userService = new UserService();

  @Test
  public void hasPlaylist() throws Exception {
    assertThat(userService.hasPlaylist("flor")).isFalse();
    assertThat(userService.hasPlaylist("shane")).isTrue();
  }

  @Test
  public void getPlayList() throws Exception {
    assertThat(userService.getPlaylist("shane")).isEqualTo(Optional.of("interview_test"));
  }

  @Test
  public void canPerformRealSort() throws Exception {
    assertThat(userService.canPerformRealSort("flor")).isTrue();
    assertThat(userService.canPerformRealSort("nonstaff")).isFalse();
  }

  @Test
  public void getAlreadySortedWeightedChance() throws Exception {
    assertThat(userService.getAlreadySortedWeightedChance("flor"))
        .as("Staff users currently always sort unsorted service addresses")
        .isEqualTo(0f);
    assertThat(userService.getAlreadySortedWeightedChance("nonstaff"))
        .as("Non staff users currently never sort unsorted service addresses")
        .isEqualTo(1f);
  }

  @Test
  public void getAlreadySortedWeightedChance_staff() throws Exception {
    assertThat(userService.getAlreadySortedWeightedChance("flor"))
        .isEqualTo(0)
        .as("User flor is a staff member and should always sort unsorted service addresses");
  }

  @Test
  public void getAlreadySortedWeightedChance_nonstaff() throws Exception {
    assertThat(userService.getAlreadySortedWeightedChance("trialuser"))
        .isEqualTo(1)
        .as("User trialuser is not a staff member and should always be given already-sorted service addresses");
  }

  @Test
  public void getLangTypes() throws Exception {
    assertThat(userService.getLangTypes("hellen"))
        .as("Hellen sorts Chinese, Korean and Japanese language types")
        .containsExactlyInAnyOrder(LangType.CHINESE);
    assertThat(userService.getLangTypes("anyuser"))
        .as("All other users sort Western, Korean, Japanese and Cyrillic language types")
        .containsExactlyInAnyOrder(LangType.WESTERN_SCRIPT, LangType.KOREAN, LangType.JAPANESE, LangType.CYRILLIC);
  }
}
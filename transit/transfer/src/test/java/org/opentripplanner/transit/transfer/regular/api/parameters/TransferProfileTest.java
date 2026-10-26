package org.opentripplanner.transit.transfer.regular.api.parameters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class TransferProfileParametersTest {

  @Test
  void aProfileWithoutPreferencesHasNoTypeConflict() {
    var subject = new TransferProfileParameters<>("walk", TransferProfileType.WALK, null, "r");

    assertThat(subject.userPreferences()).isNull();
    assertThat(subject.type()).isEqualTo(TransferProfileType.WALK);
  }

  @Test
  void thePreferencesAreThePreferencesOfTheType() {
    var subject = new TransferProfileParameters<>(
      "walk",
      TransferProfileType.WALK,
      WalkPreferences.DEFAULT,
      "r"
    );

    assertThat(subject.userPreferences().type()).isEqualTo(subject.type());
  }

  @Test
  void thePreferencesMustBeThePreferencesOfTheType() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      new TransferProfileParameters<>(
        "wheelchair",
        TransferProfileType.WHEELCHAIR,
        WalkPreferences.DEFAULT,
        "r"
      )
    );
    assertThat(ex.getMessage()).contains("WHEELCHAIR");
  }

  @Test
  void anIdIsRequired() {
    assertThrows(IllegalArgumentException.class, () ->
      new TransferProfileParameters<>(" ", TransferProfileType.WALK, null, "r")
    );
  }
}

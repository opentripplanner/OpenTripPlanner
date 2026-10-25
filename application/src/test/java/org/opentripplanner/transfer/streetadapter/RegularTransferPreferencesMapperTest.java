package org.opentripplanner.transfer.streetadapter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

class RegularTransferPreferencesMapperTest {

  private static final RegularTransferPreferencesMapper SUBJECT =
    new RegularTransferPreferencesMapper();

  private static final WalkPreferences PREFERENCES = WalkPreferences.of()
    .withSpeed(Speed.ofMetersPerSecond(1.1))
    .withReluctance(Reluctance.of(3.0))
    .build();

  @Test
  void toRequestAppliesThePreferencesOnTheBaseRequest() {
    var base = RouteRequest.defaultValue().copyOf().withNumItineraries(7).buildDefault();

    var request = SUBJECT.toRequest(PREFERENCES, base);

    assertThat(request.preferences().walk().speed()).isEqualTo(1.1);
    assertThat(request.preferences().walk().reluctance()).isEqualTo(3.0);
    assertThat(request.journey().transfer().mode()).isEqualTo(StreetMode.WALK);
    // The rest of the base request is kept
    assertThat(request.numItineraries()).isEqualTo(7);
  }

  @Test
  void preferencesOfTheBaseRequestAreOverridden() {
    var base = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(9.0).withReluctance(9.0)))
      .buildDefault();

    var request = SUBJECT.toRequest(PREFERENCES, base);

    assertThat(request.preferences().walk().speed()).isEqualTo(1.1);
    assertThat(request.preferences().walk().reluctance()).isEqualTo(3.0);
  }

  @Test
  void toUserPreferencesIsTheInverseOfToRequest() {
    var request = SUBJECT.toRequest(PREFERENCES, RouteRequest.defaultValue());

    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(PREFERENCES);
  }

  @Test
  void toUserPreferencesReadsTheWalkPreferencesOfARequest() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(1.2).withReluctance(4.0)))
      .buildDefault();

    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(
      WalkPreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(1.2))
        .withReluctance(Reluctance.of(4.0))
        .build()
    );
  }

  @Test
  void aWheelchairRequestHasWheelchairPreferences() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withWheelchair(true))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(0.9).withReluctance(4.0)))
      .buildDefault();

    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(
      WheelchairPreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(0.9))
        .withReluctance(Reluctance.of(4.0))
        .build()
    );
  }

  @Test
  void wheelchairPreferencesMakeAWheelchairRequest() {
    var preferences = WheelchairPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(0.9))
      .withReluctance(Reluctance.of(4.0))
      .build();

    var request = SUBJECT.toRequest(preferences, RouteRequest.defaultValue());

    assertThat(request.journey().wheelchair()).isTrue();
    assertThat(request.journey().transfer().mode()).isEqualTo(StreetMode.WALK);
    assertThat(request.preferences().walk().speed()).isEqualTo(0.9);
    assertThat(request.preferences().walk().reluctance()).isEqualTo(4.0);
    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(preferences);
  }

  @Test
  void walkPreferencesMakeARequestThatIsNotWheelchair() {
    var base = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withWheelchair(true))
      .buildDefault();

    var request = SUBJECT.toRequest(PREFERENCES, base);

    assertThat(request.journey().wheelchair()).isFalse();
  }

  @Test
  void aTransferModeWithoutAProfileTypeIsNotAccepted() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(new StreetRequest(StreetMode.BIKE)))
      .buildDefault();

    var ex = assertThrows(IllegalArgumentException.class, () -> SUBJECT.toUserPreferences(request));
    assertThat(ex.getMessage()).contains("BIKE");
  }
}

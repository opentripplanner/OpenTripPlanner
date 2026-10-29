package org.opentripplanner.transfer.streetadapter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicycleWalk;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.TimeSlopeSafetyTrianglePreference;
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
      .withJourney(j -> j.withTransfer(new StreetRequest(StreetMode.SCOOTER_RENTAL)))
      .buildDefault();

    var ex = assertThrows(IllegalArgumentException.class, () -> SUBJECT.toUserPreferences(request));
    assertThat(ex.getMessage()).contains("SCOOTER_RENTAL");
  }

  @Test
  void aBikeTransferRequestHasBicyclePreferences() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(new StreetRequest(StreetMode.BIKE)))
      .withPreferences(p ->
        p
          .withWalk(w -> w.withSpeed(1.0))
          // The walk preferences must not be used
          .withBike(b -> b.withSpeed(6.0).withReluctance(3.0))
      )
      .buildDefault();

    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(
      BicyclePreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(6.0))
        .withReluctance(Reluctance.of(3.0))
        .build()
    );
  }

  @Test
  void bicyclePreferencesMakeABikeRequest() {
    var preferences = BicyclePreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(6.0))
      .withReluctance(Reluctance.of(3.0))
      .build();

    var request = SUBJECT.toRequest(preferences, RouteRequest.defaultValue());

    assertThat(request.journey().transfer().mode()).isEqualTo(StreetMode.BIKE);
    assertThat(request.journey().wheelchair()).isFalse();
    assertThat(request.preferences().bike().speed()).isEqualTo(6.0);
    assertThat(request.preferences().bike().reluctance()).isEqualTo(3.0);
    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(preferences);
  }

  @Test
  void theUserPreferencesOfAProfileTypeAreTakenFromTheRequestPreferencesOfThatType() {
    // A profile without preferences of its own is of a type, but its request is a default request
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(1.0)).withBike(b -> b.withSpeed(6.0)))
      .buildDefault();

    var bicycle = (BicyclePreferences) SUBJECT.toUserPreferences(
      TransferProfileType.BICYCLE,
      request
    );
    var walk = (WalkPreferences) SUBJECT.toUserPreferences(TransferProfileType.WALK, request);

    assertThat(bicycle.speed()).isEqualTo(Speed.ofMetersPerSecond(6.0));
    assertThat(walk.speed()).isEqualTo(Speed.ofMetersPerSecond(1.0));
  }

  @Test
  void aCarTransferRequestHasCarPreferences() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(new StreetRequest(StreetMode.CAR)))
      .withPreferences(p -> p.withCar(c -> c.withReluctance(3.0)))
      .buildDefault();

    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(
      CarPreferences.of().withReluctance(Reluctance.of(3.0)).build()
    );
  }

  @Test
  void carPreferencesMakeACarRequest() {
    var preferences = CarPreferences.of().withReluctance(Reluctance.of(3.0)).build();

    var request = SUBJECT.toRequest(preferences, RouteRequest.defaultValue());

    assertThat(request.journey().transfer().mode()).isEqualTo(StreetMode.CAR);
    assertThat(request.journey().wheelchair()).isFalse();
    assertThat(request.preferences().car().reluctance()).isEqualTo(3.0);
    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(preferences);
  }

  @Test
  void theWalkingWithTheBicycleIsTheWalkingOfTheBikePreferences() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(StreetMode.BIKE))
      .withPreferences(p ->
        p
          .withWalk(w -> w.withSpeed(1.0).withReluctance(9.0))
          .withBike(b -> b.withWalking(w -> w.withSpeed(0.9).withReluctance(7.0)))
      )
      .buildDefault();

    var bicycle = (BicyclePreferences) SUBJECT.toUserPreferences(request);

    assertThat(bicycle.walk()).isEqualTo(
      BicycleWalk.of(Speed.ofMetersPerSecond(0.9), Reluctance.of(7.0))
    );
  }

  @Test
  void theWalkingWithTheBicycleMakesTheWalkingOfTheBikePreferences() {
    var preferences = BicyclePreferences.of()
      .withWalk(BicycleWalk.of(Speed.ofMetersPerSecond(0.9), Reluctance.of(7.0)))
      .build();

    var request = SUBJECT.toRequest(preferences, RouteRequest.defaultValue());

    assertThat(request.preferences().bike().walking().speed()).isEqualTo(0.9);
    assertThat(request.preferences().bike().walking().reluctance()).isEqualTo(7.0);
    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(preferences);
  }

  @Test
  void theSafetyFactorOfAWalkRequestIsTheSafetyFactorOfTheWalkPreferences() {
    var walkRequest = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSafetyFactor(0.4)))
      .buildDefault();

    var walk = (WalkPreferences) SUBJECT.toUserPreferences(walkRequest);

    assertThat(walk.safetyFactor()).isEqualTo(Ratio.of(0.4).normalize());
  }

  @Test
  void wheelchairRequestsThatDifferOnlyInTheWalkSafetyFactorHaveTheSamePreferences() {
    // The street search does not use the safety factor of a wheelchair request
    var wheelchairRequest = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withWheelchair(true))
      .buildDefault();
    var safeWheelchairRequest = wheelchairRequest
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSafetyFactor(0.4)))
      .buildDefault();

    assertThat(SUBJECT.toUserPreferences(safeWheelchairRequest)).isEqualTo(
      SUBJECT.toUserPreferences(wheelchairRequest)
    );
  }

  @Test
  void theSafetyFactorMakesTheSafetyFactorOfTheWalkPreferencesOfTheRequest() {
    var walk = WalkPreferences.of().withSafetyFactor(Ratio.of(0.4).normalize()).build();
    var base = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p -> p.withWalk(w -> w.withSafetyFactor(0.2)))
      .buildDefault();

    var walkRequest = SUBJECT.toRequest(walk, RouteRequest.defaultValue());
    var wheelchairRequest = SUBJECT.toRequest(WheelchairPreferences.DEFAULT, base);

    assertThat(walkRequest.preferences().walk().safetyFactor()).isEqualTo(0.4);
    // The safety factor of the base request is kept for a wheelchair request
    assertThat(wheelchairRequest.preferences().walk().safetyFactor()).isEqualTo(0.2);
    assertThat(SUBJECT.toUserPreferences(walkRequest)).isEqualTo(walk);
  }

  @Test
  void theOptimizationTypesOfABikeRequestAreTrianglesWithOneValueOfOne() {
    assertThat(optimizeOf(VehicleRoutingOptimizeType.SHORTEST_DURATION)).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0)
    );
    assertThat(optimizeOf(VehicleRoutingOptimizeType.FLAT_STREETS)).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.0, 1.0, 0.0)
    );
    assertThat(optimizeOf(VehicleRoutingOptimizeType.SAFE_STREETS)).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0)
    );
  }

  @Test
  void theTriangleOfABikeRequestIsNormalized() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(StreetMode.BIKE))
      .withPreferences(p ->
        p.withBike(b ->
          b
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(t -> t.withTime(2.0).withSlope(3.0).withSafety(5.0))
        )
      )
      .buildDefault();

    var bicycle = (BicyclePreferences) SUBJECT.toUserPreferences(request);

    assertThat(bicycle.optimize()).isEqualTo(TimeSlopeSafetyTrianglePreference.of(0.2, 0.3, 0.5));
  }

  @Test
  void theOptimizationMakesATriangleRequestAndIsReadBack() {
    var preferences = BicyclePreferences.of()
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(0.2, 0.3, 0.5))
      .build();

    var request = SUBJECT.toRequest(preferences, RouteRequest.defaultValue());

    assertThat(request.preferences().bike().optimizeType()).isEqualTo(
      VehicleRoutingOptimizeType.TRIANGLE
    );
    assertThat(request.preferences().bike().optimizeTriangle().time()).isEqualTo(0.2);
    assertThat(request.preferences().bike().optimizeTriangle().slope()).isEqualTo(0.3);
    assertThat(request.preferences().bike().optimizeTriangle().safety()).isEqualTo(0.5);
    assertThat(SUBJECT.toUserPreferences(request)).isEqualTo(preferences);
  }

  private static TimeSlopeSafetyTrianglePreference optimizeOf(VehicleRoutingOptimizeType type) {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(j -> j.withTransfer(StreetMode.BIKE))
      .withPreferences(p -> p.withBike(b -> b.withOptimizeType(type)))
      .buildDefault();
    return ((BicyclePreferences) SUBJECT.toUserPreferences(request)).optimize();
  }
}

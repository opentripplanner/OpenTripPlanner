package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.model.Cost.costOfSeconds;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class StreetSearchRequestMapperTest {

  private static final double DELTA = 0.00001;
  private static final Instant INSTANT = Instant.parse("2022-11-10T10:00:00Z");

  @Test
  void mapFromToCoordinates() {
    var builder = builder();

    var from = GenericLocation.fromCoordinate(10, 11);
    var to = GenericLocation.fromCoordinate(20, 21);

    var req = builder.withDateTime(INSTANT).withFrom(from).withTo(to).buildRequest();

    var subject = StreetSearchRequestMapper.mapInternal(req).build();

    assertEquals(INSTANT, subject.startTime());

    var fromEnvelope = subject.fromEnvelope();
    assertEquals(10.9954339685, fromEnvelope.getMinX(), DELTA);
    assertEquals(9.99550339902, fromEnvelope.getMinY(), DELTA);
    assertEquals(11.0045660314, fromEnvelope.getMaxX(), DELTA);
    assertEquals(10.0044966009, fromEnvelope.getMaxY(), DELTA);

    var toEnvelope = subject.toEnvelope();
    assertEquals(20.9952146804, toEnvelope.getMinX(), DELTA);
    assertEquals(19.9955033990, toEnvelope.getMinY(), DELTA);
    assertEquals(21.0047853195, toEnvelope.getMaxX(), DELTA);
    assertEquals(20.0044966009, toEnvelope.getMaxY(), DELTA);
  }

  @Test
  void mapFromToStopIds() {
    var builder = builder();

    var from = GenericLocation.fromStopId("S1", "A", "STOP1");
    var to = GenericLocation.fromStopId("S2", "A", "STOP2");

    var req = builder.withDateTime(INSTANT).withFrom(from).withTo(to).buildRequest();

    var subject = StreetSearchRequestMapper.mapInternal(req).build();

    assertNull(subject.fromEnvelope());
    assertNull(subject.toEnvelope());
  }

  @Test
  void mapVehicleWalking() {
    var builder = builder();

    Instant dateTime = INSTANT;
    builder.withDateTime(dateTime);
    var from = new GenericLocation(null, id("STOP"), null, null);
    builder.withFrom(from);
    var to = GenericLocation.fromCoordinate(60.0, 20.0);
    builder.withTo(to);
    builder.withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)));

    builder.withJourney(jb ->
      jb
        .withWheelchair(true)
        .withModes(RequestModes.of().withAllStreetModes(StreetMode.BIKE).build())
    );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    assertEquals(dateTime, subject.startTime());
    assertTrue(subject.wheelchairEnabled());
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void mapTransferRequest(boolean arriveBy) {
    var from = new GenericLocation(null, id("STOP"), null, null);
    var to = GenericLocation.fromCoordinate(60.0, 20.0);
    var builder = builder()
      .withArriveBy(arriveBy)
      .withDateTime(INSTANT)
      .withFrom(from)
      .withTo(to)
      .withPreferences(it -> it.withWalk(walk -> walk.withSpeed(2.4)))
      .withJourney(j -> j.withWheelchair(true));

    var request = builder.buildRequest();

    var subject = StreetSearchRequestMapper.mapToTransferRequest(request).build();

    assertNull(subject.fromEnvelope());
    assertNull(subject.toEnvelope());
    assertTrue(subject.wheelchairEnabled());
    assertEquals(2.4, subject.walk().speed());
    assertEquals(Instant.EPOCH, subject.startTime());
    // arrive by must always be false for transfer requests
    assertFalse(subject.arriveBy());
  }

  @Test
  void mapWalkRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withWalk(walk ->
          walk
            .withSpeed(1.5)
            .withReluctance(2.5)
            .withStairsReluctance(3.5)
            .withStairsTimeFactor(4.5)
            .withBoardCost(100)
            .withSafetyFactor(0.8)
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var walkRequest = subject.walk();
    assertEquals(1.5, walkRequest.speed());
    assertEquals(2.5, walkRequest.reluctance());
    assertEquals(3.5, walkRequest.stairsReluctance());
    assertEquals(4.5, walkRequest.stairsTimeFactor());
    assertEquals(0.8, walkRequest.safetyFactor());
  }

  @Test
  void mapEscalator() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withWalk(w -> w.withEscalator(e -> e.withReluctance(99).withSpeed(88)))
      );

    var subject = StreetSearchRequestMapper.mapInternal(builder.buildRequest()).build();

    var req = subject.walk().escalator();
    assertEquals(99, req.reluctance());
    assertEquals(88, req.speed());
  }

  @Test
  void mapBikeRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withBike(bike ->
          bike
            .withSpeed(5.0)
            .withReluctance(1.5)
            .withBoardCost(200)
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(0.8).withSafety(0.1).withSlope(0.1))
            .withWalking(walking ->
              walking
                .withSpeed(1.2)
                .withReluctance(2.0)
                .withStairsReluctance(5.0)
                .withMountDismountTime(Duration.ofSeconds(30))
                .withMountDismountCost(77)
            )
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var bikeRequest = subject.bike();
    assertEquals(5.0, bikeRequest.speed());
    assertEquals(1.5, bikeRequest.reluctance());
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, bikeRequest.optimizeType());

    var walking = bikeRequest.walking();
    assertEquals(1.2, walking.speed());
    assertEquals(2.0, walking.reluctance());
    assertEquals(5.0, walking.stairsReluctance());
    assertEquals(Duration.ofSeconds(30), walking.mountDismountTime());
    assertEquals(costOfSeconds(77), walking.mountDismountCost());
    assertEquals(0.1, bikeRequest.optimizeTriangle().slope());
    assertEquals(0.1, bikeRequest.optimizeTriangle().safety());
    assertEquals(0.8, bikeRequest.optimizeTriangle().time());
  }

  @Test
  void bikeTriangle() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withBike(bike ->
          bike
            .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
            .withOptimizeTriangle(it -> it.withTime(1).withSafety(2).withSlope(3))
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var bikeRequest = subject.bike();
    assertEquals(VehicleRoutingOptimizeType.TRIANGLE, bikeRequest.optimizeType());
    assertEquals(0.5, bikeRequest.optimizeTriangle().slope());
    assertEquals(0.33, bikeRequest.optimizeTriangle().safety());
    assertEquals(0.17, bikeRequest.optimizeTriangle().time());
  }

  @Test
  void mapCarRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withCar(car ->
          car
            .withReluctance(1.8)
            .withPickupTime(Duration.ofMinutes(5))
            .withPickupCost(10)
            .withAccelerationSpeed(2.5)
            .withDecelerationSpeed(3.0)
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var carRequest = subject.car();
    assertEquals(1.8, carRequest.reluctance());
    assertEquals(Duration.ofMinutes(5), carRequest.pickupTime());
    assertEquals(Cost.costOfSeconds(10), carRequest.pickupCost());
    assertEquals(2.5, carRequest.accelerationSpeed());
    assertEquals(3.0, carRequest.decelerationSpeed());
  }

  @Test
  void mapScooterRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withScooter(scooter ->
          scooter
            .withSpeed(4.5)
            .withReluctance(2.0)
            .withOptimizeType(VehicleRoutingOptimizeType.SAFE_STREETS)
            .withOptimizeTriangle(b -> b.withSafety(0.2).withSlope(0.2).withTime(0.6))
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var scooterRequest = subject.scooter();
    assertEquals(4.5, scooterRequest.speed());
    assertEquals(2.0, scooterRequest.reluctance());
    assertEquals(VehicleRoutingOptimizeType.SAFE_STREETS, scooterRequest.optimizeType());
    assertEquals(0.6, scooterRequest.optimizeTriangle().time());
    assertEquals(0.2, scooterRequest.optimizeTriangle().slope());
    assertEquals(0.2, scooterRequest.optimizeTriangle().safety());
  }

  @Test
  void mapRentalRequest() {
    var request = builder()
      .withPreferences(pref ->
        pref.withBike(bike ->
          bike.withRental(rental ->
            rental
              .withPickupTime(Duration.ofSeconds(120))
              .withPickupCost(180)
              .withDropOffTime(Duration.ofSeconds(90))
              .withDropOffCost(150)
              .withUseAvailabilityInformation(true)
              .withAllowArrivingInRentedVehicleAtDestination(false)
              .withArrivingInRentalVehicleAtDestinationCost(30)
          )
        )
      )
      .buildRequest();

    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var rentalRequest = subject.bike().rental();
    assertEquals(Duration.ofSeconds(120), rentalRequest.pickupTime());
    assertEquals(costOfSeconds(180), rentalRequest.pickupCost());
    assertEquals(Duration.ofSeconds(90), rentalRequest.dropOffTime());
    assertEquals(costOfSeconds(150), rentalRequest.dropOffCost());
    assertTrue(rentalRequest.useAvailabilityInformation());
    assertFalse(rentalRequest.allowArrivingInRentedVehicleAtDestination());
    assertEquals(costOfSeconds(30), rentalRequest.arrivingInRentalVehicleAtDestinationCost());
  }

  @Test
  void mapCarRentalDepartureRequest() {
    var builder = builder();

    Instant dateTime = Instant.parse("2022-11-10T10:00:00Z");
    var rentalDuration = Duration.ofHours(2);
    builder.withDateTime(dateTime);
    builder.withJourney(jb ->
      jb
        .withModes(RequestModes.of().withAllStreetModes(StreetMode.BIKE).build())
        .withDirect(new StreetRequest(StreetMode.CAR_RENTAL, rentalDuration))
    );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    assertEquals(dateTime, subject.startTime());
    assertEquals(dateTime, subject.rentalPeriod().start());
    assertEquals(dateTime.plus(rentalDuration), subject.rentalPeriod().end());
  }

  /**
   * test properties, which may differ on arrival route requests
   */
  @Test
  void mapCarRentalArrivalRequest() {
    var builder = builder().withArriveBy(true);

    var dateTime = Instant.parse("2022-11-10T10:00:00Z");
    var rentalDuration = Duration.ofHours(2);
    builder.withDateTime(dateTime);
    var from = new GenericLocation(null, TimetableRepositoryForTest.id("STOP"), null, null);
    builder.withFrom(from);
    var to = GenericLocation.fromCoordinate(60.0, 20.0);
    builder.withTo(to);
    builder.withJourney(jb ->
      jb.withDirect(new StreetRequest(StreetMode.CAR_RENTAL, rentalDuration))
    );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    assertEquals(dateTime, subject.startTime());
    assertEquals(dateTime.minus(rentalDuration), subject.rentalPeriod().start());
    assertEquals(dateTime, subject.rentalPeriod().end());
  }

  @Test
  void mapParkingRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withCar(car ->
          car.withParking(parking ->
            parking
              .withCost(15)
              .withTime(Duration.ofMinutes(5))
              .withUnpreferredVehicleParkingTagCost(20)
              .withPreferredVehicleParkingTags(Set.of("A"))
          )
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var parkingRequest = subject.car().parking();
    assertEquals(costOfSeconds(15), parkingRequest.cost());
    assertEquals(Duration.ofMinutes(5), parkingRequest.time());
    assertEquals(costOfSeconds(20), parkingRequest.unpreferredVehicleParkingTagCost());
    assertNotNull(parkingRequest.filter());
  }

  @Test
  void mapArriveByAndFromTo() {
    var from = GenericLocation.fromCoordinate(59.0, 10.0);
    var to = GenericLocation.fromCoordinate(60.0, 11.0);

    var request = builder().withFrom(from).withTo(to).withArriveBy(true).buildRequest();

    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    assertTrue(subject.arriveBy());
  }

  @Test
  void mapAccessibilityRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withWheelchair(wheelchair -> {
          wheelchair
            .withMaxSlope(0.9)
            .withInaccessibleStreetReluctance(3)
            .withStairsReluctance(52.0)
            .withElevator(e -> e.withAccessibleOnly())
            .withStop(b -> b.withUnknownCost(100).withInaccessibleCost(200));
        })
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var req = subject.wheelchair();
    assertEquals(0.9, req.maxSlope());
    assertEquals(3, req.inaccessibleStreetReluctance());
    assertEquals(52.0, req.stairsReluctance());
    assertTrue(req.elevator().onlyConsiderAccessible());
    assertEquals(100, req.stop().unknownCost());
    assertEquals(200, req.stop().inaccessibleCost());
    assertFalse(req.stop().onlyConsiderAccessible());
  }

  @Test
  void mapElevator() {
    var builder = builder()
      .withPreferences(pref ->
        pref.withStreet(s ->
          s.withElevator(e ->
            e
              .withBoardSlack(Duration.ofSeconds(88))
              .withBoardCost(77)
              .withHopTime(Duration.ofSeconds(66))
              .withReluctance(2.0)
          )
        )
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    var req = subject.elevator();
    assertEquals(88, req.boardSlack().toSeconds());
    assertEquals(77, req.boardCost());
    assertEquals(66, req.hopTime().toSeconds());
    assertEquals(2.0, req.reluctance());
  }

  @Test
  void mapSystemRequest() {
    var builder = builder()
      .withPreferences(pref ->
        pref
          .withSystem(system -> system.withGeoidElevation(true))
          .withStreet(street -> street.withTurnReluctance(3.5))
      );

    var request = builder.buildRequest();
    var subject = StreetSearchRequestMapper.mapInternal(request).build();

    assertTrue(subject.geoidElevation());
    assertEquals(3.5, subject.turnReluctance());
  }

  private static RouteRequestBuilder builder() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(1, 1));
  }
}

package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;

class TripScheduleIndexResolverTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);
  private static final ZoneId TIME_ZONE = ZoneId.of("GMT");

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE,
    TIME_ZONE
  );

  private final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private final RegularStop STOP_B = ENV_BUILDER.stop("B");
  private final RegularStop STOP_C = ENV_BUILDER.stop("C");

  @Test
  void resolvesSimpleOnBoardAccess() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripAndServiceDate = new TripAndServiceDate(env.tripData("T1").trip(), SERVICE_DATE);
    var location = new LocationInTripPatternReference(STOP_B.getIndex(), 1, 0);
    var result = new TripScheduleIndexResolver(env.raptorRoutingRequestTransitData()).resolve(
      tripAndServiceDate,
      location
    );

    var routingPattern = env.tripData("T1").scheduledTripPattern().getRoutingTripPattern();

    assertEquals(routingPattern.patternIndex(), result.routeIndex());
    assertEquals(0, result.tripScheduleIndex());
  }

  @Test
  void resolvesFirstStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripAndServiceDate = new TripAndServiceDate(env.tripData("T1").trip(), SERVICE_DATE);
    var location = new LocationInTripPatternReference(STOP_A.getIndex(), 0, 0);
    var result = new TripScheduleIndexResolver(env.raptorRoutingRequestTransitData()).resolve(
      tripAndServiceDate,
      location
    );

    var routingPattern = env.tripData("T1").scheduledTripPattern().getRoutingTripPattern();

    assertEquals(routingPattern.patternIndex(), result.routeIndex());
    assertEquals(0, result.tripScheduleIndex());
  }

  /**
   * When the stop index in the location does not appear in any pattern for the trip, the resolver
   * throws.
   */
  @Test
  void throwsWhenStopIndexDoesNotMatchTripPattern() {
    var stopA1 = ENV_BUILDER.stop("A1");
    var stopA2 = ENV_BUILDER.stop("A2");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(stopA2, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    // A1 is not visited by the trip
    var tripAndServiceDate = new TripAndServiceDate(env.tripData("T1").trip(), SERVICE_DATE);
    var location = new LocationInTripPatternReference(stopA1.getIndex(), 0, 0);
    assertThrows(InvalidRoutingInputException.class, () ->
      new TripScheduleIndexResolver(env.raptorRoutingRequestTransitData()).resolve(
        tripAndServiceDate,
        location
      )
    );
  }

  /**
   * Passing a stop position where boarding is not allowed should throw.
   */
  @Test
  void throwsWhenBoardingNotPossibleAtStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1")
        .addStop(STOP_A, "10:00")
        .addStop(STOP_B, "10:05", "10:05", PickDrop.NONE, PickDrop.SCHEDULED)
        .addStop(STOP_C, "10:10")
    ).build();

    var tripAndServiceDate = new TripAndServiceDate(env.tripData("T1").trip(), SERVICE_DATE);
    var patternSearch = env.raptorRoutingRequestTransitData();
    var location = new LocationInTripPatternReference(STOP_B.getIndex(), 1, 0);
    assertThrows(InvalidRoutingInputException.class, () ->
      new TripScheduleIndexResolver(patternSearch).resolve(tripAndServiceDate, location)
    );
  }

  /**
   * When there are multiple passes of the same stop, and only one of the passes disallows boarding,
   * we should only throw when the given stop position disallows boarding.
   */
  @Test
  void throwsOnlyWhenBoardingNotPossibleAtGivenStopPosition() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1")
        .addStop(STOP_A, "10:00")
        .addStop(STOP_B, "10:05", "10:05", PickDrop.NONE, PickDrop.SCHEDULED)
        .addStop(STOP_C, "10:10")
        .addStop(STOP_A, "10:15", "10:15")
        .addStop(STOP_B, "10:20", "10:20")
        .addStop(STOP_C, "10:25", "10:25")
    ).build();

    var tripAndServiceDate = new TripAndServiceDate(env.tripData("T1").trip(), SERVICE_DATE);
    var patternSearch = env.raptorRoutingRequestTransitData();

    // Position 1 does not allow boarding
    assertThrows(InvalidRoutingInputException.class, () ->
      new TripScheduleIndexResolver(patternSearch).resolve(
        tripAndServiceDate,
        new LocationInTripPatternReference(STOP_B.getIndex(), 1, 0)
      )
    );

    // Position 4 does allow boarding
    assertDoesNotThrow(() ->
      new TripScheduleIndexResolver(patternSearch).resolve(
        tripAndServiceDate,
        new LocationInTripPatternReference(STOP_B.getIndex(), 4, 0)
      )
    );
  }
}

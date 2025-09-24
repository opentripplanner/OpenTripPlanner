package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Tests that stops can be SKIPPED for a trip which repeats times for consecutive stops.
 *
 * @link <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/6848">issue</a>
 */
class AssignedStopIdsTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);
  private final RegularStop STOP_E = ENV_BUILDER.stop(STOP_E_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "10:00:00", "10:01:00")
    .addStop(STOP_B, "10:01:00", "10:01:00")
    .addStop(STOP_C, "10:01:00", "10:02:00")
    .build();

  @Test
  void assignedThenRevertedStopIds() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    assertFalse(env.getPatternForTrip(TRIP_1_ID).isCreatedByRealtimeUpdater());
    assertEquals(
      List.of("F:TestTrip1Pattern"),
      env
        .getTransitService()
        .getRealtimeRaptorTransitData()
        .getTripPatternsForRunningDate(SERVICE_DATE)
        .stream()
        .map(TripPatternForDate::getTripPattern)
        .map(RoutingTripPattern::getPattern)
        .map(AbstractTransitEntity::getId)
        .map(Object::toString)
        .toList()
    );

    var tripUpdate1 = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addAssignedStopTime(0, "09:50:00", STOP_D_ID)
      .addStopTime(1, "10:01:00")
      .addStopTime(2, "10:02:00")
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate1));
    assertEquals(
      "UPDATED | D 9:50 9:50 | B 10:01 10:01 | C 10:02 10:02",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
    assertTrue(env.getPatternForTrip(TRIP_1_ID).isCreatedByRealtimeUpdater());
    assertEquals(
      List.of("F:route-1::rt#1"),
      env
        .getTransitService()
        .getRealtimeRaptorTransitData()
        .getTripPatternsForRunningDate(SERVICE_DATE)
        .stream()
        .map(TripPatternForDate::getTripPattern)
        .map(RoutingTripPattern::getPattern)
        .map(AbstractTransitEntity::getId)
        .map(Object::toString)
        .toList()
    );

    var tripUpdate2 = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addAssignedStopTime(0, "09:55:00", STOP_E_ID)
      .addStopTime(1, "10:01:00")
      .addStopTime(2, "10:02:00")
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate2));
    assertEquals(
      "UPDATED | E 9:55 9:55 | B 10:01 10:01 | C 10:02 10:02",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
    assertTrue(env.getPatternForTrip(TRIP_1_ID).isCreatedByRealtimeUpdater());
    assertEquals(
      List.of("F:route-1::rt#2"),
      env
        .getTransitService()
        .getRealtimeRaptorTransitData()
        .getTripPatternsForRunningDate(SERVICE_DATE)
        .stream()
        .map(TripPatternForDate::getTripPattern)
        .map(RoutingTripPattern::getPattern)
        .map(AbstractTransitEntity::getId)
        .map(Object::toString)
        .toList()
    );

    var tripUpdate3 = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addAssignedStopTime(0, "10:01:00", STOP_A_ID)
      .addStopTime(1, "10:02:00")
      .addStopTime(2, "10:03:00")
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate3));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.getRealtimeTimetable(TRIP_1_ID)
    );

    assertFalse(env.getPatternForTrip(TRIP_1_ID).isCreatedByRealtimeUpdater());
    assertEquals(
      List.of("F:TestTrip1Pattern"),
      env
        .getTransitService()
        .getRealtimeRaptorTransitData()
        .getTripPatternsForRunningDate(SERVICE_DATE)
        .stream()
        .map(TripPatternForDate::getTripPattern)
        .map(RoutingTripPattern::getPattern)
        .map(AbstractTransitEntity::getId)
        .map(Object::toString)
        .toList()
    );
  }
}

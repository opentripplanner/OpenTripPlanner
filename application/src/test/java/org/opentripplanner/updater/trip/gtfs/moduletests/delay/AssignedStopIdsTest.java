package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Tests updating and reverting the stops/platforms for existing trips.
 */
class AssignedStopIdsTest implements RealtimeTestConstants {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);
  private static final LocalDate SERVICE_DATE_PLUS = SERVICE_DATE.plusDays(1);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE,
    TIME_ZONE
  );
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);
  private final RegularStop STOP_E = ENV_BUILDER.stop(STOP_E_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withServiceDates(SERVICE_DATE, SERVICE_DATE_PLUS)
    .addStop(STOP_A, "10:00:00", "10:00:00")
    .addStop(STOP_B, "10:01:00", "10:01:00")
    .addStop(STOP_C, "10:02:00", "10:02:00");

  private final TripInput TRIP_2_INPUT = TripInput.of(TRIP_2_ID)
    .withServiceDates(SERVICE_DATE, SERVICE_DATE_PLUS)
    .addStop(STOP_A, "11:00:00", "11:00:00")
    .addStop(STOP_B, "11:01:00", "11:01:00")
    .addStop(STOP_C, "11:02:00", "11:02:00");

  @Test
  void assignedThenRevertedStopIds() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));

    var rt = GtfsRtTestHelper.of(env);
    var tripUpdate1 = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addAssignedStopTime(0, "09:50:00", STOP_D_ID)
      .addStopTime(1, "10:01:00")
      .addStopTime(2, "10:02:00")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate1));
    assertEquals(
      "UPDATED | D 9:50 9:50 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env));

    var tripUpdate2 = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addAssignedStopTime(0, "09:55:00", STOP_E_ID)
      .addStopTime(1, "10:01:00")
      .addStopTime(2, "10:02:00")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate2));
    assertEquals(
      "UPDATED | E 9:55 9:55 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Route1::rt#2"), routingTripPatternIdsForDate(env));

    var tripUpdate3 = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addAssignedStopTime(0, "10:01:00", STOP_A_ID)
      .addStopTime(1, "10:02:00")
      .addStopTime(2, "10:03:00")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate3));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));
  }

  @Test
  @Disabled
  void reuseRealtimeTripPatterns() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(TRIP_2_INPUT).build();

    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));

    var rt = GtfsRtTestHelper.of(env);
    var tripUpdate1 = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addAssignedStopTime(0, "10:01", STOP_E_ID)
      .build();

    var tripUpdate2 = rt
      .tripUpdateScheduled(TRIP_2_ID)
      .addAssignedStopTime(0, "11:01", STOP_E_ID)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate1));
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 11:00 11:00 | B 11:01 11:01 | C 11:02 11:02",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1", "F:Route1::rt#1"), routingTripPatternIdsForDate(env));

    assertSuccess(rt.applyTripUpdates(List.of(tripUpdate1, tripUpdate2)));
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env));

    assertSuccess(rt.applyTripUpdate(tripUpdate2));
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1", "F:Route1::rt#1"), routingTripPatternIdsForDate(env));

    assertSuccess(
      rt.applyTripUpdates(
        List.of(
          rt.tripUpdateScheduled(TRIP_1_ID).addDelayedStopTime(0, 0).build(),
          rt.tripUpdateScheduled(TRIP_2_ID).addDelayedStopTime(0, 0).build()
        )
      )
    );
    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));
  }

  @Test
  @Disabled
  void reuseRealtimeTripPatternsOnDifferentServiceDates() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(TRIP_2_INPUT).build();

    assertFalse(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertFalse(env.tripData(TRIP_2_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    var rt = GtfsRtTestHelper.of(env);
    var tripUpdate11 = rt
      .tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE)
      .addAssignedStopTime(0, "10:01", STOP_E_ID)
      .build();
    var tripUpdate12 = rt
      .tripUpdateScheduled(TRIP_2_ID, SERVICE_DATE)
      .addAssignedStopTime(0, "11:01", STOP_E_ID)
      .build();

    var tripUpdate21 = rt
      .tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE_PLUS)
      .addAssignedStopTime(0, "10:01", STOP_E_ID)
      .build();
    var tripUpdate22 = rt
      .tripUpdateScheduled(TRIP_2_ID, SERVICE_DATE_PLUS)
      .addAssignedStopTime(0, "11:01", STOP_E_ID)
      .build();

    assertSuccess(rt.applyTripUpdates(List.of(tripUpdate11, tripUpdate12)));
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 11:00 11:00 | B 11:01 11:01 | C 11:02 11:02",
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(env.tripData(TRIP_2_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertFalse(
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    assertSuccess(
      rt.applyTripUpdates(List.of(tripUpdate11, tripUpdate12, tripUpdate21, tripUpdate22))
    );
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertTrue(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(env.tripData(TRIP_2_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertTrue(
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    assertSuccess(rt.applyTripUpdates(List.of(tripUpdate21, tripUpdate22)));
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 11:00 11:00 | B 11:01 11:01 | C 11:02 11:02",
      env.tripData(TRIP_2_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | E 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(
      "UPDATED | E 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertFalse(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertTrue(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertTrue(
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Route1::rt#1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    assertSuccess(
      rt.applyTripUpdates(
        List.of(
          rt.tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE).addDelayedStopTime(0, 0).build(),
          rt.tripUpdateScheduled(TRIP_2_ID, SERVICE_DATE).addDelayedStopTime(0, 0).build(),
          rt.tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE_PLUS).addDelayedStopTime(0, 0).build(),
          rt.tripUpdateScheduled(TRIP_2_ID, SERVICE_DATE_PLUS).addDelayedStopTime(0, 0).build()
        )
      )
    );
    assertFalse(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertFalse(
      env.tripData(TRIP_2_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));
  }

  @Test
  void reuseScheduledTripPatterns() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).addTrip(TRIP_2_INPUT).build();

    assertFalse(env.tripData(TRIP_1_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(env.tripData(TRIP_2_ID).tripPattern().isCreatedByRealtimeUpdater());
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));

    var rt = GtfsRtTestHelper.of(env);
    var tripUpdate1 = rt.tripUpdateScheduled(TRIP_1_ID).addDelayedStopTime(0, 60).build();

    var tripUpdate2 = rt.tripUpdateScheduled(TRIP_2_ID).addDelayedStopTime(0, 60).build();

    assertSuccess(rt.applyTripUpdate(tripUpdate1));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 11:00 11:00 | B 11:01 11:01 | C 11:02 11:02",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));

    assertSuccess(rt.applyTripUpdates(List.of(tripUpdate1, tripUpdate2)));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "UPDATED | A 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));

    assertSuccess(rt.applyTripUpdate(tripUpdate2));
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertEquals(
      "UPDATED | A 11:01 11:01 | B 11:02 11:02 | C 11:03 11:03",
      env.tripData(TRIP_2_ID).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env));
  }

  @Test
  void reuseScheduledTripPatternsOnDifferentServiceDates() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    assertFalse(env.tripData(TRIP_1_ID, SERVICE_DATE).tripPattern().isCreatedByRealtimeUpdater());
    assertFalse(
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).tripPattern().isCreatedByRealtimeUpdater()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    var rt = GtfsRtTestHelper.of(env);
    var tripUpdate1 = rt
      .tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE)
      .addDelayedStopTime(0, 60)
      .build();

    var tripUpdate2 = rt
      .tripUpdateScheduled(TRIP_1_ID, SERVICE_DATE_PLUS)
      .addDelayedStopTime(0, 60)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate1));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    assertSuccess(rt.applyTripUpdates(List.of(tripUpdate1, tripUpdate2)));
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));

    assertSuccess(rt.applyTripUpdate(tripUpdate2));
    assertEquals(
      "SCHEDULED | A 10:00 10:00 | B 10:01 10:01 | C 10:02 10:02",
      env.tripData(TRIP_1_ID, SERVICE_DATE).showTimetable()
    );
    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:02 10:02 | C 10:03 10:03",
      env.tripData(TRIP_1_ID, SERVICE_DATE_PLUS).showTimetable()
    );
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE));
    assertEquals(List.of("F:Pattern1"), routingTripPatternIdsForDate(env, SERVICE_DATE_PLUS));
  }

  private List<String> routingTripPatternIdsForDate(TransitTestEnvironment env) {
    return routingTripPatternIdsForDate(env, env.defaultServiceDate());
  }

  private List<String> routingTripPatternIdsForDate(
    TransitTestEnvironment env,
    LocalDate serviceDate
  ) {
    return tripPatternsForDate(env, serviceDate)
      .stream()
      .map(TripPatternForDate::getTripPattern)
      .map(RoutingTripPattern::getPattern)
      .map(AbstractTransitEntity::getId)
      .map(Object::toString)
      .sorted()
      .toList();
  }

  private Collection<TripPatternForDate> tripPatternsForDate(
    TransitTestEnvironment env,
    LocalDate serviceDate
  ) {
    return env
      .transitService()
      .getRealtimeRaptorTransitData()
      .getTripPatternsForRunningDate(serviceDate);
  }
}

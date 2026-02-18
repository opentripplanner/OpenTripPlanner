package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.router.OnBoardAccessResolver;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.utils.time.ServiceDateUtils;

class OnBoardAccessResolverTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);
  private static final ZoneId TIME_ZONE = ZoneId.of("GMT");

  private static final String STOP_A_ID = "A";
  private static final String STOP_B_ID = "B";
  private static final String STOP_C_ID = "C";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE,
    TIME_ZONE
  );
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private static Instant toInstant(int secondsSinceStartOfService) {
    return ServiceDateUtils.asStartOfService(SERVICE_DATE, TIME_ZONE)
      .plusSeconds(secondsSinceStartOfService)
      .toInstant();
  }

  @Test
  void resolveSimpleOnBoardAccess() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    var result = resolver.resolve(tripLocation, patternSearch);

    var tripData = env.tripData("T1");
    var routingPattern = tripData.scheduledTripPattern().getRoutingTripPattern();

    assertEquals(routingPattern.patternIndex(), result.routeIndex());
    assertEquals(0, result.tripScheduleIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(routingPattern.stopIndex(1), result.stop());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  @Test
  void resolveFirstStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_A.getId()
    );

    var result = resolver.resolve(tripLocation, env.raptorRequestData().onBoardTripPatternSearch());

    var routingPattern = env.tripData("T1").scheduledTripPattern().getRoutingTripPattern();

    assertEquals(0, result.stopPositionInPattern());
    assertEquals(routingPattern.stopIndex(0), result.stop());
    assertEquals(10 * 3600, result.boardingTime());
  }

  @Test
  void throwsOnLastStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_C.getId()
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void throwsOnLastStopWithAimedDepartureTime() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_C.getId(),
      toInstant(10 * 3600 + 10 * 60)
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void throwsOnUnknownTrip() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("unknown"), SERVICE_DATE),
      STOP_A.getId()
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void throwsOnUnknownStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      id("unknown-stop")
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void resolveOnBoardAccessWithZeroCost() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    var result = resolver.resolve(tripLocation, env.raptorRequestData().onBoardTripPatternSearch());
    assertEquals(0, result.c1());
  }

  @Test
  void resolveWithAimedDepartureTimeOnUniqueStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId(),
      toInstant(10 * 3600 + 5 * 60)
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    var result = resolver.resolve(tripLocation, patternSearch);

    var routingPattern = env.tripData("T1").scheduledTripPattern().getRoutingTripPattern();

    assertEquals(routingPattern.patternIndex(), result.routeIndex());
    assertEquals(0, result.tripScheduleIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(routingPattern.stopIndex(1), result.stop());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  @Test
  void throwsOnWrongAimedDepartureTimeOnUniqueStop() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    // STOP_B departs at 10:05, but we provide 10:00 — should fail
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId(),
      toInstant(10 * 3600)
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void resolveWithScheduledRaptorData() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    // Use ignoreRealtimeUpdates=true, mirroring the production flag in TransitRouter
    var patternSearch = env.raptorRequestData(true).onBoardTripPatternSearch();
    var result = resolver.resolve(tripLocation, patternSearch);

    var routingPattern = env.tripData("T1").scheduledTripPattern().getRoutingTripPattern();

    assertEquals(routingPattern.patternIndex(), result.routeIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(routingPattern.stopIndex(1), result.stop());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  @Test
  void throwsOnRingLineWithStopId() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_A, "10:15")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_A.getId()
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    assertThrows(RoutingValidationException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  @Test
  void resolveRingLineWithScheduledDepartureTime() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1")
        .addStop(STOP_A, "10:00")
        .addStop(STOP_B, "10:05")
        .addStop(STOP_A, "10:15")
        .addStop(STOP_C, "10:20")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripRef = TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE);
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // First occurrence of STOP_A at 10:00
    var firstOccurrence = TripLocation.of(tripRef, STOP_A.getId(), toInstant(10 * 3600));
    var result1 = resolver.resolve(firstOccurrence, patternSearch);
    assertEquals(0, result1.stopPositionInPattern());
    assertEquals(10 * 3600, result1.boardingTime());

    // Second occurrence of STOP_A at 10:15
    var secondOccurrence = TripLocation.of(tripRef, STOP_A.getId(), toInstant(10 * 3600 + 15 * 60));
    var result2 = resolver.resolve(secondOccurrence, patternSearch);
    assertEquals(2, result2.stopPositionInPattern());
    assertEquals(10 * 3600 + 15 * 60, result2.boardingTime());
  }

  /**
   * When a realtime-modified pattern is not in the Raptor pattern index,
   * findPatternInRaptorData falls back to the base/static pattern.
   */
  @Test
  void resolveFallsBackToBasePatternWhenRealtimePatternNotInIndex() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripData = env.tripData("T1");
    var scheduledPattern = tripData.scheduledTripPattern();
    var tripTimes = tripData.scheduledTripTimes();

    // Build pattern search BEFORE applying the realtime update — so it only has scheduled patterns
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // Realtime-modified pattern (different route index, not in Raptor data)
    var realtimePattern = TripPattern.of(id("P1-rt"))
      .withRoute(scheduledPattern.getRoute())
      .withStopPattern(scheduledPattern.getStopPattern())
      .withRealTimeStopPatternModified()
      .build();

    // Apply realtime update that maps the trip to the new pattern
    env
      .timetableSnapshotManager()
      .updateBuffer(RealTimeTripUpdate.of(realtimePattern, tripTimes, SERVICE_DATE).build());
    env.timetableSnapshotManager().purgeAndCommit();

    // Transit service sees the realtime pattern, but patternSearch has only scheduled
    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    // findPattern(trip, SERVICE_DATE) returns realtimePattern (not in index),
    // falls back to findPattern(trip) which returns scheduledPattern (in index)
    var result = resolver.resolve(tripLocation, patternSearch);

    assertEquals(scheduledPattern.getRoutingTripPattern().patternIndex(), result.routeIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  /**
   * Verify that resolveBoardingDateTime works when a realtime updater has modified the trip's
   * stop pattern, moving it to a new TripPattern whose scheduled timetable is empty.
   */
  @Test
  void resolveBoardingDateTimeWithRealtimeModifiedPattern() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripData = env.tripData("T1");
    var scheduledPattern = tripData.scheduledTripPattern();
    var tripTimes = tripData.scheduledTripTimes();

    // Realtime-modified pattern (empty scheduled timetable)
    var realtimePattern = TripPattern.of(id("P1-rt"))
      .withRoute(scheduledPattern.getRoute())
      .withStopPattern(scheduledPattern.getStopPattern())
      .withRealTimeStopPatternModified()
      .build();

    // Apply realtime update
    env
      .timetableSnapshotManager()
      .updateBuffer(RealTimeTripUpdate.of(realtimePattern, tripTimes, SERVICE_DATE).build());
    env.timetableSnapshotManager().purgeAndCommit();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    var result = resolver.resolveBoardingDateTime(tripLocation, TIME_ZONE);

    long expectedEpochSecond =
      SERVICE_DATE.atStartOfDay(TIME_ZONE).toEpochSecond() + 10 * 3600 + 5 * 60;
    assertEquals(expectedEpochSecond, result.getEpochSecond());
  }

  /**
   * When a station ID is passed instead of a stop ID, the resolver should
   * find the child stop that the trip visits.
   */
  @Test
  void resolveByStationId() {
    var stopA = ENV_BUILDER.stopAtStation("SA1", "StationA");
    var stopB = ENV_BUILDER.stopAtStation("SB1", "StationB");
    var stopC = ENV_BUILDER.stopAtStation("SC1", "StationC");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(stopA, "10:00").addStop(stopB, "10:05").addStop(stopC, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // Pass the station ID — should resolve to the child stop's position
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      id("StationB")
    );

    var result = resolver.resolve(tripLocation, patternSearch);
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  /**
   * Station with multiple child stops where the trip visits only one. Passing the station ID
   * should find the visited stop regardless of child stop iteration order.
   */
  @Test
  void resolveByStationIdWithMultipleChildStops() {
    ENV_BUILDER.stopAtStation("SA1", "StationA");
    var stopA2 = ENV_BUILDER.stopAtStation("SA2", "StationA");
    var stopB = ENV_BUILDER.stopAtStation("SB1", "StationB");
    var stopC = ENV_BUILDER.stopAtStation("SC1", "StationC");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(stopA2, "10:00").addStop(stopB, "10:05").addStop(stopC, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // Pass station ID — should find SA2 at position 1 (even though SA1 also belongs to StationA)
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      id("StationA")
    );

    var result = resolver.resolve(tripLocation, patternSearch);
    assertEquals(0, result.stopPositionInPattern());
    assertEquals(10 * 3600, result.boardingTime());
  }

  /**
   * Station with multiple child stops where the trip visits only one. Passing the stop ID for the
   * wrong stop means we throw
   */
  @Test
  void resolveByStationIdWithMultipleChildStopsThrowsWhenWrongStopPassed() {
    var stopA1 = ENV_BUILDER.stopAtStation("SA1", "StationA");
    var stopA2 = ENV_BUILDER.stopAtStation("SA2", "StationA");
    var stopB = ENV_BUILDER.stopAtStation("SB1", "StationB");
    var stopC = ENV_BUILDER.stopAtStation("SC1", "StationC");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(stopA2, "10:00").addStop(stopB, "10:05").addStop(stopC, "10:10")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // A2 is the stop visited, but here we pass A1
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      stopA1.getId()
    );

    // Should throw since A1 is not visited
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }

  /**
   * Station with multiple child stops on a ring line — pattern visits SA1 then SA2 (both children
   * of the same station). Passing the station ID without a departure time should throw because
   * it is ambiguous. Passing with a departure time should disambiguate.
   */
  @Test
  void resolveStationOnRingLineThrowsWithoutDepartureTime() {
    var stopA1 = ENV_BUILDER.stopAtStation("SA1", "StationA");
    var stopA2 = ENV_BUILDER.stopAtStation("SA2", "StationA");
    var stopB = ENV_BUILDER.stopAtStation("SB1", "StationB");
    var stopC = ENV_BUILDER.stopAtStation("SC1", "StationC");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1")
        .addStop(stopA1, "10:00")
        .addStop(stopB, "10:05")
        .addStop(stopA2, "10:15")
        .addStop(stopC, "10:20")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    var tripRef = TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE);

    // Without departure time — ambiguous, should throw
    var ambiguous = TripLocation.of(tripRef, id("StationA"));
    assertThrows(RoutingValidationException.class, () ->
      resolver.resolve(ambiguous, patternSearch)
    );

    // With departure time for SA1 at 10:00 — should find position 0
    var withTimeFirst = TripLocation.of(tripRef, id("StationA"), toInstant(10 * 3600));
    var result1 = resolver.resolve(withTimeFirst, patternSearch);
    assertEquals(0, result1.stopPositionInPattern());
    assertEquals(10 * 3600, result1.boardingTime());

    // With departure time for SA2 at 10:15 — should find position 2
    var withTime = TripLocation.of(tripRef, id("StationA"), toInstant(10 * 3600 + 15 * 60));
    var result2 = resolver.resolve(withTime, patternSearch);
    assertEquals(2, result2.stopPositionInPattern());
    assertEquals(10 * 3600 + 15 * 60, result2.boardingTime());
  }

  /**
   * When a TripPattern is copied (as happens during graph build in TransitDataImportBuilder),
   * the copy gets a new RoutingTripPattern with a different patternIndex. However, the
   * scheduledTimetable may still reference the original pattern. The Raptor data is built from
   * scheduledTimetable.getPattern().getRoutingTripPattern() (the original), while
   * TransitService.findPattern(trip) returns the copy.
   *
   * This test verifies that findPatternInRaptorData falls back to the scheduledTimetable's
   * pattern when the copy's RoutingTripPattern is not in the Raptor index.
   */
  @Test
  void resolveFallsBackToScheduledTimetablePatternWhenCopiedPatternNotInIndex() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripData = env.tripData("T1");
    var originalPattern = tripData.scheduledTripPattern();

    // Build Raptor data BEFORE replacing the pattern — so it indexes the original's
    // RoutingTripPattern
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // Simulate what TransitDataImportBuilder does: copy the pattern while reusing the
    // existing scheduledTimetable. The copy gets a new RoutingTripPattern (different
    // patternIndex), but scheduledTimetable.getPattern() still points to the original.
    // We must change an unrelated field (name) to prevent AbstractBuilder.build() from
    // returning the original due to sameAs() equality.
    var copiedPattern = originalPattern
      .copy()
      .withName("copied")
      .withScheduledTimeTable(originalPattern.getScheduledTimetable())
      .build();

    // Replace the pattern in the repository — findPattern(trip) will now return the copy
    env.timetableRepository().addTripPattern(copiedPattern.getId(), copiedPattern);
    env.timetableRepository().index();

    // Verify our setup: the copy has a different RoutingTripPattern index
    assertNotEquals(
      originalPattern.getRoutingTripPattern().patternIndex(),
      copiedPattern.getRoutingTripPattern().patternIndex()
    );
    // And the scheduledTimetable still references the original
    assertSame(originalPattern, copiedPattern.getScheduledTimetable().getPattern());

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    // Should succeed by falling back to scheduledTimetable.getPattern() (the original)
    var result = resolver.resolve(tripLocation, patternSearch);

    assertEquals(originalPattern.getRoutingTripPattern().patternIndex(), result.routeIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  /**
   * Same as the copied-pattern test above, but for resolveBoardingDateTime which doesn't
   * need the Raptor pattern index. This verifies the method works even when the pattern in
   * the TransitService is a copy with a different RoutingTripPattern.
   */
  @Test
  void resolveBoardingDateTimeWithCopiedPattern() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripData = env.tripData("T1");
    var originalPattern = tripData.scheduledTripPattern();

    // Copy pattern with reused scheduledTimetable (same as TransitDataImportBuilder)
    var copiedPattern = originalPattern
      .copy()
      .withName("copied")
      .withScheduledTimeTable(originalPattern.getScheduledTimetable())
      .build();
    env.timetableRepository().addTripPattern(copiedPattern.getId(), copiedPattern);
    env.timetableRepository().index();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    var result = resolver.resolveBoardingDateTime(tripLocation, TIME_ZONE);

    long expectedEpochSecond =
      SERVICE_DATE.atStartOfDay(TIME_ZONE).toEpochSecond() + 10 * 3600 + 5 * 60;
    assertEquals(expectedEpochSecond, result.getEpochSecond());
  }

  /**
   * Combines both failure modes: a realtime update creates a new pattern not in the index,
   * AND the base pattern was copied (so its RoutingTripPattern also differs from what's in
   * the index). The resolver must:
   * 1. Try findPattern(trip, date) → realtime pattern (not in index, scheduledTimetable
   *    points to itself → still not in index)
   * 2. Fall back to findPattern(trip) → copied base pattern (not in index)
   * 3. Fall back to copiedBase.scheduledTimetable.getPattern() → original (in index)
   */
  @Test
  void resolveFallsBackThroughRealtimeAndCopiedPattern() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05").addStop(STOP_C, "10:10")
    ).build();

    var tripData = env.tripData("T1");
    var originalPattern = tripData.scheduledTripPattern();
    var tripTimes = tripData.scheduledTripTimes();

    // Build Raptor data with the original pattern
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // Step 1: Copy the pattern (simulating graph build), reusing scheduledTimetable
    var copiedPattern = originalPattern
      .copy()
      .withName("copied")
      .withScheduledTimeTable(originalPattern.getScheduledTimetable())
      .build();
    env.timetableRepository().addTripPattern(copiedPattern.getId(), copiedPattern);
    env.timetableRepository().index();

    // Step 2: Apply a realtime update that moves the trip to a new pattern
    var realtimePattern = TripPattern.of(id("P1-rt"))
      .withRoute(originalPattern.getRoute())
      .withStopPattern(originalPattern.getStopPattern())
      .withRealTimeStopPatternModified()
      .build();
    env
      .timetableSnapshotManager()
      .updateBuffer(RealTimeTripUpdate.of(realtimePattern, tripTimes, SERVICE_DATE).build());
    env.timetableSnapshotManager().purgeAndCommit();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      STOP_B.getId()
    );

    // findPattern(trip, date) → realtimePattern (not in index)
    // findPattern(trip) → copiedPattern (not in index)
    // copiedPattern.scheduledTimetable.getPattern() → originalPattern (in index!)
    var result = resolver.resolve(tripLocation, patternSearch);

    assertEquals(originalPattern.getRoutingTripPattern().patternIndex(), result.routeIndex());
    assertEquals(1, result.stopPositionInPattern());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  /**
   * When the clocks move forward in spring due to DST, midnight and noon-minus-12h
   * (start-of-service) differ by one hour. The aimed departure time conversion uses
   * start-of-service (noon-minus-12h) as the reference, not midnight, because TripTimes are
   * relative to start-of-service.
   */
  @Test
  void resolveWithAimedDepartureTimeOnDstSpringForwardDay() {
    // Europe/Oslo moves clocks forward on 2024-03-31: clocks skip from 02:00 to 03:00
    var dstDate = LocalDate.of(2024, 3, 31);
    var dstZone = ZoneId.of("Europe/Oslo");
    var dstEnvBuilder = TransitTestEnvironment.of(dstDate, dstZone);
    var stopA = dstEnvBuilder.stop("A");
    var stopB = dstEnvBuilder.stop("B");
    var stopC = dstEnvBuilder.stop("C");

    var env = dstEnvBuilder
      .addTrip(
        TripInput.of("T1").addStop(stopA, "10:00").addStop(stopB, "10:05").addStop(stopC, "10:10")
      )
      .build();

    var resolver = new OnBoardAccessResolver(env.transitService());

    // Compute the aimed departure instant using start-of-service (noon-minus-12h),
    // which is what a correct client would send
    var aimedDeparture = ServiceDateUtils.asStartOfService(dstDate, dstZone)
      .plusSeconds(10 * 3600 + 5 * 60)
      .toInstant();

    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), dstDate),
      stopB.getId(),
      aimedDeparture
    );

    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();
    var result = resolver.resolve(tripLocation, patternSearch);

    assertEquals(1, result.stopPositionInPattern());
    assertEquals(10 * 3600 + 5 * 60, result.boardingTime());
  }

  /**
   * Passing a station ID whose child stops are not visited by the trip should throw.
   */
  @Test
  void throwsOnWrongStationId() {
    var stopA = ENV_BUILDER.stopAtStation("SA1", "StationA");
    var stopB = ENV_BUILDER.stopAtStation("SB1", "StationB");
    ENV_BUILDER.stopAtStation("SC1", "StationC");
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(stopA, "10:00").addStop(stopB, "10:05")
    ).build();

    var resolver = new OnBoardAccessResolver(env.transitService());
    var patternSearch = env.raptorRequestData().onBoardTripPatternSearch();

    // StationC has child stop SC1 which is not in the trip's pattern
    var tripLocation = TripLocation.of(
      TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE),
      id("StationC")
    );
    assertThrows(IllegalArgumentException.class, () ->
      resolver.resolve(tripLocation, patternSearch)
    );
  }
}

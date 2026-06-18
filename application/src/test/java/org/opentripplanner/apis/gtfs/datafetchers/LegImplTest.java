package org.opentripplanner.apis.gtfs.datafetchers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.dataFetchingEnvironment;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.model.RealTimeTripStateModel;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model._data.TripTimesForTest;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;

class LegImplTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();
  private static final AreaStop AREA_STOP = MODEL.areaStop("a1").build();
  private static final RegularStop REGULAR_STOP = MODEL.stop("r1", 60.0, 10.0).build();
  private static final GroupStop GROUP_STOP = MODEL.groupStop("g1", REGULAR_STOP);
  private static final StopPattern STOP_PATTERN = TimetableRepositoryForTest.stopPattern(
    REGULAR_STOP,
    REGULAR_STOP,
    AREA_STOP,
    GROUP_STOP,
    REGULAR_STOP
  );
  private static final Trip TRIP = TimetableRepositoryForTest.trip("trip1").build();
  private static final TripPattern PATTERN = TimetableRepositoryForTest.tripPattern(
    "p",
    TRIP.getRoute()
  )
    .withStopPattern(STOP_PATTERN)
    .build();

  private static final ScheduledTripTimes TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("10:00 11:00 12:00 13:00 14:00")
    .withGtfsSequenceOfStopIndex(new int[] { 0, 1, 2, 3 })
    .withTrip(TRIP)
    .build();
  private static final ZonedDateTime TIME = ZonedDateTime.parse("2025-06-26T10:25:28+02:00");
  private static final ScheduledTransitLeg LEG = new ScheduledTransitLegBuilder<>()
    .withStartTime(TIME)
    .withEndTime(TIME)
    .withZoneId(TIME.getZone())
    .withServiceDate(TIME.toLocalDate())
    .withTripTimes(TRIP_TIMES)
    .withBoardStopIndexInPattern(0)
    .withAlightStopIndexInPattern(4)
    .withTripPattern(PATTERN)
    .build();
  private static final LegImpl IMPL = new LegImpl();
  private static final Map<String, Object> INCLUDE_STOP_ONLY = Map.of(
    "include",
    List.of(GraphQLTypes.GraphQLStopType.STOP)
  );
  private static final ZonedDateTime ANY_TIME = ZonedDateTime.of(
    2025,
    10,
    21,
    13,
    59,
    45,
    0,
    ZoneId.of("UTC")
  );
  private static final StreetLeg WALK_LEG = StreetLeg.of()
    .withStartTime(ANY_TIME)
    .withEndTime(ANY_TIME.plusMinutes(23))
    .withMode(TraverseMode.WALK)
    .build();

  @Test
  void intermediateStops() throws Exception {
    var env = dataFetchingEnvironment(LEG);
    var stops = IMPL.intermediateStops().get(env);
    assertThat(stops).containsExactly(REGULAR_STOP, AREA_STOP, GROUP_STOP);
  }

  @Test
  void intermediateStopsWithFilter() throws Exception {
    var env = dataFetchingEnvironment(LEG, INCLUDE_STOP_ONLY);
    var stops = IMPL.intermediateStops().get(env);
    assertThat(stops).containsExactly(REGULAR_STOP);
  }

  @Test
  void intermediatePlaces() throws Exception {
    var env = dataFetchingEnvironment(LEG);
    var stops = toStops(IMPL.intermediatePlaces().get(env));
    assertThat(stops).containsExactly(REGULAR_STOP, AREA_STOP, GROUP_STOP);
  }

  @Test
  void walkLeg() throws Exception {
    var env = dataFetchingEnvironment(WALK_LEG);
    assertNull(IMPL.intermediatePlaces().get(env));
  }

  @Test
  void walkLegWithFilter() throws Exception {
    var env = dataFetchingEnvironment(WALK_LEG, INCLUDE_STOP_ONLY);
    assertNull(IMPL.intermediateStops().get(env));
  }

  private static ScheduledTransitLeg legWithRealTimeTripTimes(RealTimeTripTimesBuilder builder) {
    return new ScheduledTransitLegBuilder<>()
      .withStartTime(TIME)
      .withEndTime(TIME)
      .withZoneId(TIME.getZone())
      .withServiceDate(TIME.toLocalDate())
      .withTripTimes(builder.build())
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(4)
      .withTripPattern(PATTERN)
      .build();
  }

  // ---------------------------------------------------------------------------
  // realTimeTripState
  // ---------------------------------------------------------------------------

  @Test
  void realTimeTripState_isNull_forNonTransitLeg() throws Exception {
    // Walk legs have no trip state
    var env = dataFetchingEnvironment(WALK_LEG);
    assertNull(IMPL.realTimeTripState().get(env));
  }

  @Test
  void realTimeTripState_allFlagsFalse_forScheduledTransitLeg() throws Exception {
    // LEG uses ScheduledTripTimes — all flags default to false
    var env = dataFetchingEnvironment(LEG);
    var state = IMPL.realTimeTripState().get(env);
    assertEquals(new RealTimeTripStateModel(false, false, false, false, false, false), state);
  }

  @Test
  void realTimeTripState_canceledFlagIsTrue_forCanceledTrip() throws Exception {
    var builder = TripTimesForTest.scheduled().createRealTimeFromScheduledTimes();
    builder.withCanceled();
    var leg = legWithRealTimeTripTimes(builder);
    var state = IMPL.realTimeTripState().get(dataFetchingEnvironment(leg));
    assertEquals(true, state.canceled());
    assertEquals(true, state.updated());
    assertEquals(false, state.added());
    assertEquals(false, state.deleted());
    assertEquals(false, state.timesModified());
    assertEquals(false, state.tripPatternModified());
  }

  @Test
  void realTimeTripState_timesModifiedAndUpdated_forDelayedTrip() throws Exception {
    var builder = TripTimesForTest.scheduled().createRealTimeFromScheduledTimes();
    builder.withRealTimeUpdated();
    var leg = legWithRealTimeTripTimes(builder);
    var state = IMPL.realTimeTripState().get(dataFetchingEnvironment(leg));
    assertEquals(true, state.timesModified());
    assertEquals(true, state.updated());
    assertEquals(false, state.canceled());
    assertEquals(false, state.added());
    assertEquals(false, state.deleted());
    assertEquals(false, state.tripPatternModified());
  }

  @Test
  void realTimeTripState_addedFlag_forAddedTrip() throws Exception {
    var builder = TripTimesForTest.scheduled().createRealTimeFromScheduledTimes();
    builder.withAdded();
    var leg = legWithRealTimeTripTimes(builder);
    var state = IMPL.realTimeTripState().get(dataFetchingEnvironment(leg));
    assertEquals(true, state.added());
    assertEquals(true, state.updated());
    assertEquals(false, state.canceled());
  }

  @Test
  void realTimeTripState_tripPatternModifiedFlag() throws Exception {
    var builder = TripTimesForTest.scheduled().createRealTimeFromScheduledTimes();
    builder.withModifiedTripPattern();
    var leg = legWithRealTimeTripTimes(builder);
    var state = IMPL.realTimeTripState().get(dataFetchingEnvironment(leg));
    assertEquals(true, state.tripPatternModified());
    assertEquals(true, state.updated());
    assertEquals(false, state.canceled());
  }

  private static Stream<StopLocation> toStops(Iterable<StopArrival> stopArrivals) {
    return StreamSupport.stream(stopArrivals.spliterator(), false).map(s -> s.place.stop);
  }
}

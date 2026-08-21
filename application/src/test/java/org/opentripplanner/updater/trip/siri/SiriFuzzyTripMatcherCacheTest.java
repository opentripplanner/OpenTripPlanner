package org.opentripplanner.updater.trip.siri;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.utils.time.TimeUtils;

class SiriFuzzyTripMatcherCacheTest implements RealtimeTestConstants {

  private static final String PLANNING_CODE = "47";
  private static final int LAST_STOP_ARRIVAL = TimeUtils.time("0:20:00");

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final Route RAIL_ROUTE = ENV_BUILDER.route("RailRoute", r ->
    r.withMode(TransitMode.RAIL)
  );

  @Test
  void railTripIsIndexedByInternalPlanningCode() {
    var env = ENV_BUILDER.addTrip(railTrip(TRIP_1_ID, PLANNING_CODE)).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(tripIds(cache.tripsByInternalPlanningCode(PLANNING_CODE))).containsExactly(
      TRIP_1_ID
    );
  }

  @Test
  void railTripsWithSamePlanningCodeAreGroupedTogether() {
    var env = ENV_BUILDER.addTrip(railTrip(TRIP_1_ID, PLANNING_CODE))
      .addTrip(railTrip(TRIP_2_ID, PLANNING_CODE))
      .build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(tripIds(cache.tripsByInternalPlanningCode(PLANNING_CODE))).containsExactly(
      TRIP_1_ID,
      TRIP_2_ID
    );
  }

  @Test
  void nonRailTripIsNotIndexedByInternalPlanningCode() {
    var busRoute = ENV_BUILDER.route("BusRoute", r -> r.withMode(TransitMode.BUS));
    var busTrip = tripInput(TRIP_1_ID)
      .withRoute(busRoute)
      .withNetexInternalPlanningCode(PLANNING_CODE);
    var env = ENV_BUILDER.addTrip(busTrip).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(cache.tripsByInternalPlanningCode(PLANNING_CODE)).isEmpty();
  }

  @Test
  void unknownPlanningCodeReturnsEmptySet() {
    var env = ENV_BUILDER.addTrip(railTrip(TRIP_1_ID, PLANNING_CODE)).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(cache.tripsByInternalPlanningCode("unknown")).isEmpty();
  }

  @Test
  void tripIsIndexedByLastStopAndScheduledArrivalTime() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL))).containsExactly(
      TRIP_1_ID
    );
  }

  @Test
  void lookupWithWrongStopOrArrivalTimeReturnsEmptySet() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(cache.tripsByLastStopArrival(STOP_A_ID, LAST_STOP_ARRIVAL)).isEmpty();
    assertThat(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL + 1)).isEmpty();
  }

  @Test
  void tripsWithSameLastStopAndArrivalAreGroupedTogether() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).addTrip(tripInput(TRIP_2_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(env.transitRepository());

    assertThat(tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL))).containsExactly(
      TRIP_1_ID,
      TRIP_2_ID
    );
  }

  private TripInput railTrip(String tripId, String internalPlanningCode) {
    return tripInput(tripId)
      .withRoute(RAIL_ROUTE)
      .withNetexInternalPlanningCode(internalPlanningCode);
  }

  private TripInput tripInput(String tripId) {
    return TripInput.of(tripId)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(STOP_B, "0:20:00", "0:20:00");
  }

  private static Set<String> tripIds(Set<Trip> trips) {
    return trips
      .stream()
      .map(trip -> trip.getId().getId())
      .collect(Collectors.toSet());
  }
}

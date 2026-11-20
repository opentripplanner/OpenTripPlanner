package org.opentripplanner.updater.trip.siri;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.utils.time.TimeUtils;

class SiriFuzzyTripMatcherCacheTest implements RealtimeTestConstants {

  private static final int LAST_STOP_ARRIVAL = TimeUtils.time("0:20:00");

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  @Test
  void tripIsIndexedByLastStopAndScheduledArrivalTime() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(
      new DefaultTransitService(
        env.transitRepository(),
        DefaultTimetableRepository.scheduledOnly(env.scheduledTimetableData())
      )
    );

    assertThat(
      tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL, LAST_STOP_ARRIVAL))
    ).containsExactly(TRIP_1_ID);
  }

  @Test
  void lookupWithWrongStopOrArrivalTimeReturnsEmptySet() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(
      new DefaultTransitService(
        env.transitRepository(),
        DefaultTimetableRepository.scheduledOnly(env.scheduledTimetableData())
      )
    );

    assertThat(
      cache.tripsByLastStopArrival(STOP_A_ID, LAST_STOP_ARRIVAL, LAST_STOP_ARRIVAL)
    ).isEmpty();
    assertThat(
      cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL + 1, LAST_STOP_ARRIVAL + 1)
    ).isEmpty();
  }

  @Test
  void tripsWithSameLastStopAndArrivalAreGroupedTogether() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).addTrip(tripInput(TRIP_2_ID)).build();

    var cache = SiriFuzzyTripMatcherCache.create(
      new DefaultTransitService(
        env.transitRepository(),
        DefaultTimetableRepository.scheduledOnly(env.scheduledTimetableData())
      )
    );

    assertThat(
      tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL, LAST_STOP_ARRIVAL))
    ).containsExactly(TRIP_1_ID, TRIP_2_ID);
  }

  @Test
  void lookupWithinTimeWindowIncludesBothLimits() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();
    var cache = createCache(env);

    assertThat(
      tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL - 59, LAST_STOP_ARRIVAL))
    ).containsExactly(TRIP_1_ID);
    assertThat(
      tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL, LAST_STOP_ARRIVAL + 59))
    ).containsExactly(TRIP_1_ID);
  }

  @Test
  void lookupOutsideTimeWindowReturnsEmptySet() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID)).build();
    var cache = createCache(env);

    assertThat(
      cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL - 60, LAST_STOP_ARRIVAL - 1)
    ).isEmpty();
    assertThat(
      cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL + 1, LAST_STOP_ARRIVAL + 60)
    ).isEmpty();
    assertThat(cache.tripsByLastStopArrival("unknown", 0, Integer.MAX_VALUE)).isEmpty();
  }

  @Test
  void lookupWithinTimeWindowReturnsTripsWithDifferentArrivalTimes() {
    var env = ENV_BUILDER.addTrip(tripInput(TRIP_1_ID))
      .addTrip(
        TripInput.of(TRIP_2_ID)
          .addStop(STOP_A, "0:10:00", "0:10:00")
          .addStop(STOP_B, "0:20:24", "0:20:24")
      )
      .build();
    var cache = createCache(env);

    assertThat(
      tripIds(cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL, LAST_STOP_ARRIVAL + 30))
    ).containsExactly(TRIP_1_ID, TRIP_2_ID);
    assertThat(
      tripIds(
        cache.tripsByLastStopArrival(STOP_B_ID, LAST_STOP_ARRIVAL + 1, LAST_STOP_ARRIVAL + 30)
      )
    ).containsExactly(TRIP_2_ID);
  }

  private static SiriFuzzyTripMatcherCache createCache(TransitTestEnvironment env) {
    return SiriFuzzyTripMatcherCache.create(
      new DefaultTransitService(
        env.transitRepository(),
        DefaultTimetableRepository.scheduledOnly(env.scheduledTimetableData())
      )
    );
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

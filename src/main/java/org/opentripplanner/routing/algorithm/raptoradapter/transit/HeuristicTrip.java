package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * A heuristic trip contains the lowest bound of hop and dwell times for a set of timetables on a
 * pattern. It is useful for computing the heuristic for the path pruning, as no schedule searches
 * need to be done.
 */
public final class HeuristicTrip implements DefaultTripSchedule {

  static final Deduplicator DEDUPLICATOR = new Deduplicator();

  private final int[] arrivalTimes;
  private final int[] departureTimes;
  private final DefaultTripPattern pattern;

  public static HeuristicTrip of(TripPatternForDate tripPatternForDate) {
    HeuristicTrip trip = new HeuristicTrip(
      tripPatternForDate.getTripPattern(),
      updater -> {
        for (TripTimes times : tripPatternForDate.tripTimes()) {
          updater.accept(times::getArrivalTime, times::getDepartureTime);
        }
        for (FrequencyEntry frequencyEntry : tripPatternForDate.getFrequencies()) {
          TripTimes times = frequencyEntry.tripTimes;
          updater.accept(times::getArrivalTime, times::getDepartureTime);
        }
      }
    );

    return DEDUPLICATOR.deduplicateObject(HeuristicTrip.class, trip);
  }

  public static HeuristicTrip of(TripPatternForDate[] tripPatternForDates) {
    HeuristicTrip trip = new HeuristicTrip(
      tripPatternForDates[0].getTripPattern(),
      updater -> {
        for (var tripPatternForDate : tripPatternForDates) {
          HeuristicTrip heuristicTrip = tripPatternForDate.heuristicTrip();
          updater.accept(heuristicTrip::arrival, heuristicTrip::departure);
        }
      }
    );

    return DEDUPLICATOR.deduplicateObject(HeuristicTrip.class, trip);
  }

  public static RaptorTripSchedule of(List<? extends DefaultTripSchedule> trips) {
    HeuristicTrip heuristicTrip = new HeuristicTrip(
      (DefaultTripPattern) trips.get(0).pattern(),
      updater -> {
        for (var trip : trips) {
          updater.accept(trip::arrival, trip::departure);
        }
      }
    );

    return DEDUPLICATOR.deduplicateObject(HeuristicTrip.class, heuristicTrip);
  }

  private HeuristicTrip(
    DefaultTripPattern pattern,
    Consumer<BiConsumer<IntUnaryOperator, IntUnaryOperator>> consumer
  ) {
    this.pattern = pattern;

    var arrivalTimes = IntUtils.intArray(pattern.numberOfStopsInPattern() - 1, Integer.MAX_VALUE);
    var departureTimes = IntUtils.intArray(pattern.numberOfStopsInPattern() - 1, Integer.MAX_VALUE);

    consumer.accept((IntUnaryOperator getArrivalTime, IntUnaryOperator getDepartureTime) ->
      updateTimes(arrivalTimes, departureTimes, getArrivalTime, getDepartureTime)
    );

    this.arrivalTimes = DEDUPLICATOR.deduplicateIntArray(arrivalTimes);
    this.departureTimes = DEDUPLICATOR.deduplicateIntArray(departureTimes);
  }

  @Override
  public int departure(int stopPosInPattern) {
    if (stopPosInPattern == 0) {
      return 0;
    }

    return departureTimes[stopPosInPattern - 1];
  }

  @Override
  public int arrival(int stopPosInPattern) {
    if (stopPosInPattern == 0) {
      return 0;
    }

    return arrivalTimes[stopPosInPattern - 1];
  }

  @Override
  public RaptorTripPattern pattern() {
    return pattern;
  }

  @Override
  public int tripSortIndex() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HeuristicTrip that)) {
      return false;
    }

    return (
      pattern.equals(that.pattern) &&
      Arrays.equals(arrivalTimes, that.arrivalTimes) &&
      Arrays.equals(departureTimes, that.departureTimes)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern, Arrays.hashCode(arrivalTimes), Arrays.hashCode(departureTimes));
  }

  @Override
  public int transitReluctanceFactorIndex() {
    return pattern.slackIndex();
  }

  @Override
  public Accessibility wheelchairBoarding() {
    return Accessibility.POSSIBLE;
  }

  private static void updateTimes(
    int[] arrivalTimes,
    int[] departureTimes,
    IntUnaryOperator getArrivalTime,
    IntUnaryOperator getDepartureTime
  ) {
    int departureTime = getDepartureTime.applyAsInt(0);
    int previous = 0;
    for (int i = 1; i <= arrivalTimes.length; i++) {
      int arrayIndex = i - 1;

      // Get arrival time and calculate hop time
      int arrivalTime = getArrivalTime.applyAsInt(i);
      int hopTime = arrivalTime - departureTime;

      // Update arrival time
      arrivalTimes[arrayIndex] = Math.min(arrivalTimes[arrayIndex], previous + hopTime);
      previous = arrivalTimes[arrayIndex];

      // Get departure time and calculate dwell time
      departureTime = getDepartureTime.applyAsInt(i);
      int dwellTime = departureTime - arrivalTime;

      // Update departure time
      departureTimes[arrayIndex] = Math.min(departureTimes[arrayIndex], previous + dwellTime);
      previous = departureTimes[arrayIndex];
    }
  }
}

package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate implements Comparable<TripPatternForDate> {

  private static final Logger LOG = LoggerFactory.getLogger(TripPatternForDate.class);

  /**
   * The original TripPattern whose TripSchedules were filtered to produce this.tripSchedules. Its
   * TripSchedules remain unchanged.
   */
  private final RoutingTripPattern tripPattern;

  /**
   * The filtered TripSchedules for only those trips in the TripPattern that are active on the given
   * day. Invariant: this array should contain a subset of the TripSchedules in
   * tripPattern.tripSchedules.
   */
  private final TripTimes[] tripTimes;

  /**
   * The filtered FrequencyEntries for only those entries in the TripPattern that are active on the
   * given day. Invariant: this array should contain a subset of the TripSchedules in
   * tripPattern.frequencyEntries.
   */
  private final FrequencyEntry[] frequencies;

  /** The service date of the trip pattern. */
  private final LocalDate serviceDate;

  /**
   * The running date on which the first trip departs. Not necessarily the same as the service date.
   */
  private final LocalDate startOfRunningPeriod;

  /**
   * The running date on which the last trip arrives.
   */
  private final LocalDate endOfRunningPeriod;

  public TripPatternForDate(
    RoutingTripPattern tripPattern,
    List<TripTimes> tripTimes,
    List<FrequencyEntry> frequencies,
    LocalDate serviceDate
  ) {
    this.tripPattern = tripPattern;
    this.tripTimes = tripTimes.toArray(new TripTimes[0]);
    this.frequencies = frequencies.toArray(new FrequencyEntry[0]);
    this.serviceDate = serviceDate;

    // TODO: We expect a pattern only containing trips or frequencies, fix ability to merge
    if (hasFrequencies()) {
      this.startOfRunningPeriod = ServiceDateUtils.asDateTime(
        serviceDate,
        frequencies
          .stream()
          .mapToInt(frequencyEntry -> frequencyEntry.startTime)
          .min()
          .orElseThrow()
      ).toLocalDate();

      this.endOfRunningPeriod = ServiceDateUtils.asDateTime(
        serviceDate,
        frequencies.stream().mapToInt(frequencyEntry -> frequencyEntry.endTime).max().orElseThrow()
      ).toLocalDate();
    } else {
      // These depend on the tripTimes array being sorted
      var first = tripTimes.get(0);
      this.startOfRunningPeriod = ServiceDateUtils.asDateTime(
        serviceDate,
        first.getDepartureTime(0)
      ).toLocalDate();
      var last = tripTimes.get(tripTimes.size() - 1);
      this.endOfRunningPeriod = ServiceDateUtils.asDateTime(
        serviceDate,
        last.getArrivalTime(last.getNumStops() - 1)
      ).toLocalDate();
      assertValidRunningPeriod(startOfRunningPeriod, endOfRunningPeriod, first, last);
    }
  }

  public List<TripTimes> tripTimes() {
    return Arrays.asList(tripTimes);
  }

  public List<FrequencyEntry> getFrequencies() {
    return Arrays.asList(frequencies);
  }

  public RoutingTripPattern getTripPattern() {
    return tripPattern;
  }

  public int stopIndex(int i) {
    return this.tripPattern.stopIndex(i);
  }

  public TripTimes getTripTimes(int i) {
    return tripTimes[i];
  }

  /**
   * The service date for which the trip pattern belongs to. Not necessarily the same as the start
   * of the running period in cases where the trip pattern only runs after midnight.
   */
  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public int numberOfTripSchedules() {
    return tripTimes.length;
  }

  /**
   * The start of the running period. This is determined by the first departure time for this
   * pattern. Not necessarily the same as the service date if the pattern runs after midnight.
   */
  public LocalDate getStartOfRunningPeriod() {
    return startOfRunningPeriod;
  }

  /**
   * Returns the running dates. A Trip "runs through" a date if any of its arrivals or departures is
   * happening on that date. The same trip pattern can therefore have multiple running dates and
   * trip pattern is not required to "run" on its service date.
   */
  public List<LocalDate> getRunningPeriodDates() {
    // Add one day to ensure last day is included
    return startOfRunningPeriod
      .datesUntil(endOfRunningPeriod.plusDays(1))
      .collect(Collectors.toList());
  }

  public boolean hasFrequencies() {
    return frequencies.length != 0;
  }

  @Override
  public int compareTo(TripPatternForDate other) {
    return serviceDate.compareTo(other.serviceDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      tripPattern,
      serviceDate,
      Arrays.hashCode(tripTimes),
      Arrays.hashCode(frequencies)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TripPatternForDate that = (TripPatternForDate) o;

    return (
      tripPattern.equals(that.tripPattern) &&
      serviceDate.equals(that.serviceDate) &&
      Arrays.equals(tripTimes, that.tripTimes) &&
      Arrays.equals(frequencies, that.frequencies)
    );
  }

  @Override
  public String toString() {
    return (
      "TripPatternForDate{" + "tripPattern=" + tripPattern + ", serviceDate=" + serviceDate + '}'
    );
  }

  @Nullable
  public TripPatternForDate newWithFilteredTripTimes(Predicate<TripTimes> filter) {
    ArrayList<TripTimes> filteredTripTimes = new ArrayList<>(tripTimes.length);
    for (TripTimes tripTimes : tripTimes) {
      if (filter.test(tripTimes)) {
        filteredTripTimes.add(tripTimes);
      }
    }

    List<FrequencyEntry> filteredFrequencies = new ArrayList<>(frequencies.length);
    for (FrequencyEntry frequencyEntry : frequencies) {
      if (filter.test(frequencyEntry.tripTimes)) {
        filteredFrequencies.add(frequencyEntry);
      }
    }

    if (filteredTripTimes.isEmpty() && filteredFrequencies.isEmpty()) {
      return null;
    }

    if (
      tripTimes.length == filteredTripTimes.size() &&
      frequencies.length == filteredFrequencies.size()
    ) {
      return this;
    }

    return new TripPatternForDate(tripPattern, filteredTripTimes, filteredFrequencies, serviceDate);
  }

  private static void assertValidRunningPeriod(
    LocalDate startOfRunningPeriod,
    LocalDate endOfRunningPeriod,
    TripTimes first,
    TripTimes last
  ) {
    if (first.getTrip().getRoute().getFlexibleLineType() != null) {
      // do not validate running period for flexible trips
      return;
    }
    if (startOfRunningPeriod.isAfter(endOfRunningPeriod)) {
      LOG.warn(
        "Could not construct as start of the running period {} in trip {} is after the end {} in trip {}",
        startOfRunningPeriod,
        first.getTrip().getId(),
        endOfRunningPeriod,
        last.getTrip().getId()
      );
      throw new IllegalArgumentException(
        "Start of the running period is after end of the running period"
      );
    }
  }
}

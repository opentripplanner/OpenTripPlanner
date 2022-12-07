package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate implements Comparable<TripPatternForDate> {

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

  /** The date for which the filtering was performed. */
  private final LocalDate localDate;

  /**
   * The date on which the first trip departs.
   */
  private final LocalDate startOfRunningPeriod;

  /**
   * The date on which the last trip arrives.
   */
  private final LocalDate endOfRunningPeriod;

  public TripPatternForDate(
    RoutingTripPattern tripPattern,
    List<TripTimes> tripTimes,
    List<FrequencyEntry> frequencies,
    LocalDate localDate
  ) {
    this.tripPattern = tripPattern;
    this.tripTimes = tripTimes.toArray(new TripTimes[0]);
    this.frequencies = frequencies.toArray(new FrequencyEntry[0]);
    this.localDate = localDate;

    // TODO: We expect a pattern only containing trips or frequencies, fix ability to merge
    if (hasFrequencies()) {
      this.startOfRunningPeriod =
        ServiceDateUtils
          .asDateTime(
            localDate,
            frequencies
              .stream()
              .mapToInt(frequencyEntry -> frequencyEntry.startTime)
              .min()
              .orElseThrow()
          )
          .toLocalDate();

      this.endOfRunningPeriod =
        ServiceDateUtils
          .asDateTime(
            localDate,
            frequencies
              .stream()
              .mapToInt(frequencyEntry -> frequencyEntry.endTime)
              .max()
              .orElseThrow()
          )
          .toLocalDate();
    } else {
      // These depend on the tripTimes array being sorted
      this.startOfRunningPeriod =
        ServiceDateUtils.asDateTime(localDate, tripTimes.get(0).getDepartureTime(0)).toLocalDate();
      var last = tripTimes.get(tripTimes.size() - 1);
      this.endOfRunningPeriod =
        ServiceDateUtils
          .asDateTime(localDate, last.getArrivalTime(last.getNumStops() - 1))
          .toLocalDate();
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

  public LocalDate getLocalDate() {
    return localDate;
  }

  public int numberOfTripSchedules() {
    return tripTimes.length;
  }

  public LocalDate getStartOfRunningPeriod() {
    return startOfRunningPeriod;
  }

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
    return localDate.compareTo(other.localDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      tripPattern,
      localDate,
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
      localDate.equals(that.localDate) &&
      Arrays.equals(tripTimes, that.tripTimes) &&
      Arrays.equals(frequencies, that.frequencies)
    );
  }

  @Override
  public String toString() {
    return "TripPatternForDate{" + "tripPattern=" + tripPattern + ", localDate=" + localDate + '}';
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

    return new TripPatternForDate(tripPattern, filteredTripTimes, filteredFrequencies, localDate);
  }
}

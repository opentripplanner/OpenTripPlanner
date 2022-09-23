package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.util.time.ServiceDateUtils;

/**
 * A TripPattern with its TripSchedules filtered by validity on a particular date. This is to avoid
 * having to do any filtering by date during the search itself.
 */
public class TripPatternForDate {

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
  private final List<TripTimes> tripTimes;

  /**
   * The filtered FrequencyEntries for only those entries in the TripPattern that are active on the
   * given day. Invariant: this array should contain a subset of the TripSchedules in
   * tripPattern.frequencyEntries.
   */
  private final List<FrequencyEntry> frequencies;

  /** The date for which the filtering was performed. */
  private final LocalDate localDate;

  /**
   * The first departure time of the first trip.
   */
  private final LocalDateTime startOfRunningPeriod;

  /**
   * The last arrival time of the last trip.
   */
  private final LocalDateTime endOfRunningPeriod;

  public TripPatternForDate(
    RoutingTripPattern tripPattern,
    List<TripTimes> tripTimes,
    List<FrequencyEntry> frequencies,
    LocalDate localDate
  ) {
    this.tripPattern = tripPattern;
    this.tripTimes = List.copyOf(tripTimes);
    this.frequencies = List.copyOf(frequencies);
    this.localDate = localDate;

    // TODO: We expect a pattern only containing trips or frequencies, fix ability to merge
    if (hasFrequencies()) {
      this.startOfRunningPeriod =
        ServiceDateUtils.asDateTime(
          localDate,
          frequencies
            .stream()
            .mapToInt(frequencyEntry -> frequencyEntry.startTime)
            .min()
            .orElseThrow()
        );

      this.endOfRunningPeriod =
        ServiceDateUtils.asDateTime(
          localDate,
          frequencies
            .stream()
            .mapToInt(frequencyEntry -> frequencyEntry.endTime)
            .max()
            .orElseThrow()
        );
    } else {
      // These depend on the tripTimes array being sorted
      this.startOfRunningPeriod =
        ServiceDateUtils.asDateTime(localDate, tripTimes.get(0).getDepartureTime(0));
      var last = tripTimes.get(tripTimes.size() - 1);
      this.endOfRunningPeriod =
        ServiceDateUtils.asDateTime(localDate, last.getArrivalTime(last.getNumStops() - 1));
    }
  }

  public List<TripTimes> tripTimes() {
    return tripTimes;
  }

  public List<FrequencyEntry> getFrequencies() {
    return frequencies;
  }

  public RoutingTripPattern getTripPattern() {
    return tripPattern;
  }

  public int stopIndex(int i) {
    return this.tripPattern.stopIndex(i);
  }

  public TripTimes getTripTimes(int i) {
    return tripTimes.get(i);
  }

  public LocalDate getLocalDate() {
    return localDate;
  }

  public int numberOfTripSchedules() {
    return tripTimes.size();
  }

  public LocalDateTime getStartOfRunningPeriod() {
    return startOfRunningPeriod;
  }

  public List<LocalDate> getRunningPeriodDates() {
    // Add one day to ensure last day is included
    return startOfRunningPeriod
      .toLocalDate()
      .datesUntil(endOfRunningPeriod.toLocalDate().plusDays(1))
      .collect(Collectors.toList());
  }

  public boolean hasFrequencies() {
    return !frequencies.isEmpty();
  }

  public int hashCode() {
    return Objects.hash(tripPattern, tripTimes, localDate);
  }

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
      tripTimes.equals(that.tripTimes)
    );
  }

  @Override
  public String toString() {
    return "TripPatternForDate{" + "tripPattern=" + tripPattern + ", localDate=" + localDate + '}';
  }

  @Nullable
  public TripPatternForDate newWithFilteredTripTimes(Predicate<TripTimes> filter) {
    ArrayList<TripTimes> filteredTripTimes = new ArrayList<>(tripTimes.size());
    for (TripTimes tripTimes : tripTimes) {
      if (filter.test(tripTimes)) {
        filteredTripTimes.add(tripTimes);
      }
    }

    List<FrequencyEntry> filteredFrequencies = new ArrayList<>(frequencies.size());
    for (FrequencyEntry frequencyEntry : frequencies) {
      if (filter.test(frequencyEntry.tripTimes)) {
        filteredFrequencies.add(frequencyEntry);
      }
    }

    if (filteredTripTimes.isEmpty() && filteredFrequencies.isEmpty()) {
      return null;
    }

    if (
      tripTimes.size() == filteredTripTimes.size() &&
      frequencies.size() == filteredFrequencies.size()
    ) {
      return this;
    }

    return new TripPatternForDate(tripPattern, filteredTripTimes, filteredFrequencies, localDate);
  }
}

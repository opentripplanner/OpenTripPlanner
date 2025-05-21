package org.opentripplanner.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * A Timetable is a TripTimes (stop-level details like arrival and departure times) for each of the
 * trips on a particular TripPattern.
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more than
 * one Timetable when stop time updates are being applied: one for the scheduled stop times, one for
 * each snapshot of updated stop times, another for a working buffer of updated stop times, etc.
 */
public class Timetable implements Serializable {

  private final TripPattern pattern;

  private final List<TripTimes> tripTimes;

  private final List<FrequencyEntry> frequencyEntries;

  @Nullable
  private final LocalDate serviceDate;

  Timetable(TimetableBuilder timetableBuilder) {
    this.pattern = timetableBuilder.getPattern();
    this.serviceDate = timetableBuilder.getServiceDate();
    this.tripTimes = timetableBuilder.createImmutableOrderedListOfTripTimes();
    this.frequencyEntries = List.copyOf(timetableBuilder.getFrequencies());
  }

  /** Construct an empty Timetable. */
  public static TimetableBuilder of() {
    return new TimetableBuilder();
  }

  /**
   * Copy timetable into a builder which can be used to modify the timetable.
   */
  public TimetableBuilder copyOf() {
    return new TimetableBuilder(this);
  }

  @Nullable
  public TripTimes getTripTimes(Trip trip) {
    for (TripTimes tt : tripTimes) {
      if (tt.getTrip() == trip) {
        return tt;
      }
    }
    return null;
  }

  public TripTimes getTripTimes(FeedScopedId tripId) {
    for (TripTimes tt : tripTimes) {
      if (tt.getTrip().getId().equals(tripId)) {
        return tt;
      }
    }
    return null;
  }

  public boolean isValidFor(LocalDate serviceDate) {
    return this.serviceDate == null || this.serviceDate.equals(serviceDate);
  }

  /** Find and cache service codes. Duplicates information in trip.getServiceId for optimization. */
  // TODO maybe put this is a more appropriate place
  public void setServiceCodes(Map<FeedScopedId, Integer> serviceCodes) {
    for (TripTimes tt : this.tripTimes) {
      ((RealTimeTripTimes) tt).setServiceCode(serviceCodes.get(tt.getTrip().getServiceId()));
    }
    // Repeated code... bad sign...
    for (FrequencyEntry freq : this.frequencyEntries) {
      TripTimes tt = freq.tripTimes;
      ((RealTimeTripTimes) tt).setServiceCode(serviceCodes.get(tt.getTrip().getServiceId()));
    }
  }

  /**
   * A circular reference between TripPatterns and their scheduled (non-updated) timetables.
   */
  public TripPattern getPattern() {
    return pattern;
  }

  /**
   * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
   * additional TripTimes objects for unscheduled trips. Frequency entries are stored separately.
   */
  public List<TripTimes> getTripTimes() {
    return tripTimes;
  }

  /**
   * Contains one FrequencyEntry object for each block of frequency-based trips.
   */
  public List<FrequencyEntry> getFrequencyEntries() {
    return frequencyEntries;
  }

  /**
   * The ServiceDate for which this (updated) timetable is valid. If null, then it is valid for all
   * dates.
   */
  @Nullable
  public LocalDate getServiceDate() {
    return serviceDate;
  }

  /**
   * Return the direction for all the trips in this timetable.
   * By construction, all trips in a timetable have the same direction.
   */
  public Direction getDirection() {
    return getDirection(tripTimes, frequencyEntries);
  }

  /**
   * Return an arbitrary TripTimes in this Timetable.
   * Return a scheduled trip times if it exists, otherwise return a frequency-based trip times.
   */
  public TripTimes getRepresentativeTripTimes() {
    return getRepresentativeTripTimes(tripTimes, frequencyEntries);
  }

  /**
   * @return true if the timetable was created by a real-time update, false if this
   * timetable is based on scheduled data.
   * Only real-time timetables have a service date.
   */
  public boolean isCreatedByRealTimeUpdater() {
    return serviceDate != null;
  }

  /**
   * The direction for the given collections of trip times.
   * The method assumes that all trip times have the same directions and picks up one arbitrarily.
   * @param scheduledTripTimes all the scheduled-based trip times in a timetable.
   * @param frequencies all the frequency-based trip times in a timetable.
   */
  static Direction getDirection(
    Collection<TripTimes> scheduledTripTimes,
    Collection<FrequencyEntry> frequencies
  ) {
    return Optional.ofNullable(getRepresentativeTripTimes(scheduledTripTimes, frequencies))
      .map(TripTimes::getTrip)
      .map(Trip::getDirection)
      .orElse(Direction.UNKNOWN);
  }

  /**
   * Return an arbitrary TripTimes.
   * @param scheduledTripTimes all the scheduled-based trip times in a timetable.
   * @param frequencies all the frequency-based trip times in a timetable.
   *
   */
  private static TripTimes getRepresentativeTripTimes(
    Collection<TripTimes> scheduledTripTimes,
    Collection<FrequencyEntry> frequencies
  ) {
    if (!scheduledTripTimes.isEmpty()) {
      return scheduledTripTimes.iterator().next();
    } else if (!frequencies.isEmpty()) {
      return frequencies.iterator().next().tripTimes;
    } else {
      // Pattern is created only for real-time updates
      return null;
    }
  }
}

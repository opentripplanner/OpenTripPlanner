package org.opentripplanner.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class TimetableBuilder {

  private TripPattern pattern;
  private LocalDate serviceDate;
  private final List<TripTimes> tripTimes = new ArrayList<>();
  private final List<FrequencyEntry> frequencies = new ArrayList<>();

  TimetableBuilder() {}

  TimetableBuilder(Timetable tt) {
    pattern = tt.getPattern();
    serviceDate = tt.getServiceDate();
    tripTimes.addAll(tt.getTripTimes());
    frequencies.addAll(tt.getFrequencyEntries());
  }

  public TimetableBuilder withTripPattern(TripPattern tripPattern) {
    this.pattern = tripPattern;
    return this;
  }

  public TimetableBuilder withServiceDate(LocalDate serviceDate) {
    this.serviceDate = serviceDate;
    return this;
  }

  public TimetableBuilder addTripTimes(TripTimes tripTimes) {
    this.tripTimes.add(tripTimes);
    return this;
  }

  public TimetableBuilder addAllTripTimes(List<TripTimes> tripTimes) {
    this.tripTimes.addAll(tripTimes);
    return this;
  }

  public TimetableBuilder setTripTimes(int tripIndex, TripTimes tripTimes) {
    this.tripTimes.set(tripIndex, tripTimes);
    return this;
  }

  public TimetableBuilder removeTripTimes(TripTimes tripTimesToRemove) {
    tripTimes.remove(tripTimesToRemove);
    return this;
  }

  public TimetableBuilder removeAllTripTimes(Set<TripTimes> tripTimesToBeRemoved) {
    tripTimes.removeAll(tripTimesToBeRemoved);
    return this;
  }

  /**
   * Apply the same update to all trip-times including scheduled and frequency based
   * trip times.
   * <p>
   */
  public TimetableBuilder updateAllTripTimes(UnaryOperator<TripTimes> update) {
    tripTimes.replaceAll(update);
    frequencies.replaceAll(it ->
      new FrequencyEntry(
        it.startTime,
        it.endTime,
        it.headway,
        it.exactTimes,
        update.apply(it.tripTimes)
      )
    );
    return this;
  }

  public TimetableBuilder addFrequencyEntry(FrequencyEntry frequencyEntry) {
    this.frequencies.add(frequencyEntry);
    return this;
  }

  public TripPattern getPattern() {
    return pattern;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public List<TripTimes> getTripTimes() {
    return tripTimes;
  }

  public List<FrequencyEntry> getFrequencies() {
    return frequencies;
  }

  /**
   * The direction for all the trips in this timetable.
   */
  public Direction getDirection() {
    return Optional
      .ofNullable(getRepresentativeTripTimes())
      .map(TripTimes::getTrip)
      .map(Trip::getDirection)
      .orElse(Direction.UNKNOWN);
  }

  private TripTimes getRepresentativeTripTimes() {
    if (!tripTimes.isEmpty()) {
      return tripTimes.getFirst();
    } else if (!frequencies.isEmpty()) {
      return frequencies.getFirst().tripTimes;
    } else {
      // Pattern is created only for real-time updates
      return null;
    }
  }

  public Timetable build() {
    return new Timetable(this);
  }
}

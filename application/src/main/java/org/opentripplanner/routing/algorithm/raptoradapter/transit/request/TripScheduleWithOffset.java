package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.LocalDate;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This represents a single trip within a TripPattern, but with a time offset in seconds. This is
 * used to represent a trip on a subsequent service day than the first one in the date range used.
 * <p>
 * Use flyweight pattern, reusing TripPatternForDates data
 */
public final class TripScheduleWithOffset implements TripSchedule {

  private final TripPatternForDates pattern;
  private final int sortIndex;
  private final int tripIndexForDates;
  private final IntUnaryOperator arrivalTimes;
  private final IntUnaryOperator departureTimes;

  // Computed when needed later for RaptorPathToItineraryMapper
  private TripTimes tripTimes = null;
  private LocalDate serviceDate = null;
  private int secondsOffset;

  TripScheduleWithOffset(TripPatternForDates pattern, int tripIndexForDates) {
    this.tripIndexForDates = tripIndexForDates;
    this.pattern = pattern;

    // get arrival/departures lambda
    this.arrivalTimes = pattern.getArrivalTimesForTrip(tripIndexForDates);
    this.departureTimes = pattern.getDepartureTimesForTrip(tripIndexForDates);

    // Trip times are sorted based on the arrival times at stop 0,
    this.sortIndex = arrivalTimes.applyAsInt(0);
  }

  @Override
  public int tripSortIndex() {
    return sortIndex;
  }

  @Override
  public int arrival(int stopPosInPattern) {
    return this.arrivalTimes.applyAsInt(stopPosInPattern);
  }

  @Override
  public int departure(int stopPosInPattern) {
    return this.departureTimes.applyAsInt(stopPosInPattern);
  }

  @Override
  public RaptorTripPattern pattern() {
    return pattern;
  }

  @Override
  public int transitReluctanceFactorIndex() {
    return pattern.transitReluctanceFactorIndex();
  }

  @Override
  public Accessibility wheelchairBoarding() {
    return pattern.wheelchairBoardingForTrip(tripIndexForDates);
  }

  /*
   * Following methods are only called in RaptorPathToItineraryMapper, instantiation or debug/tests, these are not optimised for performance
   */
  @Override
  public TripTimes getOriginalTripTimes() {
    if (tripTimes == null) {
      this.findTripTimes();
    }
    return this.tripTimes;
  }

  @Override
  public TripPattern getOriginalTripPattern() {
    return pattern.getTripPattern().getPattern();
  }

  @Override
  public LocalDate getServiceDate() {
    if (tripTimes == null) {
      this.findTripTimes();
    }
    return serviceDate;
  }

  public int getSecondsOffset() {
    if (tripTimes == null) {
      this.findTripTimes();
    }
    return secondsOffset;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripScheduleWithOffset.class)
      .addObj("info", pattern.debugInfo())
      .addObjOpSafe("id", () -> tripTimes.getTrip().getId())
      .addObjOpSafe("pattern.id", () -> pattern.getTripPattern().getPattern().getId())
      .addServiceTime("depart", secondsOffset + getOriginalTripTimes().getDepartureTime(0))
      .toString();
  }

  private void findTripTimes() {
    int index = tripIndexForDates;
    IntIterator indexIterator = pattern.tripPatternForDatesIndexIterator(true);
    while (indexIterator.hasNext()) {
      int i = indexIterator.next();
      TripPatternForDate tripPatternForDate = pattern.tripPatternForDate(i);
      int numSchedules = tripPatternForDate.numberOfTripSchedules();

      if (index < numSchedules) {
        this.tripTimes = tripPatternForDate.getTripTimes(index);
        this.serviceDate = tripPatternForDate.getServiceDate();
        this.secondsOffset = pattern.tripPatternForDateOffsets(i);
        return;
      }
      index -= numSchedules;
    }
    throw new IndexOutOfBoundsException("Index out of bound: " + index);
  }
}

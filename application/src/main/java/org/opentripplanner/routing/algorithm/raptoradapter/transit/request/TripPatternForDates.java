package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.util.IntIterators;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency.TripFrequencyAlightSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency.TripFrequencyBoardSearch;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A collection of all the TripSchedules active on a range of consecutive days. The outer list of
 * tripSchedulesByDay refers to days in order.
 */
public class TripPatternForDates
  implements
    RaptorRoute<TripSchedule>,
    RaptorTimeTable<TripSchedule>,
    DefaultTripPattern,
    TripSearchTimetable<TripSchedule> {

  private final RoutingTripPattern tripPattern;

  private final TripPatternForDate[] tripPatternForDates;

  private final int[] offsets;

  private final int numberOfTripSchedules;

  private final boolean isFrequencyBased;

  /**
   * The arrival times in a nStops * numberOfTripSchedules sized array. The trips are stored first
   * by the stop position and then by trip index, so with stops 1 and 2, and trips A and B, the
   * order is [1A, 1B, 2A, 2B]
   */
  private final int[] arrivalTimes;

  /**
   * The arrival times in a nStops * numberOfTripSchedules sized array. The order is the same as in
   * arrivalTimes.
   */
  private final int[] departureTimes;

  private final Accessibility[] wheelchairBoardings;

  // bit arrays with boarding/alighting information for all stops on trip pattern,
  // potentially filtered by wheelchair accessibility
  private final BitSet boardingPossible;
  private final BitSet alightingPossible;

  private final int priorityGroupId;

  TripPatternForDates(
    RoutingTripPattern tripPattern,
    TripPatternForDate[] tripPatternForDates,
    int[] offsets,
    BitSet boardingPossible,
    BitSet alightningPossible,
    int priorityGroupId
  ) {
    this.tripPattern = tripPattern;
    this.tripPatternForDates = tripPatternForDates;
    this.offsets = offsets;
    this.boardingPossible = boardingPossible;
    this.alightingPossible = alightningPossible;
    this.priorityGroupId = priorityGroupId;

    int numberOfTripSchedules = 0;
    boolean hasFrequencies = false;
    for (TripPatternForDate tripPatternForDate : this.tripPatternForDates) {
      numberOfTripSchedules += tripPatternForDate.numberOfTripSchedules();
      if (tripPatternForDate.hasFrequencies()) {
        hasFrequencies = true;
      }
    }
    this.numberOfTripSchedules = numberOfTripSchedules;
    this.isFrequencyBased = hasFrequencies;

    wheelchairBoardings = new Accessibility[numberOfTripSchedules];

    final int nStops = tripPattern.numberOfStopsInPattern();
    this.arrivalTimes = new int[nStops * numberOfTripSchedules];
    this.departureTimes = new int[nStops * numberOfTripSchedules];
    int i = 0;
    for (int d = 0; d < this.tripPatternForDates.length; d++) {
      int offset = this.offsets[d];
      for (var trip : this.tripPatternForDates[d].tripTimes()) {
        wheelchairBoardings[i] = trip.getWheelchairAccessibility();
        for (int s = 0; s < nStops; s++) {
          this.arrivalTimes[s * numberOfTripSchedules + i] = trip.getArrivalTime(s) + offset;
          this.departureTimes[s * numberOfTripSchedules + i] = trip.getDepartureTime(s) + offset;
        }
        i++;
      }
    }
  }

  public RoutingTripPattern getTripPattern() {
    return tripPattern;
  }

  /* Support for frequency based routing */

  public IntIterator tripPatternForDatesIndexIterator(boolean ascendingOnDate) {
    return ascendingOnDate
      ? IntIterators.intIncIterator(0, tripPatternForDates.length)
      : IntIterators.intDecIterator(tripPatternForDates.length, 0);
  }

  public TripPatternForDate tripPatternForDate(int index) {
    return tripPatternForDates[index];
  }

  /**
   * @deprecated This is exposed because it is needed in the TripFrequencyNnnSearch classes, but is
   * an implementation detail that should not leak outside the class.
   */
  @Deprecated
  public int tripPatternForDateOffsets(int index) {
    return offsets[index];
  }

  // Implementing RaptorRoute
  @Override
  public RaptorTimeTable<TripSchedule> timetable() {
    return this;
  }

  @Override
  public RaptorTripPattern pattern() {
    return this;
  }

  /* Implementing RaptorTripPattern */

  @Override
  public int patternIndex() {
    return tripPattern.patternIndex();
  }

  @Override
  public int numberOfStopsInPattern() {
    return tripPattern.numberOfStopsInPattern();
  }

  @Override
  public int stopIndex(int stopPositionInPattern) {
    return tripPattern.stopIndex(stopPositionInPattern);
  }

  @Override
  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return boardingPossible.get(stopPositionInPattern);
  }

  @Override
  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return alightingPossible.get(stopPositionInPattern);
  }

  @Override
  public int slackIndex() {
    return tripPattern.slackIndex();
  }

  @Override
  public int priorityGroupId() {
    return priorityGroupId;
  }

  public int transitReluctanceFactorIndex() {
    return tripPattern.transitReluctanceFactorIndex();
  }

  @Override
  public String debugInfo() {
    return tripPattern.debugInfo();
  }

  /*  Implementing RaptorTimeTable */

  @Override
  public RaptorTripScheduleSearch<TripSchedule> tripSearch(SearchDirection direction) {
    if (useCustomizedTripSearch()) {
      return createCustomizedTripSearch(direction);
    }
    return TripScheduleSearchFactory.create(direction, this);
  }

  @Override
  public TripSchedule getTripSchedule(int index) {
    return new TripScheduleWithOffset(this, index);
  }

  @Override
  public IntUnaryOperator getArrivalTimes(int stopPositionInPattern) {
    final int base = stopPositionInPattern * numberOfTripSchedules;
    return (int index) -> arrivalTimes[base + index];
  }

  @Override
  public IntUnaryOperator getDepartureTimes(int stopPositionInPattern) {
    final int base = stopPositionInPattern * numberOfTripSchedules;
    return (int index) -> departureTimes[base + index];
  }

  public IntUnaryOperator getArrivalTimesForTrip(int index) {
    return (int stopPositionInPattern) ->
      arrivalTimes[stopPositionInPattern * numberOfTripSchedules + index];
  }

  public IntUnaryOperator getDepartureTimesForTrip(int index) {
    return (int stopPositionInPattern) ->
      departureTimes[stopPositionInPattern * numberOfTripSchedules + index];
  }

  @Override
  public int numberOfTripSchedules() {
    return numberOfTripSchedules;
  }

  @Override
  public Route route() {
    return tripPattern.route();
  }

  /**
   * Raptor provides a trips search for regular trip schedules, but in some cases it makes
   * sense to be able to override this - for example for frequency based trips.
   *
   * @return {@code true} If you do not want to use the built-in trip search and instead
   *         will provide your own. Make sure to implement the
   *         {@link #createCustomizedTripSearch(SearchDirection)} for both forward and reverse
   *         searches.
   */
  public boolean useCustomizedTripSearch() {
    return isFrequencyBased;
  }

  /**
   * Factory method to provide an alternative trip search in Raptor.
   * @see #useCustomizedTripSearch()
   */
  public RaptorTripScheduleSearch<TripSchedule> createCustomizedTripSearch(
    SearchDirection direction
  ) {
    return direction.isForward()
      ? new TripFrequencyBoardSearch<>(this)
      : new TripFrequencyAlightSearch<>(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripPatternForDates.class)
      .addObj("pattern", debugInfo())
      .addServiceTimeSchedule("offsets", offsets)
      .addNum("nTrips", numberOfTripSchedules)
      .toString();
  }

  public Accessibility wheelchairBoardingForTrip(int index) {
    return wheelchairBoardings[index];
  }
}

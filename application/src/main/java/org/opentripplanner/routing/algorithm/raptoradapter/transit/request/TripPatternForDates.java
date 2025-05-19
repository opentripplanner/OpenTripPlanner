package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.Arrays;
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

  private static final int FIRST_STOP_POS_IN_PATTERN = 0;

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

    this.wheelchairBoardings = new Accessibility[numberOfTripSchedules];

    final int nStops = tripPattern.numberOfStopsInPattern();
    this.arrivalTimes = new int[nStops * numberOfTripSchedules];
    this.departureTimes = new int[nStops * numberOfTripSchedules];

    var tripIndex = createTripTimesForDaysIndex(tripPatternForDates, offsets);

    for (int i = 0; i < tripIndex.size(); ++i) {
      int day = tripIndex.day(i);
      int offset = this.offsets[day];
      var tt = tripPatternForDates[day].tripTimes().get(tripIndex.tripIndexForDay(i));

      wheelchairBoardings[i] = tt.getWheelchairAccessibility();
      for (int s = 0; s < nStops; s++) {
        this.arrivalTimes[s * numberOfTripSchedules + i] = tt.getArrivalTime(s) + offset;
        this.departureTimes[s * numberOfTripSchedules + i] = tt.getDepartureTime(s) + offset;
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

  public TripPatternForDate tripPatternForDate(int dayIndex) {
    return tripPatternForDates[dayIndex];
  }

  /**
   * @deprecated This is exposed because it is needed in the TripFrequencyNnnSearch classes, but is
   * an implementation detail that should not leak outside the class.
   */
  @Deprecated
  public int tripPatternForDateOffsets(int dayIndex) {
    return offsets[dayIndex];
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
  public TripSchedule getTripSchedule(int tripIndex) {
    return new TripScheduleWithOffset(this, tripIndex);
  }

  @Override
  public IntUnaryOperator getArrivalTimes(int stopPositionInPattern) {
    final int base = stopPositionInPattern * numberOfTripSchedules;
    return (int tripIndex) -> arrivalTimes[base + tripIndex];
  }

  @Override
  public IntUnaryOperator getDepartureTimes(int stopPositionInPattern) {
    final int base = stopPositionInPattern * numberOfTripSchedules;
    return (int tripIndex) -> departureTimes[base + tripIndex];
  }

  public IntUnaryOperator getArrivalTimesForTrip(int tripIndex) {
    return (int stopPositionInPattern) ->
      arrivalTimes[stopPositionInPattern * numberOfTripSchedules + tripIndex];
  }

  public IntUnaryOperator getDepartureTimesForTrip(int tripIndex) {
    return (int stopPositionInPattern) ->
      departureTimes[stopPositionInPattern * numberOfTripSchedules + tripIndex];
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

  public Accessibility wheelchairBoardingForTrip(int tripIndex) {
    return wheelchairBoardings[tripIndex];
  }

  /**
   * Return a list with all depature times for the fisrt stop for each trip per day.
   *
   * There is not unit-test on this method, so keep the surface to {@link TripPatternForDate}
   * as thin as possible.
   */
  private static TripTimesForDaysIndex createTripTimesForDaysIndex(
    TripPatternForDate[] tripPatternForDates,
    int[] offsets
  ) {
    var depatureTimes = Arrays.stream(tripPatternForDates)
      .map(TripPatternForDate::tripTimes)
      .map(l -> l.stream().mapToInt(t -> t.getDepartureTime(FIRST_STOP_POS_IN_PATTERN)).toArray())
      .toList();
    return TripTimesForDaysIndex.ofTripTimesForDay(depatureTimes, offsets);
  }
}

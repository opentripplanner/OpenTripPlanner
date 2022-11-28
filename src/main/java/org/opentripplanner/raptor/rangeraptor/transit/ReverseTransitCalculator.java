package org.opentripplanner.raptor.rangeraptor.transit;

import java.util.Iterator;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.IntIterators;

/**
 * A calculator that will take you back in time not forward, this is the basic logic to implement a
 * reveres search.
 */
public final class ReverseTransitCalculator<T extends RaptorTripSchedule>
  extends ReverseTimeCalculator
  implements TransitCalculator<T> {

  private final int latestArrivalTime;
  private final int searchWindowInSeconds;
  private final int earliestAcceptableDepartureTime;
  private final int iterationStep;

  public ReverseTransitCalculator(SearchParams s, RaptorTuningParameters t) {
    // The request is already modified to search backwards, so 'earliestDepartureTime()'
    // goes with destination and 'latestArrivalTime()' match origin.
    this(
      s.latestArrivalTime(),
      s.searchWindowInSeconds(),
      s.earliestDepartureTime(),
      t.iterationDepartureStepInSeconds()
    );
  }

  ReverseTransitCalculator(
    int latestArrivalTime,
    int searchWindowInSeconds,
    int earliestAcceptableDepartureTime,
    int iterationStep
  ) {
    this.latestArrivalTime = latestArrivalTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.earliestAcceptableDepartureTime =
      earliestAcceptableDepartureTime == TIME_NOT_SET
        ? unreachedTime()
        : earliestAcceptableDepartureTime;
    this.iterationStep = iterationStep;
  }

  @Override
  public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
    return plusDuration(onTrip.departure(stopPositionInPattern), alightSlack);
  }

  @Override
  public boolean exceedsTimeLimit(int time) {
    return isBefore(earliestAcceptableDepartureTime, time);
  }

  @Override
  public String exceedsTimeLimitReason() {
    return (
      "The departure time exceeds the time limit, depart to early: " +
      TimeUtils.timeToStrLong(earliestAcceptableDepartureTime) +
      "."
    );
  }

  @Override
  public int departureTime(RaptorAccessEgress accessEgress, int departureTime) {
    return accessEgress.latestArrivalTime(departureTime);
  }

  @Override
  public IntIterator rangeRaptorMinutes() {
    return oneIterationOnly()
      ? IntIterators.singleValueIterator(latestArrivalTime)
      : IntIterators.intIncIterator(
        latestArrivalTime - searchWindowInSeconds,
        latestArrivalTime,
        iterationStep
      );
  }

  @Override
  public boolean oneIterationOnly() {
    return searchWindowInSeconds <= iterationStep;
  }

  @Override
  public IntIterator patternStopIterator(int nStopsInPattern) {
    return IntIterators.intDecIterator(nStopsInPattern, 0);
  }

  @Override
  public RaptorConstrainedTripScheduleBoardingSearch<T> transferConstraintsSearch(
    RaptorTransitDataProvider<T> transitData,
    int routeIndex
  ) {
    return transitData.transferConstraintsReverseSearch(routeIndex);
  }

  @Override
  public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.boardingPossibleAt(stopPos);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(
    RaptorTransitDataProvider<T> transitDataProvider,
    int fromStop
  ) {
    return transitDataProvider.getTransfersToStop(fromStop);
  }

  @Override
  public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.alightingPossibleAt(stopPos);
  }

  @Override
  public RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    return timeTable.tripSearch(SearchDirection.REVERSE);
  }

  @Override
  public RaptorTripScheduleSearch<T> createExactTripSearch(RaptorTimeTable<T> timeTable) {
    return new TripScheduleExactMatchSearch<>(createTripSearch(timeTable), this, -iterationStep);
  }
}

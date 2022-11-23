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
 * Used to calculate times in a forward trip search.
 */
public final class ForwardTransitCalculator<T extends RaptorTripSchedule>
  extends ForwardTimeCalculator
  implements TransitCalculator<T> {

  private final int earliestDepartureTime;
  private final int searchWindowInSeconds;
  private final int latestAcceptableArrivalTime;
  private final int iterationStep;

  public ForwardTransitCalculator(SearchParams s, RaptorTuningParameters t) {
    this(
      s.earliestDepartureTime(),
      s.searchWindowInSeconds(),
      s.latestArrivalTime(),
      t.iterationDepartureStepInSeconds()
    );
  }

  ForwardTransitCalculator(
    int earliestDepartureTime,
    int searchWindowInSeconds,
    int latestAcceptableArrivalTime,
    int iterationStep
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.latestAcceptableArrivalTime =
      latestAcceptableArrivalTime == TIME_NOT_SET ? unreachedTime() : latestAcceptableArrivalTime;
    this.iterationStep = iterationStep;
  }

  @Override
  public int stopArrivalTime(T onTrip, int stopPositionInPattern, int alightSlack) {
    return onTrip.arrival(stopPositionInPattern) + alightSlack;
  }

  @Override
  public boolean exceedsTimeLimit(int time) {
    return isBefore(latestAcceptableArrivalTime, time);
  }

  @Override
  public String exceedsTimeLimitReason() {
    return (
      "The arrival time exceeds the time limit, arrive to late: " +
      TimeUtils.timeToStrLong(latestAcceptableArrivalTime) +
      "."
    );
  }

  @Override
  public int departureTime(RaptorAccessEgress accessEgress, int departureTime) {
    return accessEgress.earliestDepartureTime(departureTime);
  }

  @Override
  public IntIterator rangeRaptorMinutes() {
    return oneIterationOnly()
      ? IntIterators.singleValueIterator(earliestDepartureTime)
      : IntIterators.intDecIterator(
        earliestDepartureTime + searchWindowInSeconds,
        earliestDepartureTime,
        iterationStep
      );
  }

  @Override
  public boolean oneIterationOnly() {
    return searchWindowInSeconds <= iterationStep;
  }

  @Override
  public IntIterator patternStopIterator(int nStopsInPattern) {
    return IntIterators.intIncIterator(0, nStopsInPattern);
  }

  @Override
  public RaptorConstrainedTripScheduleBoardingSearch<T> transferConstraintsSearch(
    RaptorTransitDataProvider<T> transitData,
    int routeIndex
  ) {
    return transitData.transferConstraintsForwardSearch(routeIndex);
  }

  @Override
  public boolean alightingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.alightingPossibleAt(stopPos);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(
    RaptorTransitDataProvider<T> transitDataProvider,
    int fromStop
  ) {
    return transitDataProvider.getTransfersFromStop(fromStop);
  }

  @Override
  public boolean boardingPossibleAt(RaptorTripPattern pattern, int stopPos) {
    return pattern.boardingPossibleAt(stopPos);
  }

  @Override
  public RaptorTripScheduleSearch<T> createTripSearch(RaptorTimeTable<T> timeTable) {
    return timeTable.tripSearch(SearchDirection.FORWARD);
  }

  @Override
  public RaptorTripScheduleSearch<T> createExactTripSearch(RaptorTimeTable<T> pattern) {
    return new TripScheduleExactMatchSearch<>(createTripSearch(pattern), this, iterationStep);
  }
}

package org.opentripplanner.raptor.rangeraptor.transit;

import java.util.Iterator;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.util.IntIterators;
import org.opentripplanner.utils.time.TimeUtils;

public final class ReverseRaptorTransitCalculator<T extends RaptorTripSchedule>
  extends ReverseTransitCalculator<T>
  implements RaptorTransitCalculator<T> {

  private final int latestArrivalTime;
  private final int searchWindowInSeconds;
  private final int earliestAcceptableDepartureTime;
  private final int iterationStep;

  public ReverseRaptorTransitCalculator(SearchParams s, RaptorTuningParameters t) {
    // The request is already modified to search backwards, so 'earliestDepartureTime()'
    // goes with destination and 'latestArrivalTime()' match origin.
    this(
      s.latestArrivalTime(),
      s.searchWindowInSeconds(),
      s.earliestDepartureTime(),
      t.iterationDepartureStepInSeconds()
    );
  }

  ReverseRaptorTransitCalculator(
    int latestArrivalTime,
    int searchWindowInSeconds,
    int earliestAcceptableDepartureTime,
    int iterationStep
  ) {
    this.latestArrivalTime = latestArrivalTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.earliestAcceptableDepartureTime = earliestAcceptableDepartureTime ==
      RaptorConstants.TIME_NOT_SET
      ? unreachedTime()
      : earliestAcceptableDepartureTime;
    this.iterationStep = iterationStep;
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
  public int minIterationDepartureTime() {
    return latestArrivalTime;
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
  public RaptorConstrainedBoardingSearch<T> transferConstraintsSearch(
    RaptorTransitDataProvider<T> transitData,
    int routeIndex
  ) {
    return transitData.transferConstraintsReverseSearch(routeIndex);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(
    RaptorTransitDataProvider<T> transitDataProvider,
    int fromStop
  ) {
    return transitDataProvider.getTransfersToStop(fromStop);
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

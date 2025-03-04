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

public final class ForwardRaptorTransitCalculator<T extends RaptorTripSchedule>
  extends ForwardTransitCalculator<T>
  implements RaptorTransitCalculator<T> {

  private final int earliestDepartureTime;
  private final int searchWindowInSeconds;
  private final int latestAcceptableArrivalTime;
  private final int iterationStep;

  public ForwardRaptorTransitCalculator(SearchParams s, RaptorTuningParameters t) {
    this(
      s.earliestDepartureTime(),
      s.searchWindowInSeconds(),
      s.latestArrivalTime(),
      t.iterationDepartureStepInSeconds()
    );
  }

  public ForwardRaptorTransitCalculator(
    int earliestDepartureTime,
    int searchWindowInSeconds,
    int latestAcceptableArrivalTime,
    int iterationStep
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.latestAcceptableArrivalTime = latestAcceptableArrivalTime == RaptorConstants.TIME_NOT_SET
      ? unreachedTime()
      : latestAcceptableArrivalTime;
    this.iterationStep = iterationStep;
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
  public int minIterationDepartureTime() {
    return earliestDepartureTime;
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
  public RaptorConstrainedBoardingSearch<T> transferConstraintsSearch(
    RaptorTransitDataProvider<T> transitData,
    int routeIndex
  ) {
    return transitData.transferConstraintsForwardSearch(routeIndex);
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(
    RaptorTransitDataProvider<T> transitDataProvider,
    int fromStop
  ) {
    return transitDataProvider.getTransfersFromStop(fromStop);
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

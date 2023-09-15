package org.opentripplanner.raptor.rangeraptor.transit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.IntPredicate;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptor.util.IntIterators;

public final class ForwardRaptorTransitCalculator<T extends RaptorTripSchedule>
  extends ForwardTransitCalculator<T>
  implements RaptorTransitCalculator<T> {

  private final int earliestDepartureTime;
  private final int searchWindowInSeconds;
  private final int latestAcceptableArrivalTime;
  private final int iterationStep;

  @Nullable
  private final IntPredicate acceptC2AtDestination;

  public ForwardRaptorTransitCalculator(
    SearchParams s,
    RaptorTuningParameters t,
    @Nullable IntPredicate acceptC2AtDestination
  ) {
    this(
      s.routerEarliestDepartureTime(),
      s.routerSearchWindowInSeconds(),
      s.latestArrivalTime(),
      t.iterationDepartureStepInSeconds(),
      acceptC2AtDestination
    );
  }

  public ForwardRaptorTransitCalculator(
    int earliestDepartureTime,
    int searchWindowInSeconds,
    int latestAcceptableArrivalTime,
    int iterationStep,
    IntPredicate acceptC2AtDestination
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.latestAcceptableArrivalTime =
      latestAcceptableArrivalTime == RaptorConstants.TIME_NOT_SET
        ? unreachedTime()
        : latestAcceptableArrivalTime;
    this.iterationStep = iterationStep;
    this.acceptC2AtDestination = acceptC2AtDestination;
  }

  @Override
  public Collection<String> rejectDestinationArrival(ArrivalView<T> destArrival) {
    var errors = new ArrayList<String>();

    if (exceedsTimeLimit(destArrival.arrivalTime())) {
      errors.add(exceedsTimeLimitReason());
    }
    rejectC2AtDestination(destArrival).ifPresent(errors::add);

    return errors;
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

  /**
   * Test if the c2 value is acceptable, or should be rejected. If ok return nothing, if rejected
   * return the reason for the debug event log.
   */
  private Optional<String> rejectC2AtDestination(ArrivalView<T> destArrival) {
    return acceptC2AtDestination == null || acceptC2AtDestination.test(destArrival.c2())
      ? Optional.empty()
      : Optional.of("C2 value rejected: " + destArrival.c2() + ".");
  }
}

package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * The responsibility of this class is to provide transfer constraints to the Raptor search for a
 * given pattern. The instance is stateful and not thread-safe. The current stop position is checked
 * for transfers, then the provider is asked to list all transfers between the current pattern and
 * the source trip stop arrival. The source is the "from" point in a transfer for a forward search,
 * and the "to" point in the reverse search.
 */
public final class ConstrainedBoardingSearch
  implements RaptorConstrainedBoardingSearch<TripSchedule> {

  /**
   * Abort the search after looking at 5 valid boardings. In the case where this happens, one of
   * these trips are probably a better match. We abort to avoid stepping through all trips, possibly
   * a large number (several days).
   */
  private static final int ABORT_SEARCH_AFTER_N_VALID_NORMAL_TRIPS = 5;

  private static final ConstrainedBoardingSearchStrategy FORWARD_STRATEGY =
    new ConstrainedBoardingSearchForward();
  private static final ConstrainedBoardingSearchStrategy REVERSE_STRATEGY =
    new ConstrainedBoardingSearchReverse();
  public static final RaptorConstrainedBoardingSearch<TripSchedule> NOOP_SEARCH =
    new NoopRaptorConstrainedBoardingSearch();

  /** Handle forward and reverse specific tasks */
  private final ConstrainedBoardingSearchStrategy searchStrategy;

  /**
   * List of transfers used for boarding for each stop position in pattern
   */
  @Nullable
  private final TransferForPatternByStopPos transfersToTargetStop;

  /**
   * List of transfers for each stop position in pattern
   */
  @Nullable
  private final TransferForPatternByStopPos transfersFromSourceStop;

  private List<TransferForPattern> currentTransfers;
  private int currentTargetStopPos;

  // If we find a trip these variables are used to cache the result
  private int onTripEarliestBoardTime;
  private int onTripIndex;
  private TransferConstraint onTripTxConstraint;

  public ConstrainedBoardingSearch(
    boolean forwardSearch,
    @Nullable TransferForPatternByStopPos transfersFromSourceStop,
    @Nullable TransferForPatternByStopPos transfersToTargetStop
  ) {
    this.transfersToTargetStop = transfersToTargetStop;
    this.transfersFromSourceStop = transfersFromSourceStop;
    this.searchStrategy = forwardSearch ? FORWARD_STRATEGY : REVERSE_STRATEGY;
  }

  @Override
  public boolean transferExistTargetStop(int targetStopPos) {
    if (transfersToTargetStop == null) {
      return false;
    }

    // Get all guaranteed transfers for the target pattern at the target stop position
    this.currentTransfers = transfersToTargetStop.get(targetStopPos);
    this.currentTargetStopPos = targetStopPos;
    return currentTransfers != null;
  }

  @Override
  public boolean transferExistSourceStop(int sourceStopPos) {
    return transfersFromSourceStop != null && transfersFromSourceStop.get(sourceStopPos) != null;
  }

  @Override
  public RaptorBoardOrAlightEvent<TripSchedule> find(
    RaptorTimeTable<TripSchedule> timetable,
    int transferSlack,
    TripSchedule sourceTripSchedule,
    int sourceStopIndex,
    int prevTransitArrivalTime,
    int earliestBoardTime
  ) {
    var transfers = findMatchingTransfers(sourceTripSchedule, sourceStopIndex);

    if (!transfers.iterator().hasNext()) {
      return RaptorBoardOrAlightEvent.empty(earliestBoardTime);
    }

    boolean found = findTimetableTripInfo(
      timetable,
      transfers,
      transferSlack,
      currentTargetStopPos,
      prevTransitArrivalTime,
      earliestBoardTime
    );

    if (!found) {
      return RaptorBoardOrAlightEvent.empty(earliestBoardTime);
    }

    var trip = timetable.getTripSchedule(onTripIndex);
    int departureTime = searchStrategy.time(trip, currentTargetStopPos);

    return new ConstrainedTransferBoarding<>(
      onTripTxConstraint,
      onTripIndex,
      trip,
      currentTargetStopPos,
      departureTime,
      onTripEarliestBoardTime
    );
  }

  private Iterable<TransferForPattern> findMatchingTransfers(
    TripSchedule tripSchedule,
    int stopIndex
  ) {
    final Trip trip = tripSchedule.getOriginalTripTimes().getTrip();
    // for performance reasons we use a for loop here as streams are much slower.
    // I experimented with LinkedList and ArrayList and LinkedList was faster, presumably
    // because insertion is quick and we don't need index-based access, only iteration.
    var result = new LinkedList<TransferForPattern>();
    for (var t : currentTransfers) {
      if (t.matchesSourcePoint(stopIndex, trip)) {
        result.add(t);
      }
    }
    return result;
  }

  /**
   * Find the trip to board (trip index) and the transfer constraint.
   * <p>
   * This method sets the following parameters if successful:
   * <ul>
   *     <li>{@code onTripIndex}
   *     <li>{@code onTripTxConstraint}
   *     <li>{@code onTripEarliestBoardTime}
   * </ul>
   *
   * @return {@code true} if a matching trip is found
   */
  private boolean findTimetableTripInfo(
    RaptorTimeTable<TripSchedule> timetable,
    Iterable<TransferForPattern> transfers,
    int transferSlack,
    int stopPos,
    int sourceTransitArrivalTime,
    int earliestBoardTime
  ) {
    int nAllowedBoardings = 0;
    boolean useNextNormalTrip = false;

    var index = searchStrategy.scheduleIndexIterator(timetable);
    outer: while (index.hasNext()) {
      onTripIndex = index.next();
      var it = timetable.getTripSchedule(onTripIndex);

      // Forward: boardTime, Reverse: alightTime
      int time = searchStrategy.time(it, stopPos);

      if (searchStrategy.timeIsBefore(time, sourceTransitArrivalTime)) {
        continue;
      }

      ++nAllowedBoardings;

      var targetTrip = it.getOriginalTripTimes().getTrip();

      for (TransferForPattern tx : transfers) {
        onTripTxConstraint = (TransferConstraint) tx.getTransferConstraint();

        onTripEarliestBoardTime = onTripTxConstraint.calculateTransferTargetTime(
          sourceTransitArrivalTime,
          transferSlack,
          () -> earliestBoardTime,
          searchStrategy.direction()
        );

        if (!onTripTxConstraint.isFacilitated()) {
          if (searchStrategy.timeIsBefore(time, onTripEarliestBoardTime)) {
            continue;
          }
        }

        if (tx.applyToAllTargetTrips()) {
          return true;
        } else if (tx.applyToTargetTrip(targetTrip)) {
          if (onTripTxConstraint.isNotAllowed()) {
            useNextNormalTrip = true;
            continue outer;
          } else {
            return true;
          }
        }
      }
      if (useNextNormalTrip) {
        onTripEarliestBoardTime = earliestBoardTime;
        onTripTxConstraint = TransferConstraint.REGULAR_TRANSFER;
        return true;
      }
      if (nAllowedBoardings == ABORT_SEARCH_AFTER_N_VALID_NORMAL_TRIPS) {
        return false;
      }
    }
    return false;
  }

  private static final class NoopRaptorConstrainedBoardingSearch
    implements RaptorConstrainedBoardingSearch<TripSchedule> {

    @Override
    public boolean transferExistTargetStop(int targetStopPos) {
      return false;
    }

    @Override
    public boolean transferExistSourceStop(int targetStopPos) {
      return false;
    }

    @Override
    public RaptorBoardOrAlightEvent<TripSchedule> find(
      RaptorTimeTable<TripSchedule> targetTimetable,
      int transferSlack,
      TripSchedule sourceTrip,
      int sourceStopIndex,
      int prevTransitArrivalTime,
      int earliestBoardTime
    ) {
      return null;
    }
  }
}

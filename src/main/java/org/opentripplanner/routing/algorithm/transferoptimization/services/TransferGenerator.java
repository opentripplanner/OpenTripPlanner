package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * This class is responsible for finding all possible transfers between each pair of transit legs
 * passed in. The configured slack should be respected, and the transfers found here should be
 * equivalent to the transfers explored during routing (in Raptor).
 * <p>
 * This class also filters away transfers which cannot be used due to time constraints. For example,
 * if a transfer point is before the earliest possible boarding or after the latest possible
 * arrival. Transfer constraints should also be respected.
 * <p>
 * This service does NOT combine transfers between various trips to form full paths. There are
 * potentially millions of permutations, so we do that later when we can prune the result.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TransferGenerator<T extends RaptorTripSchedule> {

  private static final int SAME_STOP_TRANSFER_TIME = 0;

  private final TransferServiceAdaptor<T> transferServiceAdaptor;
  private final RaptorSlackProvider slackProvider;
  private final RaptorTransitDataProvider<T> stdTransfers;

  private T fromTrip;
  private T toTrip;

  public TransferGenerator(
    TransferServiceAdaptor<T> transferServiceAdaptor,
    RaptorSlackProvider slackProvider,
    RaptorTransitDataProvider<T> stdTransfers
  ) {
    this.transferServiceAdaptor = transferServiceAdaptor;
    this.slackProvider = slackProvider;
    this.stdTransfers = stdTransfers;
  }

  public List<List<TripToTripTransfer<T>>> findAllPossibleTransfers(
    List<TransitPathLeg<T>> transitLegs
  ) {
    List<List<TripToTripTransfer<T>>> result = new ArrayList<>();
    var fromLeg = transitLegs.get(0);
    TransitPathLeg<T> toLeg;
    var earliestDeparture = getFromStopTime(fromLeg);

    for (int i = 1; i < transitLegs.size(); i++) {
      toLeg = transitLegs.get(i);

      var transfers = findTransfers(fromLeg.trip(), earliestDeparture, toLeg.trip());

      result.add(transfers);

      // Setup next iteration
      earliestDeparture = findMinimumToStopTime(transfers);
      fromLeg = toLeg;
    }

    removeTransfersAfterLatestStopArrival(last(transitLegs).getToStopPosition(), result);

    return result;
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  private List<TripToTripTransfer<T>> findTransfers(
    T fromTrip,
    StopTime fromTripDeparture,
    T toTrip
  ) {
    this.fromTrip = fromTrip;
    this.toTrip = toTrip;

    int firstStopPos = firstPossibleArrivalStopPos(fromTrip, fromTripDeparture);
    return findAllTransfers(firstStopPos);
  }

  /** Given the trip and departure, find the first possible stop position to alight. */
  private int firstPossibleArrivalStopPos(T trip, StopTime departure) {
    return 1 + trip.findDepartureStopPosition(departure.time(), departure.stop());
  }

  private List<TripToTripTransfer<T>> findAllTransfers(int stopPos) {
    final List<TripToTripTransfer<T>> result = new ArrayList<>();

    while (stopPos < fromTrip.pattern().numberOfStopsInPattern()) {
      boolean alightingPossible = fromTrip.pattern().alightingPossibleAt(stopPos);
      if (alightingPossible) {
        var from = TripStopTime.arrival(fromTrip, stopPos);

        // First add high priority transfers
        result.addAll(transferFromSameStop(from));
        result.addAll(findStandardTransfers(from));
      }

      ++stopPos;
    }
    return result;
  }

  /**
   * Find potential transfers where traveller does not have to "walk" between stops
   */
  private Collection<TripToTripTransfer<T>> transferFromSameStop(TripStopTime<T> from) {
    var result = new ArrayList<TripToTripTransfer<T>>();

    final int stop = from.stop();

    // Find all possible transfers on given stop index, starting on from.time
    var possibleTransfers = toTrip.findDepartureStopPositions(from.time(), stop);

    // Loop through transfers and decide whether they are possible
    for (var stopPos : possibleTransfers) {
      // Find transfer constraint for stop position
      var tx = transferServiceAdaptor.findTransfer(from, toTrip, stop, stopPos);

      if (!isAllowedTransfer(stopPos, tx)) {
        continue;
      }

      // Check whether traveller will have enough time to do the transfer
      // We have to do it here because every stop position may have unique transfer constraint
      // So it may be possible to transfer at stop position 2 but not on 1...
      final int earliestBoardTime = calculateEarliestBoardTime(from, tx, SAME_STOP_TRANSFER_TIME);

      if (earliestBoardTime > toTrip.departure(stopPos)) {
        continue;
      }

      // Add as a possible result
      result.add(new TripToTripTransfer<>(from, TripStopTime.departure(toTrip, stopPos), null, tx));
    }

    return result;
  }

  /**
   * Find potential transfers where traveller has to "walk" between stops
   */
  private Collection<? extends TripToTripTransfer<T>> findStandardTransfers(TripStopTime<T> from) {
    final List<TripToTripTransfer<T>> result = new ArrayList<>();
    Iterator<? extends RaptorTransfer> transfers = stdTransfers.getTransfersFromStop(from.stop());

    // Loop through transfers and decide whether they are possible
    while (transfers.hasNext()) {
      var it = transfers.next();
      int toStop = it.stop();

      // Find all possible transfers on given stop index, starting on from.time
      var possibleTransfers = toTrip.findDepartureStopPositions(from.time(), toStop);

      for (var stopPos : possibleTransfers) {
        // Find transfer constraint for stop position
        var tx = transferServiceAdaptor.findTransfer(from, toTrip, toStop, stopPos);

        if (!isAllowedTransfer(stopPos, tx)) {
          continue;
        }

        // Check whether traveller will have enough time to do the transfer
        // We have to do it here because every stopPos may have unique transfer constraint
        // So it may be possible to transfer at stop position 2 but not on 1 etc...
        int earliestBoardTime = calculateEarliestBoardTime(from, tx, it.durationInSeconds());

        if (earliestBoardTime > toTrip.departure(stopPos)) {
          continue;
        }

        var to = TripStopTime.departure(toTrip, stopPos);
        // Add as a possible result
        result.add(new TripToTripTransfer<>(from, to, it, tx));
      }
    }

    return result;
  }

  /**
   * This code duplicates the logic in
   * {@link org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedBoardingSearch},
   * see the {@code findTimetableTripInfo(RaptorTimeTable, Iterable, int, int, int)}) method.
   */
  private int calculateEarliestBoardTime(
    TripStopTime<T> from,
    @Nullable ConstrainedTransfer tx,
    int regularTransferDurationInSec
  ) {
    if (tx == null) {
      return calcRegularTransferEarliestBoardTime(from, regularTransferDurationInSec);
    }

    return tx
      .getTransferConstraint()
      .calculateTransferTargetTime(
        from.time(),
        slackProvider.transferSlack(),
        () -> calcRegularTransferEarliestBoardTime(from, regularTransferDurationInSec),
        SearchDirection.FORWARD
      );
  }

  private int calcRegularTransferEarliestBoardTime(
    TripStopTime<T> from,
    int transferDurationInSeconds
  ) {
    int transferDuration = slackProvider.calcRegularTransferDuration(
      transferDurationInSeconds,
      fromTrip.pattern().slackIndex(),
      toTrip.pattern().slackIndex()
    );
    return from.time() + transferDuration;
  }

  @Nonnull
  private StopTime getFromStopTime(final TransitPathLeg<T> leg) {
    return StopTime.stopTime(leg.fromStop(), leg.fromTime());
  }

  @Nonnull
  private TripStopTime<T> findMinimumToStopTime(List<TripToTripTransfer<T>> transfers) {
    return transfers
      .stream()
      .map(TripToTripTransfer::to)
      .min(Comparator.comparingInt(TripStopTime::time))
      .orElseThrow();
  }

  private void removeTransfersAfterLatestStopArrival(
    int latestArrivalStopPos,
    List<List<TripToTripTransfer<T>>> result
  ) {
    int nextLatestArrivalStopPos = 0;

    for (int i = result.size() - 1; i >= 0; --i) {
      List<TripToTripTransfer<T>> filteredTransfers = new ArrayList<>();

      for (TripToTripTransfer<T> tx : result.get(i)) {
        if (tx.to().stopPosition() < latestArrivalStopPos) {
          filteredTransfers.add(tx);
          nextLatestArrivalStopPos = Math.max(tx.from().stopPosition(), nextLatestArrivalStopPos);
        }
      }
      latestArrivalStopPos = nextLatestArrivalStopPos;
      result.set(i, filteredTransfers);
    }
  }

  /**
   * Based on trip pattern and transfer constraint check whether transfer at this point is possible
   * @param stopPosition stop position in destination trip pattern
   * @param tx optional transfer constraint
   * @return whether this transfer is possible
   */
  private boolean isAllowedTransfer(int stopPosition, ConstrainedTransfer tx) {
    // Check in trip pattern whether boarding is possible
    if (!toTrip.pattern().boardingPossibleAt(stopPosition)) {
      return false;
    }
    // Transfer is allowed if no constrained transfer exist
    if (tx == null) {
      return true;
    }
    return !tx.getTransferConstraint().isNotAllowed();
  }
}

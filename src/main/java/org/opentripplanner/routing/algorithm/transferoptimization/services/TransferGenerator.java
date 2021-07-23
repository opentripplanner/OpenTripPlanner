package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * This class is responsible for finding all possible transfers between two trips. The configured
 * slack should be respected, and the transfers found here should be equivalent to the transfers
 * explored during routing (in Raptor).
 * <p>
 * This class also filters away transfers which cannot be used due to time constraints. For example,
 * if a transfer point is before the the earliest possible boarding or after the latest possible
 * arrival.
 * <p>
 * This service does NOT combine transfers between various trips to form full paths. There are
 * potentially millions of permutations, so we do that later when we can prune the result.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TransferGenerator<T extends RaptorTripSchedule> {

  private final TransferServiceAdaptor<T> guaranteedTransferServiceAdaptor;
  private final RaptorSlackProvider slackProvider;
  private final RaptorTransitDataProvider<T> stdTransfers;

  private T fromTrip;
  private T toTrip;

  public TransferGenerator(
      TransferServiceAdaptor<T> guaranteedTransferServiceAdaptor,
      RaptorSlackProvider slackProvider,
      RaptorTransitDataProvider<T> stdTransfers
  ) {
    this.guaranteedTransferServiceAdaptor = guaranteedTransferServiceAdaptor;
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


  /* private methods */

  private List<TripToTripTransfer<T>> findTransfers(
          T fromTrip,
          StopTime fromTripDeparture,
          T toTrip
  ) {
    this.fromTrip = fromTrip;
    this.toTrip = toTrip;

    int firstStopPos = 1 + fromTrip.findDepartureStopPosition(
        fromTripDeparture.time(),
        fromTripDeparture.stop()
    );
    return  findAllTransfers(firstStopPos);
  }

  private List<TripToTripTransfer<T>> findAllTransfers(int stopPos) {

    final List<TripToTripTransfer<T>> result = new ArrayList<>();

    while (stopPos < fromTrip.pattern().numberOfStopsInPattern()) {
      var from = TripStopTime.arrival(fromTrip, stopPos);

      // First add high priority transfers
      result.addAll(transferFromSameStop(from));
      result.addAll(findStandardTransfers(from));

      ++stopPos;
    }
    return result;
  }

  private Collection<TripToTripTransfer<T>> transferFromSameStop(TripStopTime<T> from) {
    final int stop = from.stop();
    var tx = guaranteedTransferServiceAdaptor.findTransfer(from, toTrip, stop);

    final int earliestDepartureTime = earliestDepartureTime(from.time(), tx);

    final int toTripStopPos = toTrip.findDepartureStopPosition(earliestDepartureTime, stop);

    if(toTripStopPos < 0) { return List.of(); }

    return List.of(
            new TripToTripTransfer<>(
                    from,
                    TripStopTime.departure(toTrip, toTripStopPos),
                    null,
                    tx
            )
    );
  }

  private Collection<? extends TripToTripTransfer<T>> findStandardTransfers(TripStopTime<T> from) {
    final List<TripToTripTransfer<T>> result = new ArrayList<>();
    Iterator<? extends RaptorTransfer> transfers = stdTransfers.getTransfers(from.stop());

    while (transfers.hasNext()) {
      var it = transfers.next();
      int toStop = it.stop();

      var tx = guaranteedTransferServiceAdaptor.findTransfer(from, toTrip, toStop);

      int earliestDepartureTime = earliestDepartureTime(from.time(), it.durationInSeconds(), tx);
      int toTripStopPos = toTrip.findDepartureStopPosition(earliestDepartureTime, toStop);

      if(toTripStopPos < 0) { continue; }

      var to = TripStopTime.departure(toTrip, toTripStopPos);

      boolean alightingPossible = from.trip().pattern().alightingPossibleAt(from.stopPosition());
      boolean boardingPossible = to.trip().pattern().boardingPossibleAt(to.stopPosition());

      if (boardingPossible && alightingPossible) {
        result.add(new TripToTripTransfer<>(from, to, it, tx));
      }
    }
    return result;
  }

  private int earliestDepartureTime(int  fromTime, int transferDurationInSeconds, @Nullable Transfer tx) {
    return earliestDepartureTime(fromTime, tx) + transferDurationInSeconds;
  }

  private int earliestDepartureTime(int  fromTime, @Nullable Transfer tx) {
    return tx==null || tx.includeSlack()
            ? fromTime + slackProvider.transitSlack(fromTrip.pattern())
            : fromTime;
  }

  @Nonnull
  private StopTime getFromStopTime(final TransitPathLeg<T> leg) {
    return StopTime.stopTime(leg.fromStop(), leg.fromTime());
  }

  @Nonnull
  private TripStopTime<T> findMinimumToStopTime(List<TripToTripTransfer<T>> transfers) {
    return transfers.stream()
            .map(TripToTripTransfer::to)
            .min(Comparator.comparingInt(TripStopTime::time))
            .orElseThrow();
  }

  private void removeTransfersAfterLatestStopArrival(
          int latestArrivalStopPos,
          List<List<TripToTripTransfer<T>>> result
  ) {
    int nextLatestArrivalStopPos = 0;

    for (int i = result.size()-1; i >=0; --i) {
      List<TripToTripTransfer<T>> filteredTransfers = new ArrayList<>();

      for (TripToTripTransfer<T> tx : result.get(i)) {
        if(tx.to().stopPosition() < latestArrivalStopPos) {
          filteredTransfers.add(tx);
          nextLatestArrivalStopPos = Math.max(tx.from().stopPosition(), nextLatestArrivalStopPos);
        }
      }
      latestArrivalStopPos = nextLatestArrivalStopPos;
      result.set(i, filteredTransfers);
    }
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }
}

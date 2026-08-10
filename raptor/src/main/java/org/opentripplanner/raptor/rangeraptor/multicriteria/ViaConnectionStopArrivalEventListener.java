package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.view.PathLegType.TRANSFER;
import static org.opentripplanner.raptor.api.view.PathLegType.TRANSIT;
import static org.opentripplanner.utils.collection.ListUtils.requireAtLeastNElements;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.api.request.via.RaptorPassThroughViaConnection;
import org.opentripplanner.raptor.api.request.via.RaptorTransferViaConnection;
import org.opentripplanner.raptor.api.request.via.RaptorVisitStopViaConnection;
import org.opentripplanner.raptor.api.request.via.ViaConnection;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorTripScheduleReference;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;
import org.opentripplanner.utils.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to listen for stop arrivals in one Raptor state and then copy
 * over the arrival event to another state. This is used to chain the Raptor searches
 * together to force the paths through the given via connections.
 * <p>
 * We need to delay updating the next arrival state if the via connection is a transfer.
 * Raptor processes arrivals in phases. If you arrive at a stop by transit, you may continue
 * using a transfer or transit. The transit state is copied over from the first leg state
 * without delay, while the transfer via-leg state must be cached and copied over in the
 * "transfer phase" of the Raptor algorithm. The lifecycle service will notify this class
 * at the right time to publish the transfer arrivals.
 * <p>
 * This event listener is only called for stops which allow alighting at the given stop. Since
 * we can not pick up the pass-through event during the on-board processing due to degrading the
 * performance - we do it here and for the moment does not support pass-through for stops
 * where alighting is forbidden.
 */
public final class ViaConnectionStopArrivalEventListener<T extends RaptorTripSchedule>
  implements ParetoSetEventListener<ArrivalView<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(
    ViaConnectionStopArrivalEventListener.class
  );

  private final McStopArrivalFactory<T> stopArrivalFactory;
  private final List<ViaConnection> connections;
  private final McRangeRaptorWorkerState<T> next;
  private final List<McStopArrival<T>> transfersCache = new ArrayList<>();
  private final Function<T, RaptorTripScheduleReference> tripInfoProvider;

  /**
   * @param publishTransfersEventHandler A callback used to publish via-transfer-connections. This
   *                                     should be done in the same phase as all other transfers
   *                                     processed by the Raptor algorithm.
   */
  private ViaConnectionStopArrivalEventListener(
    McStopArrivalFactory<T> stopArrivalFactory,
    List<ViaConnection> connections,
    McRangeRaptorWorkerState<T> next,
    Consumer<Runnable> publishTransfersEventHandler,
    Function<T, RaptorTripScheduleReference> tripInfoProvider
  ) {
    this.stopArrivalFactory = stopArrivalFactory;
    this.connections = requireAtLeastNElements(connections, 1);
    this.next = next;
    this.tripInfoProvider = tripInfoProvider;
    publishTransfersEventHandler.accept(this::applyTransfers);
  }

  /**
   * Factory method for creating a {@link org.opentripplanner.raptor.util.paretoset.ParetoSet}
   * listener used to copy the state when arriving at a "via point" into the next Raptor "leg".
   */
  public static <T extends RaptorTripSchedule> TIntObjectMap<
    ParetoSetEventListener<ArrivalView<T>>
  > createEventListeners(
    @Nullable ViaConnections viaConnections,
    McStopArrivalFactory<T> stopArrivalFactory,
    McRangeRaptorWorkerState<T> nextSegmentState,
    Consumer<Runnable> onTransitComplete,
    Function<T, RaptorTripScheduleReference> tripInfoProvider
  ) {
    var map = new TIntObjectHashMap<ParetoSetEventListener<ArrivalView<T>>>();
    if (viaConnections == null) {
      return map;
    }
    TIntObjectMap<List<ViaConnection>> connectionsByStop = viaConnections.byFromStop();
    for (int stop : connectionsByStop.keys()) {
      var connections = connectionsByStop.get(stop);
      var listener = new ViaConnectionStopArrivalEventListener<>(
        stopArrivalFactory,
        connections,
        nextSegmentState,
        onTransitComplete,
        tripInfoProvider
      );
      map.put(stop, listener);
    }
    return map;
  }

  private void applyTransfers() {
    for (var arrival : transfersCache) {
      next.addStopArrival(arrival);
    }
    transfersCache.clear();
  }

  @SuppressWarnings("RedundantLabeledSwitchRuleCodeBlock")
  @Override
  public void notifyElementAccepted(ArrivalView<T> newElement) {
    var arrival = (McStopArrival<T>) newElement;

    for (ViaConnection connection : connections) {
      switch (connection) {
        case RaptorPassThroughViaConnection _ -> handlePassThroughViaConnection(arrival);
        case RaptorVisitStopViaConnection visitStop -> {
          continueFromSameStopArrival(arrival, visitStop);
        }
        case RaptorTransferViaConnection transfer -> {
          if (arrival.arrivedOnBoard()) {
            continueWithTransfer(arrival, transfer);
          }
          // Silently ignore arrive-on-foot + via-transfer. Two transfers are
          // not allowed after each other, and we can safely skip it here.
        }
      }
    }
  }

  /// We need to continue pass-through connections, even if better arrivals exist in the
  /// stop arrivals at the given stop - so we ignore the fact that the alighting is rejected.
  @Override
  public void notifyElementRejected(ArrivalView<T> arrival, ArrivalView<T> rejectedByElement) {
    for (ViaConnection connection : connections) {
      if (connection instanceof RaptorPassThroughViaConnection) {
        handlePassThroughViaConnection((McStopArrival<T>) arrival);
      }
    }
  }

  private void handlePassThroughViaConnection(McStopArrival<T> arrival) {
    continueOnSameTripInNextSegment(arrival);
    continueFromSameStopArrivalFromPassThrough(arrival);
  }

  /// @param alightArrival Must be a transit arrival, if not it is ignored.
  @SuppressWarnings("DataFlowIssue")
  private void continueOnSameTripInNextSegment(ArrivalView<T> alightArrival) {
    if (!alightArrival.arrivedBy(TRANSIT)) {
      return;
    }
    var transitPath = alightArrival.transitPath();
    T trip = transitPath.trip();

    var boardStopArrival = alightArrival.previous();

    int boardingStopPos = transitPath.boardStopPosition();

    if (boardingStopPos == -1) {
      logUnexpectedStopPosition("board", trip, boardStopArrival);
      return;
    }

    int startRoutingAtStopPosition = trip.findArrivalStopPosition(
      alightArrival.arrivalTime(),
      alightArrival.stop()
    );

    if (startRoutingAtStopPosition == -1) {
      logUnexpectedStopPosition("alight", trip, alightArrival);
      return;
    }

    var info = tripInfoProvider.apply(trip);
    var boardingConstraint = new RaptorTripScheduleStopPosition(
      info.routeIndex(),
      info.tripScheduleIndex(),
      boardingStopPos
    );
    next.addOnBoardTripArrival(
      boardStopArrival,
      alightArrival.stop(),
      startRoutingAtStopPosition,
      boardingConstraint
    );
  }

  private void continueFromSameStopArrival(
    McStopArrival<T> arrival,
    RaptorVisitStopViaConnection via
  ) {
    int d = via.minimumWaitTime();
    continueFromSameStopArrival(d == 0 ? arrival : arrival.addSlackToArrivalTime(d));
  }

  /// Transit and access arrivals are forwarded to the next segment. Walk-transfer arrivals
  /// are dropped: they do not satisfy the pass-through constraint on their own, and forwarding
  /// them would produce two consecutive transfer legs, which is not representable in a path.
  private void continueFromSameStopArrivalFromPassThrough(McStopArrival<T> arrival) {
    if (!arrival.arrivedBy(TRANSFER)) {
      continueFromSameStopArrival(arrival);
    }
  }

  private void continueFromSameStopArrival(McStopArrival<T> arrival) {
    next.addViaArrival(arrival);
  }

  private void continueWithTransfer(McStopArrival<T> from, RaptorTransferViaConnection via) {
    int arrivalTime = from.arrivalTime() + via.durationInSeconds();
    var to = stopArrivalFactory.createTransferStopArrival(from, via.transfer(), arrivalTime);
    transfersCache.add(to);
  }

  private static void logUnexpectedStopPosition(
    String event,
    RaptorTripSchedule trip,
    ArrivalView<?> stopArrival
  ) {
    LOG.warn(
      "Unexpected {} stop position missing for trip {} at stop {} after {}.",
      event,
      trip.pattern().debugInfo(),
      stopArrival.stop(),
      TimeUtils.timeToStrLong(stopArrival.arrivalTime())
    );
  }
}

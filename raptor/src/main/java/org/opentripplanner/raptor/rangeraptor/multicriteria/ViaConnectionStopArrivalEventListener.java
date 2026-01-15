package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.utils.collection.ListUtils.requireAtLeastNElements;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorViaConnection;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * This class is used to listen for stop arrivals in one raptor state and then copy
 * over the arrival event to another state. This is used to chain the Raptor searches
 * together to force the paths through the given via connections.
 * <p>
 * We need to delay updating the next arrival state if the via connection is a transfer.
 * Raptor process arrivals in phases, if you arrive at a stop by transit, you may continue
 * using a transfer or transit. The transit state is copied over from the first leg state
 * without delay, while the transfer via-leg state must be cached and copied over in the
 * "transfer phase" of the Raptor algorithm. The life-cycle service will notify this class
 * at the right time to publish the transter-arrivals.
 */
public final class ViaConnectionStopArrivalEventListener<T extends RaptorTripSchedule>
  implements ParetoSetEventListener<ArrivalView<T>> {

  private final McStopArrivalFactory<T> stopArrivalFactory;
  private final List<RaptorViaConnection> connections;
  private final McStopArrivals<T> next;
  private final List<McStopArrival<T>> transfersCache = new ArrayList<>();

  /**
   * @param publishTransfersEventHandler A callback used to publish via-transfer-connections. This
   *                                     should be done in the same phase as all other transfers
   *                                     processed by the Raptor algorithm.
   */
  private ViaConnectionStopArrivalEventListener(
    McStopArrivalFactory<T> stopArrivalFactory,
    List<RaptorViaConnection> connections,
    McStopArrivals<T> next,
    Consumer<Runnable> publishTransfersEventHandler
  ) {
    this.stopArrivalFactory = stopArrivalFactory;
    this.connections = requireAtLeastNElements(connections, 1);
    this.next = next;
    publishTransfersEventHandler.accept(this::publishTransfers);
  }

  /**
   * Factory method for creating a {@link org.opentripplanner.raptor.util.paretoset.ParetoSet}
   * listener used to copy the state when arriving at a "via point" into the next Raptor "leg".
   */
  public static <T extends RaptorTripSchedule> List<
    ViaConnectionStopArrivalEventListener<T>
  > createEventListeners(
    @Nullable ViaConnections viaConnections,
    McStopArrivalFactory<T> stopArrivalFactory,
    McStopArrivals<T> nextLegStopArrivals,
    Consumer<Runnable> onTransitComplete
  ) {
    if (viaConnections == null) {
      return List.of();
    }
    var list = new ArrayList<ViaConnectionStopArrivalEventListener<T>>();
    viaConnections
      .byFromStop()
      .forEachEntry((stop, connections) ->
        list.add(
          new ViaConnectionStopArrivalEventListener<>(
            stopArrivalFactory,
            connections,
            nextLegStopArrivals,
            onTransitComplete
          )
        )
      );
    return list;
  }

  private void publishTransfers() {
    for (var arrival : transfersCache) {
      next.addStopArrival(arrival);
    }
    transfersCache.clear();
  }

  int fromStop() {
    return connections.getFirst().fromStop();
  }

  @Override
  public void notifyElementAccepted(ArrivalView<T> newElement) {
    for (RaptorViaConnection c : connections) {
      var e = (McStopArrival<T>) newElement;
      var n = createViaStopArrival(e, c);
      if (n != null) {
        if (c.isTransfer()) {
          transfersCache.add(n);
        } else {
          next.addStopArrival(n);
        }
      }
    }
  }

  @Nullable
  private McStopArrival<T> createViaStopArrival(
    McStopArrival<T> previous,
    RaptorViaConnection viaConnection
  ) {
    if (viaConnection.isSameStop()) {
      if (viaConnection.durationInSeconds() == 0) {
        return previous;
      } else {
        return previous.addSlackToArrivalTime(viaConnection.durationInSeconds());
      }
    } else {
      if (previous.arrivedOnBoard()) {
        return stopArrivalFactory.createTransferStopArrival(
          previous,
          viaConnection.transfer(),
          previous.arrivalTime() + viaConnection.durationInSeconds()
        );
      } else {
        return null;
      }
    }
  }
}

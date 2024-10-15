package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * This class listen to pareto set egress stop arrivals and on accepted transit arrivals make the
 * transfer to the destination.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class CalculateTransferToDestination<T extends RaptorTripSchedule>
  implements ParetoSetEventListener<ArrivalView<T>> {

  private final List<RaptorAccessEgress> egressPaths;
  private final DestinationArrivalPaths<T> destinationArrivals;

  CalculateTransferToDestination(
    List<RaptorAccessEgress> egressPaths,
    DestinationArrivalPaths<T> destinationArrivals
  ) {
    this.egressPaths = egressPaths;
    this.destinationArrivals = destinationArrivals;
  }

  /**
   * When a stop arrival is accepted and we arrived by transit, then add a new destination arrival.
   * <p/>
   * We do not have to handle other events like dropped or rejected.
   *
   * @param newElement the new transit arrival
   */
  @Override
  public void notifyElementAccepted(ArrivalView<T> newElement) {
    switch (newElement.arrivedBy()) {
      case TRANSIT -> addOnBoardStopArrivalToDestination(newElement);
      case TRANSFER -> addOnStreetArrivalToDestination(newElement);
      case ACCESS -> {
        if (newElement.arrivedOnBoard()) {
          addOnBoardStopArrivalToDestination(newElement);
        } else {
          addOnStreetArrivalToDestination(newElement);
        }
      }
      case EGRESS -> throw new IllegalArgumentException();
    }
  }

  private void addOnBoardStopArrivalToDestination(ArrivalView<T> newElement) {
    for (RaptorAccessEgress egress : egressPaths) {
      destinationArrivals.add(newElement, egress);
    }
  }

  private void addOnStreetArrivalToDestination(ArrivalView<T> newElement) {
    for (RaptorAccessEgress egress : egressPaths) {
      if (egress.stopReachedOnBoard()) {
        destinationArrivals.add(newElement, egress);
      }
    }
  }
}

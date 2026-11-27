package org.opentripplanner.transfer.regular;

import java.util.Set;
import org.opentripplanner.core.model.event.EventHandler;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.updater.elevator.ElevatorStatusChanged;

/**
 * Closes the elevators that are out of service in the regular transfers, in the transaction the
 * {@link ElevatorStatusChanged} event is published in. The transfers that depend on a closed
 * elevator use an alternative path without it, generated at graph build, or are removed if there is
 * none.
 * <p>
 * This is the translation between the elevator status and the closable facilities of the regular
 * transfers. It must be registered with the handle of the regular transfer repository, which
 * provides the mutable repository of the transaction.
 */
public class ElevatorStatusChangedHandler
  implements EventHandler<ElevatorStatusChanged, RegularTransferRepository<NearbyStop>>
{

  @Override
  public Class<ElevatorStatusChanged> eventType() {
    return ElevatorStatusChanged.class;
  }

  @Override
  public void handle(
    ElevatorStatusChanged event,
    RegularTransferRepository<NearbyStop> repository
  ) {
    RegularTransferFactory.createRealtimeUpdater(repository).closeFacilities(
      closedFacilityIds(event)
    );
  }

  /**
   * The closable facility ids of the elevators that are out of service. The facility id of an
   * elevator on a transfer path is its elevator id, see {@code ElevatorHopEdge#id()}.
   */
  static Set<String> closedFacilityIds(ElevatorStatusChanged event) {
    return event.outOfServiceElevatorIds();
  }
}

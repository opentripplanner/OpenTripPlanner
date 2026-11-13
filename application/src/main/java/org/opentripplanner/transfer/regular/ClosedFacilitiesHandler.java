package org.opentripplanner.transfer.regular;

import org.opentripplanner.core.model.event.EventHandler;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Closes the facilities of a {@link ClosedFacilitiesChanged} event in the regular transfers, in the
 * transaction the event is published in. The transfers that depend on a closed facility use an
 * alternative path without it, generated at graph build, or are removed if there is none.
 * <p>
 * It must be registered with the handle of the regular transfer repository, which provides the
 * mutable repository of the transaction.
 */
public class ClosedFacilitiesHandler
  implements EventHandler<ClosedFacilitiesChanged, RegularTransferRepository<NearbyStop>>
{

  @Override
  public Class<ClosedFacilitiesChanged> eventType() {
    return ClosedFacilitiesChanged.class;
  }

  @Override
  public void handle(
    ClosedFacilitiesChanged event,
    RegularTransferRepository<NearbyStop> repository
  ) {
    RegularTransferFactory.createRealtimeUpdater(repository).closeFacilities(
      event.closedFacilityIds()
    );
  }
}

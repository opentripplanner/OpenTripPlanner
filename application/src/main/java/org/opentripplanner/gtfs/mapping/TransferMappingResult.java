package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import org.opentripplanner.transfer.constrained.model.PendingConstrainedTransfer;

public record TransferMappingResult(
  Collection<PendingConstrainedTransfer> pendingConstrainedTransfers,
  Collection<StaySeatedNotAllowed> staySeatedNotAllowed
) {}

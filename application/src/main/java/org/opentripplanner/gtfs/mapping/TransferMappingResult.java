package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;

public record TransferMappingResult(
  Collection<ConstrainedTransfer> constrainedTransfers,
  Collection<StaySeatedNotAllowed> staySeatedNotAllowed
) {}

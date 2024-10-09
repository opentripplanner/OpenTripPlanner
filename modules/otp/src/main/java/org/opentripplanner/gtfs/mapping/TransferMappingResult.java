package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import org.opentripplanner.model.transfer.ConstrainedTransfer;

public record TransferMappingResult(
  Collection<ConstrainedTransfer> constrainedTransfers,
  Collection<StaySeatedNotAllowed> staySeatedNotAllowed
) {}

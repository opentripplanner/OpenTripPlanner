package org.opentripplanner.netex.mapping;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

class StopTransferPriorityMapper {

  @Nullable
  static StopTransferPriority mapToDomain(InterchangeWeightingEnumeration value) {
    if (value == null) {
      return null;
    }

    switch (value) {
      case NO_INTERCHANGE:
        return StopTransferPriority.DISCOURAGED;
      case INTERCHANGE_ALLOWED:
        return StopTransferPriority.ALLOWED;
      case PREFERRED_INTERCHANGE:
        return StopTransferPriority.PREFERRED;
      case RECOMMENDED_INTERCHANGE:
        return StopTransferPriority.RECOMMENDED;
    }
    throw new IllegalArgumentException("Unsupported interchange weight: " + value);
  }
}

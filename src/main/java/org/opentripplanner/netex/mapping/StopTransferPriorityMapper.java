package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.StopTransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

import javax.annotation.Nullable;

class StopTransferPriorityMapper {

    @Nullable
    static StopTransferPriority mapToDomain(InterchangeWeightingEnumeration value) {
        if(value == null) { return null; }

        switch (value) {
            case NO_INTERCHANGE: return StopTransferPriority.DISCOURAGED;
            case INTERCHANGE_ALLOWED: return StopTransferPriority.ALLOWED;
            case PREFERRED_INTERCHANGE: return StopTransferPriority.PREFERRED;
            case RECOMMENDED_INTERCHANGE: return StopTransferPriority.RECOMMENDED;
        }
        throw new IllegalArgumentException("Unsupported interchange weight: " + value);
    }
}

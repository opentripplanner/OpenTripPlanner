package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.TransferCostPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

public class TransferCostPriorityMapper {

    public static TransferCostPriority mapToDomain(InterchangeWeightingEnumeration value) {
        if(value == null) { return null; }

        switch (value) {
            case NO_INTERCHANGE: return TransferCostPriority.FORBIDDEN;
            case INTERCHANGE_ALLOWED: return TransferCostPriority.ALLOWED;
            case PREFERRED_INTERCHANGE: return TransferCostPriority.PREFERRED;
            case RECOMMENDED_INTERCHANGE: return TransferCostPriority.RECOMMENDED;
        }
        throw new IllegalArgumentException("Unsupported interchange weight: " + value);
    }
}

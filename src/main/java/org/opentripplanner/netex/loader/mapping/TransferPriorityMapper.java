package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.TransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;

public class TransferPriorityMapper {

    public static TransferPriority mapToDomain(InterchangeWeightingEnumeration value) {
        if(value == null) { return null; }

        switch (value) {
            case NO_INTERCHANGE: return TransferPriority.DISCOURAGED;
            case INTERCHANGE_ALLOWED: return TransferPriority.ALLOWED;
            case PREFERRED_INTERCHANGE: return TransferPriority.PREFERRED;
            case RECOMMENDED_INTERCHANGE: return TransferPriority.RECOMMENDED;
        }
        throw new IllegalArgumentException("Unsupported interchange weight: " + value);
    }
}

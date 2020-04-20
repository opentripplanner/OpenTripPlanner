package org.opentripplanner.model;


/**
 * Categorize how suitable a Station or Transfer is it for transfer. The values arrange
 * stops from transfer FORBIDDEN to PREFERRED. First of all this should encourage
 * transfers between two trips/routes to happen at the best possible location if there
 * are multiple stop to choose from. But, it will also apply to score a journey over
 * another one, if one of the journeys score better on the transfers, and they have the
 * same cost.
 */
public enum TransferCostPriority {
    /** Block transfers from/to this stop. */
    FORBIDDEN,

    /** Allow transfers from/to this stop. This is the default. */
    ALLOWED,

    /** Recommended stop place. */
    RECOMMENDED,

    /** Preferred place to transfer, strongly recommended. */
    PREFERRED;
}

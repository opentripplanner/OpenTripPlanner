package org.opentripplanner.ext.legacygraphqlapi.mapping;

import org.opentripplanner.routing.alertpatch.AlertCause;

/**
 * Class for mapping {@link AlertCause} to LegacyGraphQL API cause (GTFS RT).
 */
public class LegacyGraphQLCauseMapper {

    /**
     * Returns LegacyGraphQL API string counter part for internal {@link AlertCause} enum. Defaults
     * to returning UNKNOWN_CAUSE.
     */
    public static String getLegacyGraphQLCause(AlertCause cause) {
        if (cause == null) {
            return "UNKNOWN_CAUSE";
        }
        switch (cause) {
            case OTHER_CAUSE:
            case TECHNICAL_PROBLEM:
            case STRIKE:
            case DEMONSTRATION:
            case ACCIDENT:
            case HOLIDAY:
            case WEATHER:
            case MAINTENANCE:
            case CONSTRUCTION:
            case POLICE_ACTIVITY:
            case MEDICAL_EMERGENCY:
                return cause.name();
            case UNKNOWN_CAUSE:
            default: {
                return "UNKNOWN_CAUSE";
            }
        }
    }
}

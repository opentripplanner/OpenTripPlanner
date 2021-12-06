package org.opentripplanner.ext.legacygraphqlapi.mapping;

import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Class for mapping {@link AlertSeverity} to LegacyGraphQL API severity (GTFS RT).
 */
public class LegacyGraphQLSeverityMapper {

    /**
     * Returns LegacyGraphQL API string counter part for internal {@link AlertSeverity} enum.
     * Defaults to returning UNKNOWN_SEVERITY.
     */
    public static String getLegacyGraphQLSeverity(AlertSeverity severity) {
        if (severity == null) {
            return "UNKNOWN_SEVERITY";
        }
        switch (severity) {
            case INFO:
                return "INFO";
            case VERY_SLIGHT:
            case SLIGHT:
            case WARNING:
                return "WARNING";
            case VERY_SEVERE:
            case SEVERE:
                return "SEVERE";
            case UNKNOWN_SEVERITY:
            case UNDEFINED:
            default: {
                return "UNKNOWN_SEVERITY";
            }
        }
    }
}

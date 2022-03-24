package org.opentripplanner.ext.legacygraphqlapi.mapping;

import org.opentripplanner.routing.alertpatch.AlertEffect;

/**
 * Class for mapping {@link AlertEffect} to LegacyGraphQL API effect (GTFS RT).
 */
public class LegacyGraphQLEffectMapper {

    /**
     * Returns LegacyGraphQL API string counter part for internal {@link AlertEffect} enum. Defaults
     * to returning UNKNOWN_Effect.
     */
    public static String getLegacyGraphQLEffect(AlertEffect effect) {
        if (effect == null) {
            return "UNKNOWN_EFFECT";
        }
        switch (effect) {
            case NO_SERVICE:
            case REDUCED_SERVICE:
            case SIGNIFICANT_DELAYS:
            case DETOUR:
            case ADDITIONAL_SERVICE:
            case MODIFIED_SERVICE:
            case OTHER_EFFECT:
            case STOP_MOVED:
            case NO_EFFECT:
                return effect.name();
            case UNKNOWN_EFFECT:
            default: {
                return "UNKNOWN_EFFECT";
            }
        }
    }
}

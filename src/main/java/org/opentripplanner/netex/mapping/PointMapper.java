package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

import java.util.function.Consumer;

/**
 * Help mapping a location and verifying a correct input (prevent NPE).
 */
class PointMapper {
    /**
     * This is a utility mapper with static methods, the constructor is private to prevent
     * creating new instances of this class.
     */
    private PointMapper() {}

    /**
     * This utility method check if the given {@code point} or one of its sub elements is
     * {@code null} before passing the location to the given {@code locationHandler}.
     *
     * @return true if the handler is successfully invoked with a location, {@code false} if
     *         any of the required data elements are {@code null}.
     */
    static boolean verifyPointAndProcessCoordinate(
            SimplePoint_VersionStructure point,
            Consumer<LocationStructure> locationHandler
    ) {
        // Check and warn if point is missing, return false
        if (
                point == null
                || point.getLocation() == null
                || point.getLocation().getLongitude() == null
                || point.getLocation().getLatitude() == null
        ) {
            return false;
        }
        // Location is safe to process
        locationHandler.accept(point.getLocation());
        return true;
    }
}

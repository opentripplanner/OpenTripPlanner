package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapper {

    /**
     * This utility method check if the given {@code point} or one of its sub elements is
     * {@code null} before passing the location to the given {@code locationHandler}.
     *
     * @return true if the handler is successfully invoked with a location, {@code false} if
     *         any of the required data elements are {@code null}.
     */
    static WgsCoordinate mapToDomain(SimplePoint_VersionStructure point) {
        if(point == null || point.getLocation() == null) { return null; }
        LocationStructure loc = point.getLocation();

        // This should not happen
        if (loc.getLongitude() == null || loc.getLatitude() == null) {
            throw new IllegalArgumentException("Coordinate is not valid: " + loc);
        }
        // Location is safe to process
        return new WgsCoordinate(loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
    }
}

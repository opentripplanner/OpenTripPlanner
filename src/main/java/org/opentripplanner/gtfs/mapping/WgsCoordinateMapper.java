package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.model.WgsCoordinate;

class WgsCoordinateMapper {
    static WgsCoordinate mapToDomain(Stop stop) {
        if(stop.isLatSet() && stop.isLonSet()) {
            return new WgsCoordinate(stop.getLat(), stop.getLon());
        }
        if(!stop.isLatSet() && !stop.isLonSet()) {
            return null;
        }
        if(stop.isLatSet()) {
            throw new IllegalArgumentException(
                    "Latitude is set, but not longitude for stop: " + stop
            );
        }
        throw new IllegalArgumentException(
                "Longitude is set, but not latitude for stop: " + stop
        );
    }
}

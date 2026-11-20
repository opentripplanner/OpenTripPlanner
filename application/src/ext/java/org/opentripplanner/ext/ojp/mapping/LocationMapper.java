package org.opentripplanner.ext.ojp.mapping;

import org.opentripplanner.ojp.schema.siri.LocationStructure;
import org.opentripplanner.street.geometry.WgsCoordinate;

class LocationMapper {

  static LocationStructure map(WgsCoordinate coord) {
    return new LocationStructure().withLatitude(coord.latitude()).withLongitude(coord.longitude());
  }
}

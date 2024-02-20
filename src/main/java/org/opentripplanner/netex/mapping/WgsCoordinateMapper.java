package org.opentripplanner.netex.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapper {

  /**
   * This utility method check if the given {@code point} or one of its sub elements is {@code null}
   * before passing the location to the given {@code locationHandler}.
   *
   * @return true if the handler is successfully invoked with a location, {@code false} if any of
   * the required data elements are {@code null}.
   */
  @Nullable
  static WgsCoordinate mapToDomain(SimplePoint_VersionStructure point) {
    if (point == null || point.getLocation() == null) {
      return null;
    }
    LocationStructure loc = point.getLocation();

    if (loc.getLongitude() == null || loc.getLatitude() == null) {
      if (loc.getPos() == null) {
        throw new IllegalArgumentException("Coordinate is not valid: " + loc);
      }

      List<Double> coordinates = loc.getPos().getValue();
      if (coordinates.size() != 2) {
        throw new IllegalArgumentException("Coordinate is not valid: " + loc);
      }

      return new WgsCoordinate(coordinates.get(1), coordinates.get(0));
    }

    // Location is safe to process
    return new WgsCoordinate(loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
  }
}

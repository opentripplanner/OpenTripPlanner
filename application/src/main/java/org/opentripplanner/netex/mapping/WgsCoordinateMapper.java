package org.opentripplanner.netex.mapping;

import javax.annotation.Nullable;
import net.opengis.gml._3.DirectPositionType;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapper {

  /**
   * Maps a NeTEx {@code SimplePoint_VersionStructure} to a {@code WgsCoordinate}.
   */
  @Nullable
  static WgsCoordinate mapToDomain(SimplePoint_VersionStructure point) {
    if (point == null || point.getLocation() == null) {
      return null;
    }
    LocationStructure loc = point.getLocation();

    final DirectPositionType pos = loc.getPos();
    if (loc.getLongitude() != null && loc.getLatitude() != null) {
      return new WgsCoordinate(loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
    }
    // seen in Italian NeTEx data
    else if (pos != null && (pos.getValue().size() == 2 || pos.getValue().size() == 3)) {
      var coordinates = pos.getValue();
      return new WgsCoordinate(coordinates.getFirst(), coordinates.get(1));
    } else {
      throw new IllegalArgumentException("Coordinate is not valid: " + loc);
    }
  }
}

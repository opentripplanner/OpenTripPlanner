package org.opentripplanner.netex.mapping;

import javax.annotation.Nullable;
import net.opengis.gml._3.DirectPositionType;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.SimplePoint_VersionStructure;

class WgsCoordinateMapper {

  /**
   * Maps a NeTEx {@code SimplePoint_VersionStructure} to a {@code WgsCoordinate}.
   *
   * @return {@code null} if the point or its location is missing.
   * @throws InvalidCoordinateException if the location is present but cannot be mapped to a valid
   *                                    WGS84 coordinate, e.g. one of latitude/longitude is missing
   *                                    or a value is out of range. The caller is expected to
   *                                    report a data import issue and skip the entity - invalid
   *                                    input data must not abort the graph build.
   */
  @Nullable
  static WgsCoordinate mapToDomain(SimplePoint_VersionStructure point)
    throws InvalidCoordinateException {
    if (point == null || point.getLocation() == null) {
      return null;
    }
    LocationStructure loc = point.getLocation();

    final DirectPositionType pos = loc.getPos();
    try {
      if (loc.getLongitude() != null && loc.getLatitude() != null) {
        return new WgsCoordinate(loc.getLatitude().doubleValue(), loc.getLongitude().doubleValue());
        // seen in Italian NeTEx data - it's not clear if EPIP-compliant or not
      } else if (pos != null && (pos.getValue().size() == 2 || pos.getValue().size() == 3)) {
        var coordinates = pos.getValue();
        return new WgsCoordinate(coordinates.getFirst(), coordinates.get(1));
      }
    } catch (IllegalArgumentException e) {
      // Thrown by WgsCoordinate when latitude/longitude is out of range
      throw new InvalidCoordinateException(e.getMessage());
    }
    throw new InvalidCoordinateException("Coordinate is not valid: " + loc);
  }

  /**
   * Signals that a NeTEx location is present, but does not represent a valid WGS84 coordinate.
   * This is a checked exception to force every mapper to handle invalid input data explicitly.
   */
  static class InvalidCoordinateException extends Exception {

    InvalidCoordinateException(String message) {
      super(message);
    }
  }
}

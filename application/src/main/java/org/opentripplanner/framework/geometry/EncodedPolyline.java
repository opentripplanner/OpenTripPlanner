package org.opentripplanner.framework.geometry;

import java.io.Serializable;
import org.locationtech.jts.geom.Geometry;

/**
 * A list of coordinates encoded as a string.
 * <p>
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded
 * polyline algorithm format</a>
 */

public record EncodedPolyline(String points, int length) implements Serializable {
  public static EncodedPolyline encode(Geometry geometry) {
    return PolylineEncoder.encodeGeometry(geometry);
  }
}

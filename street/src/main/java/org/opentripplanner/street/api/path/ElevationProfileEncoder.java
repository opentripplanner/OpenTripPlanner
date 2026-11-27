package org.opentripplanner.street.model.path;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.elevation.ElevationProfile;

class ElevationProfileEncoder {

  static ElevationProfile encodeElevationProfileWithNaN(
    Edge edge,
    double distanceOffset,
    double heightOffset
  ) {
    var elevations = encodeElevationProfile(edge, distanceOffset, heightOffset);
    if (elevations.isEmpty()) {
      return ElevationProfile.of()
        .stepYUnknown(distanceOffset)
        .stepYUnknown(distanceOffset + edge.getDistanceMeters())
        .build();
    }
    return elevations;
  }

  private static ElevationProfile encodeElevationProfile(
    Edge edge,
    double distanceOffset,
    double heightOffset
  ) {
    if (!(edge instanceof StreetEdge elevEdge)) {
      return ElevationProfile.empty();
    }
    if (elevEdge.getElevationProfile() == null) {
      return ElevationProfile.empty();
    }

    var out = ElevationProfile.of();
    Coordinate[] coordArr = elevEdge.getElevationProfile().toCoordinateArray();
    for (final Coordinate coordinate : coordArr) {
      out.step(coordinate.x + distanceOffset, coordinate.y + heightOffset);
    }

    return out.build();
  }
}

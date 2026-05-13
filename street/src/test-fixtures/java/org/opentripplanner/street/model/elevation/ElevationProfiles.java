package org.opentripplanner.street.model.elevation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

public class ElevationProfiles {

  /// An elevation profile that exceeds the maximum uphill slope limit of 35%.
  public static final PackedCoordinateSequence.Double STEEP_ELEVATION_PROFILE =
    new PackedCoordinateSequence.Double(
      new Coordinate[] {
        new Coordinate(0, 6972),
        new Coordinate(25, 6985),
        new Coordinate(50, 6967),
      }
    );

  public static final PackedCoordinateSequence.Double STEEP_DOWNHILL_PROFILE =
    new PackedCoordinateSequence.Double(
      new Coordinate[] { new Coordinate(0, 1000), new Coordinate(25, 800), new Coordinate(50, 700) }
    );
}

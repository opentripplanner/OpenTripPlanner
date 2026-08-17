package org.opentripplanner.graph_builder.module.geometry;

import java.util.Arrays;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

/**
 * A key for deduplicating LineStrings by their coordinates.
 * <p>
 * JTS's {@link LineString#equals(Object)} and {@link LineString#hashCode()} are unsuitable for
 * this: equals does an exact structural comparison (fine), but hashCode is derived only from the
 * geometry's envelope (bounding box), so unrelated LineStrings that happen to share a bounding
 * box collide constantly, defeating the purpose of a hash-based cache.
 */
final class LineStringKey {

  private final double[] ordinates;

  LineStringKey(LineString lineString) {
    CoordinateSequence sequence = lineString.getCoordinateSequence();
    ordinates = new double[sequence.size() * 2];
    for (int i = 0; i < sequence.size(); i++) {
      ordinates[i * 2] = sequence.getX(i);
      ordinates[i * 2 + 1] = sequence.getY(i);
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof LineStringKey other && Arrays.equals(ordinates, other.ordinates);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(ordinates);
  }
}

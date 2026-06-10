package org.opentripplanner.street.geometry;

import java.io.Serializable;
import java.util.List;
import org.locationtech.jts.geom.LineString;

/**
 * An ordered sequence of {@link CompactLineString} members that are assumed to join end-to-end (the
 * last coordinate of one equals the first coordinate of the next), together with a precomputed
 * cumulative arc-length table indexed by vertex position.
 * <p>
 * "Vertex position" is the index of the boundary <i>between</i> two consecutive members:
 * <ul>
 *   <li>position {@code 0} is the start of member {@code 0}</li>
 *   <li>position {@code i} (for {@code 0 < i < size()}) is the shared seam between member
 *       {@code i-1} and member {@code i}</li>
 *   <li>position {@code size()} is the end of the last member</li>
 * </ul>
 * <p>
 */
public final class CompactLineStringSequence implements Serializable {

  private final CompactLineString[] geometries;
  private final int[] cumulativeDistanceMeters;

  private CompactLineStringSequence(
    CompactLineString[] geometries,
    int[] cumulativeDistanceMeters
  ) {
    this.geometries = geometries;
    this.cumulativeDistanceMeters = cumulativeDistanceMeters;
  }

  /**
   * Compact each line string and store the supplied cumulative arc-length table. The caller is
   * responsible for choosing how the cumulative distances are measured (e.g. planar sum vs.
   * haversine) and must pass an array of length {@code geometries.size() + 1} where entry 0 is 0
   * and each subsequent entry is the meter distance from the start of the sequence up to that
   * vertex position.
   */
  public static CompactLineStringSequence of(
    List<LineString> geometries,
    int[] cumulativeDistanceMeters
  ) {
    if (cumulativeDistanceMeters.length != geometries.size() + 1) {
      throw new IllegalArgumentException(
        "cumulativeDistanceMeters length (%d) must equal geometries.size() + 1 (%d)".formatted(
          cumulativeDistanceMeters.length,
          geometries.size() + 1
        )
      );
    }
    if (cumulativeDistanceMeters[0] != 0) {
      throw new IllegalArgumentException(
        "cumulativeDistanceMeters[0] must be 0 (was %d)".formatted(cumulativeDistanceMeters[0])
      );
    }
    for (int i = 1; i < cumulativeDistanceMeters.length; i++) {
      if (cumulativeDistanceMeters[i] < cumulativeDistanceMeters[i - 1]) {
        throw new IllegalArgumentException(
          "cumulativeDistanceMeters must be non-decreasing, but entry %d (%d) < entry %d (%d)".formatted(
            i,
            cumulativeDistanceMeters[i],
            i - 1,
            cumulativeDistanceMeters[i - 1]
          )
        );
      }
    }
    var packed = new CompactLineString[geometries.size()];
    for (int i = 0; i < geometries.size(); i++) {
      packed[i] = CompactLineString.of(geometries.get(i));
    }
    return new CompactLineStringSequence(packed, cumulativeDistanceMeters);
  }

  /** Number of line strings in the sequence. */
  public int size() {
    return geometries.length;
  }

  /** Decode the line string at {@code index}. */
  public LineString get(int index) {
    return geometries[index].toLineString(false);
  }

  /**
   * Arc length in meters along the sequence between two vertex positions. Constant time.
   */
  public int distanceBetween(int fromVertex, int toVertex) {
    return cumulativeDistanceMeters[toVertex] - cumulativeDistanceMeters[fromVertex];
  }

  /**
   * Decode and concatenate the line strings in {@code [fromIndex, toIndexExclusive)} into a single
   * {@link LineString}. The shared seam coordinate between consecutive members is emitted only
   * once. Returns an empty line string when the range is empty or degenerate.
   * <p>
   * Decodes straight into one result buffer rather than materializing an intermediate
   * {@link LineString} per member.
   */
  public LineString concatenate(int fromIndex, int toIndexExclusive) {
    int count = toIndexExclusive - fromIndex;
    if (count <= 0) {
      return GeometryUtils.emptyLineString();
    }
    int totalCoords = 0;
    for (int i = fromIndex; i < toIndexExclusive; i++) {
      totalCoords += geometries[i].coordinateCount();
    }
    // Each seam between two consecutive members repeats one coordinate; emit it only once.
    totalCoords -= (count - 1);
    if (totalCoords <= 0) {
      return GeometryUtils.emptyLineString();
    }
    double[] out = new double[totalCoords * 2];
    int offset = 0;
    for (int i = fromIndex; i < toIndexExclusive; i++) {
      offset = CompactLineString.decodeInto(
        geometries[i].packed(),
        out,
        offset,
        0,
        0,
        i > fromIndex
      );
    }
    return GeometryUtils.makeLineString(out);
  }
}

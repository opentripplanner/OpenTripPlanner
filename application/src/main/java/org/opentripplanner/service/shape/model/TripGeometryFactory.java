package org.opentripplanner.service.shape.model;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Builds a {@link CompactLineStringSequence}, i.e. the per-hop geometries of a trip pattern and the
 * cumulative arc-length table, from plain geometry input. Identical hops (and identical cumulative
 * tables) are deduplicated through the {@link DeduplicatorService}, so they share one instance.
 * <p>
 * Distances are accumulated in {@code double} and rounded to the nearest meter only when writing
 * each entry of the cumulative table. This bounds the rounding error of any
 * {@code distanceBetween(board, alight)} query to at most 1 meter, independent of the length.
 */
public final class TripGeometryFactory {

  private TripGeometryFactory() {}

  /**
   * Build a sequence from explicit hop geometries (e.g. from GTFS shapes). Each hop's length is
   * the sum of the distances between its consecutive coordinates, see
   * {@link GeometryUtils#sumDistances}.
   */
  public static CompactLineStringSequence fromLineStrings(
    List<LineString> hopGeometries,
    DeduplicatorService deduplicator
  ) {
    double[] cumulativeMetersDouble = new double[hopGeometries.size() + 1];
    ArrayList<CompactLineString> hops = new ArrayList<CompactLineString>(hopGeometries.size());
    for (int i = 0; i < hopGeometries.size(); i++) {
      LineString hop = hopGeometries.get(i);
      cumulativeMetersDouble[i + 1] =
        cumulativeMetersDouble[i] + GeometryUtils.sumDistances(hop.getCoordinateSequence());
      hops.add(deduplicator.deduplicateObject(CompactLineString.class, CompactLineString.of(hop)));
    }
    return build(hops, cumulativeMetersDouble, deduplicator);
  }

  /**
   * Build a sequence with a straight line between each pair of consecutive stops. Each hop's
   * length is measured with {@link SphericalDistanceLibrary} (haversine).
   */
  public static CompactLineStringSequence straightLines(
    List<WgsCoordinate> stops,
    DeduplicatorService deduplicator
  ) {
    int expectedHops = Math.max(stops.size() - 1, 0);
    double[] cumulativeMetersDouble = new double[expectedHops + 1];
    ArrayList<CompactLineString> hops = new ArrayList<CompactLineString>(expectedHops);
    for (int i = 0; i < expectedHops; i++) {
      WgsCoordinate from = stops.get(i);
      WgsCoordinate to = stops.get(i + 1);
      LineString hop = GeometryUtils.makeLineString(from, to);
      cumulativeMetersDouble[i + 1] =
        cumulativeMetersDouble[i] +
        SphericalDistanceLibrary.distance(
          from.latitude(),
          from.longitude(),
          to.latitude(),
          to.longitude()
        );
      hops.add(deduplicator.deduplicateObject(CompactLineString.class, CompactLineString.of(hop)));
    }
    return build(hops, cumulativeMetersDouble, deduplicator);
  }

  private static CompactLineStringSequence build(
    List<CompactLineString> hops,
    double[] cumulativeMetersDouble,
    DeduplicatorService deduplicator
  ) {
    int cumulativeLength = cumulativeMetersDouble.length;
    int[] cumulativeMeters = new int[cumulativeLength];
    for (int i = 0; i < cumulativeLength; i++) {
      cumulativeMeters[i] = (int) Math.round(cumulativeMetersDouble[i]);
    }
    return CompactLineStringSequence.of(hops, deduplicator.deduplicateIntArray(cumulativeMeters));
  }
}

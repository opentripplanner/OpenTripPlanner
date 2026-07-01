package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

/**
 * Behavioral tests for {@link HashGridSpatialIndex#queryAlongLineStrings(java.util.Collection)}.
 *
 * <p>Uses a 1.0×1.0 bin size so coordinates map predictably to bins: a point at {@code (n, m)}
 * with integer {@code n,m} lands in bin {@code (n, m)} via {@code Math.round}. False positives
 * (items in the same bin as a segment's bounding box but not exactly on the segment) are
 * documented behavior of the index — these tests assert presence/absence at bin granularity.
 */
class HashGridSpatialIndexQueryAlongLineStringsTest {

  private static final double BIN_SIZE = 1.0;
  private static final GeometryFactory GF = GeometryUtils.getGeometryFactory();

  private HashGridSpatialIndex<String> index;

  @BeforeEach
  void setUp() {
    index = new HashGridSpatialIndex<>(BIN_SIZE, BIN_SIZE);
  }

  @Test
  void emptyLineStringCollectionReturnsEmpty() {
    insertPoint(1.0, 1.0, "A");
    var result = index.queryAlongLineStrings(List.of());
    assertTrue(result.isEmpty());
  }

  @Test
  void emptyIndexReturnsEmpty() {
    var ls = line(1.0, 1.0, 5.0, 5.0);
    var result = index.queryAlongLineStrings(List.of(ls));
    assertTrue(result.isEmpty());
  }

  @Test
  void singleSegmentReturnsItemsInTouchedBins() {
    insertPoint(1.0, 1.0, "A");
    insertPoint(2.0, 2.0, "B");
    insertPoint(50.0, 50.0, "FAR");

    var ls = line(1.0, 1.0, 2.0, 2.0);
    var result = index.queryAlongLineStrings(List.of(ls));

    assertTrue(result.contains("A"));
    assertTrue(result.contains("B"));
    assertFalse(result.contains("FAR"));
  }

  @Test
  void itemFarFromLineStringIsExcluded() {
    insertPoint(1.0, 1.0, "NEAR");
    insertPoint(50.0, 50.0, "FAR");

    var ls = line(0.0, 0.0, 2.0, 2.0);
    var result = index.queryAlongLineStrings(List.of(ls));

    assertTrue(result.contains("NEAR"));
    assertFalse(result.contains("FAR"));
  }

  @Test
  void multiSegmentLineStringReturnsItemsAlongAnySegment() {
    insertPoint(1.0, 1.0, "A");
    insertPoint(5.0, 5.0, "B");
    insertPoint(9.0, 1.0, "C");
    insertPoint(50.0, 50.0, "FAR");

    // Path: (1,1) → (5,5) → (9,1)
    var ls = line(1.0, 1.0, 5.0, 5.0, 9.0, 1.0);
    var result = index.queryAlongLineStrings(List.of(ls));

    assertTrue(result.contains("A"));
    assertTrue(result.contains("B"));
    assertTrue(result.contains("C"));
    assertFalse(result.contains("FAR"));
  }

  @Test
  void multipleLineStringsReturnUnion() {
    insertPoint(1.0, 1.0, "A");
    insertPoint(10.0, 10.0, "B");
    insertPoint(50.0, 50.0, "FAR");

    var ls1 = line(1.0, 1.0, 2.0, 2.0);
    var ls2 = line(9.0, 9.0, 10.0, 10.0);
    var result = index.queryAlongLineStrings(List.of(ls1, ls2));

    assertTrue(result.contains("A"));
    assertTrue(result.contains("B"));
    assertFalse(result.contains("FAR"));
  }

  @Test
  void itemSpreadAcrossMultipleBinsIsReturnedOnce() {
    // Insert via LineString geometry — the item lands in every bin the line touches.
    var spreadLs = line(1.0, 1.0, 5.0, 5.0);
    index.insert(spreadLs, "SPREAD");

    // Query path overlaps all of those bins.
    var queryLs = line(0.0, 0.0, 6.0, 6.0);
    var result = index.queryAlongLineStrings(List.of(queryLs));

    assertEquals(1, result.size(), "Set semantics should deduplicate the spread item");
    assertTrue(result.contains("SPREAD"));
  }

  @Test
  void deduplicatesAcrossOverlappingLineStrings() {
    insertPoint(1.0, 1.0, "A");

    // Two overlapping query line strings both touching A's bin.
    var ls1 = line(0.0, 0.0, 2.0, 2.0);
    var ls2 = line(0.5, 0.5, 1.5, 1.5);
    var result = index.queryAlongLineStrings(List.of(ls1, ls2));

    assertEquals(1, result.size());
    assertTrue(result.contains("A"));
  }

  // Helpers --------------------------------------------------------------

  private void insertPoint(double x, double y, String item) {
    var coord = new Coordinate(x, y);
    index.insert(new Envelope(coord, coord), item);
  }

  private static LineString line(double... xy) {
    if (xy.length < 4 || xy.length % 2 != 0) {
      throw new IllegalArgumentException("Need an even number of coordinates, at least 4");
    }
    var coords = new Coordinate[xy.length / 2];
    for (int i = 0; i < coords.length; i++) {
      coords[i] = new Coordinate(xy[i * 2], xy[i * 2 + 1]);
    }
    return GF.createLineString(coords);
  }
}

package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.cursors.LongCursor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

/**
 * Tests for {@link HashGridSpatialIndex#insert(LineString, Object)}, which stores an item only in
 * the bins its segments pass through.
 * <p>
 * Bins are centred on multiples of the bin size, since keys are computed with {@code Math.round}:
 * with a bin size of 1, bin 0 spans [-0.5, 0.5) and bin 1 spans [0.5, 1.5).
 */
class HashGridSpatialIndexInsertLineStringTest {

  private static final GeometryFactory GF = GeometryUtils.getGeometryFactory();

  /** The default bin size of the index, used by the street index. */
  private static final double DEFAULT_X_BIN_SIZE = 0.0035;
  private static final double DEFAULT_Y_BIN_SIZE = 0.005;

  /** Relative tolerance, in bins, used by the geometric oracle. */
  private static final double ORACLE_TOLERANCE = 1e-6;

  private final HashGridSpatialIndex<String> unitIndex = new HashGridSpatialIndex<>(1.0, 1.0);

  @Test
  void pointLikeSegmentIsStoredInOneBin() {
    assertThat(segmentKeys(unitIndex, 2.2, 3.3, 2.2, 3.3)).containsExactly(key(2, 3));
  }

  @Test
  void segmentWithinOneBinIsStoredInOneBin() {
    assertThat(segmentKeys(unitIndex, 1.6, 1.6, 2.4, 2.4)).containsExactly(key(2, 2));
  }

  @Test
  void horizontalSegmentIsStoredInOneRow() {
    assertThat(segmentKeys(unitIndex, 0.1, 2.0, 3.9, 2.0)).containsExactly(
      key(0, 2),
      key(1, 2),
      key(2, 2),
      key(3, 2),
      key(4, 2)
    );
  }

  @Test
  void verticalSegmentIsStoredInOneColumn() {
    assertThat(segmentKeys(unitIndex, -1.0, -1.2, -1.0, 1.2)).containsExactly(
      key(-1, -1),
      key(-1, 0),
      key(-1, 1)
    );
  }

  @Test
  void verticalSegmentOnABinBorderUsesTheBinThatRoundingSelects() {
    // x = 0.5 rounds to 1, so this is also the bin a point query on the segment visits
    assertThat(segmentKeys(unitIndex, 0.5, 0.0, 0.5, 1.0)).containsExactly(key(1, 0), key(1, 1));
  }

  @Test
  void shallowDiagonalSegment() {
    // y goes from 0 to 1 while x goes from 0 to 4: the segment crosses the row border y=0.5 at
    // x=2, which is inside column 2
    assertThat(segmentKeys(unitIndex, 0.0, 0.0, 4.0, 1.0)).containsExactly(
      key(0, 0),
      key(1, 0),
      key(2, 0),
      key(2, 1),
      key(3, 1),
      key(4, 1)
    );
  }

  @Test
  void steepDiagonalSegment() {
    // The same segment as above, mirrored at the diagonal
    assertThat(segmentKeys(unitIndex, 0.0, 0.0, 1.0, 4.0)).containsExactly(
      key(0, 0),
      key(0, 1),
      key(0, 2),
      key(1, 2),
      key(1, 3),
      key(1, 4)
    );
  }

  @Test
  void diagonalWithNegativeSlope() {
    assertThat(segmentKeys(unitIndex, 0.0, 1.0, 4.0, 0.0)).containsExactly(
      key(0, 1),
      key(1, 1),
      key(2, 1),
      key(2, 0),
      key(3, 0),
      key(4, 0)
    );
  }

  @Test
  void diagonalThroughBinCornersIncludesTheBinsTouchedAtTheCorners() {
    // The segment passes exactly through the corners (0.5, 0.5), (1.5, 1.5) and (2.5, 2.5). The
    // neighbouring bins which only touch the segment at a corner are included, which is a few
    // more bins than necessary but never misses one.
    var keys = segmentKeys(unitIndex, 0.0, 0.0, 3.0, 3.0);
    assertThat(keys).containsAtLeast(key(0, 0), key(1, 1), key(2, 2), key(3, 3));
    assertThat(keys).containsNoneOf(key(0, 2), key(2, 0), key(0, 3), key(3, 0), key(1, 3));
    assertOracle(1.0, 1.0, 0.0, 0.0, 3.0, 3.0);
  }

  @Test
  void segmentCrossingTheOrigin() {
    assertThat(segmentKeys(unitIndex, -2.0, -1.0, 2.0, 1.0)).containsExactly(
      key(-2, -1),
      key(-1, -1),
      key(-1, 0),
      key(0, 0),
      key(1, 0),
      key(1, 1),
      key(2, 1)
    );
  }

  @Test
  void binsDoNotDependOnTheDirectionOfTheSegment() {
    var random = new Random(42);
    for (int i = 0; i < 1000; i++) {
      double x1 = random.nextDouble(-20, 20);
      double y1 = random.nextDouble(-20, 20);
      double x2 = random.nextDouble(-20, 20);
      double y2 = random.nextDouble(-20, 20);
      assertEquals(segmentKeys(unitIndex, x1, y1, x2, y2), segmentKeys(unitIndex, x2, y2, x1, y1));
    }
  }

  @Test
  void axisAlignedSegmentsUseTheSameBinsAsTheirEnvelope() {
    var random = new Random(7);
    for (int i = 0; i < 1000; i++) {
      double x1 = random.nextDouble(-20, 20);
      double y1 = random.nextDouble(-20, 20);
      double length = random.nextDouble(0, 20);
      assertEquals(
        envelopeKeys(1.0, 1.0, x1, y1, x1 + length, y1),
        segmentKeys(unitIndex, x1, y1, x1 + length, y1)
      );
      assertEquals(
        envelopeKeys(1.0, 1.0, x1, y1, x1, y1 + length),
        segmentKeys(unitIndex, x1, y1, x1, y1 + length)
      );
    }
  }

  static Stream<Arguments> binSizes() {
    return Stream.of(
      Arguments.of(1.0, 1.0),
      Arguments.of(DEFAULT_X_BIN_SIZE, DEFAULT_Y_BIN_SIZE),
      Arguments.of(0.3, 2.7)
    );
  }

  /**
   * Compare random segments with a geometric oracle: every bin whose rectangle intersects the
   * segment must be used, and no bin whose rectangle does not come close to the segment.
   */
  @ParameterizedTest
  @MethodSource("binSizes")
  void randomSegmentsUseExactlyTheBinsTheyPassThrough(double xBinSize, double yBinSize) {
    var random = new Random(1234);
    for (int i = 0; i < 2000; i++) {
      double x1 = random.nextDouble(-15, 15) * xBinSize;
      double y1 = random.nextDouble(-15, 15) * yBinSize;
      double x2 = x1 + random.nextDouble(-15, 15) * xBinSize;
      double y2 = y1 + random.nextDouble(-15, 15) * yBinSize;
      assertOracle(xBinSize, yBinSize, x1, y1, x2, y2);
    }
  }

  /** Segments with end points exactly on bin borders and corners are the most error-prone. */
  @ParameterizedTest
  @MethodSource("binSizes")
  void segmentsBetweenBinBordersAndCorners(double xBinSize, double yBinSize) {
    var random = new Random(99);
    for (int i = 0; i < 2000; i++) {
      // multiples of half a bin: bin centres, borders and corners
      double x1 = random.nextInt(-20, 20) * 0.5 * xBinSize;
      double y1 = random.nextInt(-20, 20) * 0.5 * yBinSize;
      double x2 = random.nextInt(-20, 20) * 0.5 * xBinSize;
      double y2 = random.nextInt(-20, 20) * 0.5 * yBinSize;
      assertOracle(xBinSize, yBinSize, x1, y1, x2, y2);
    }
  }

  @ParameterizedTest
  @MethodSource("binSizes")
  void nearlyAxisAlignedSegments(double xBinSize, double yBinSize) {
    var random = new Random(5);
    for (int i = 0; i < 1000; i++) {
      double x1 = random.nextDouble(-15, 15) * xBinSize;
      double y1 = random.nextDouble(-15, 15) * yBinSize;
      double length = random.nextDouble(-15, 15);
      double tiny = random.nextDouble(-1e-7, 1e-7);
      assertOracle(xBinSize, yBinSize, x1, y1, x1 + length * xBinSize, y1 + tiny * yBinSize);
      assertOracle(xBinSize, yBinSize, x1, y1, x1 + tiny * xBinSize, y1 + length * yBinSize);
    }
  }

  /**
   * Whatever bins are chosen, a query at any point on the line string must find the item. This is
   * the property the users of the index rely on.
   */
  @ParameterizedTest
  @MethodSource("binSizes")
  void pointQueriesAlongTheLineStringFindTheItem(double xBinSize, double yBinSize) {
    var random = new Random(31);
    for (int i = 0; i < 200; i++) {
      var index = new HashGridSpatialIndex<String>(xBinSize, yBinSize);
      var coordinates = new Coordinate[random.nextInt(2, 6)];
      for (int j = 0; j < coordinates.length; j++) {
        coordinates[j] = new Coordinate(
          random.nextDouble(-10, 10) * xBinSize,
          random.nextDouble(-10, 10) * yBinSize
        );
      }
      var lineString = GF.createLineString(coordinates);
      index.insert(lineString, "line");

      for (int j = 0; j < coordinates.length - 1; j++) {
        var from = coordinates[j];
        var to = coordinates[j + 1];
        for (int k = 0; k <= 100; k++) {
          double t = k / 100.0;
          double x = from.x + (to.x - from.x) * t;
          double y = from.y + (to.y - from.y) * t;
          assertWithMessage("query at (%s, %s) on %s", x, y, lineString)
            .that(index.query(new Envelope(x, x, y, y)))
            .containsExactly("line");
        }
      }
    }
  }

  @Test
  void envelopeQueriesIntersectingTheLineStringFindTheItem() {
    var index = new HashGridSpatialIndex<String>(DEFAULT_X_BIN_SIZE, DEFAULT_Y_BIN_SIZE);
    var lineString = line(10.0, 59.0, 10.1, 59.05, 10.12, 58.9);
    index.insert(lineString, "line");

    var random = new Random(3);
    for (int i = 0; i < 5000; i++) {
      double x = random.nextDouble(9.95, 10.17);
      double y = random.nextDouble(58.85, 59.1);
      var envelope = new Envelope(
        x,
        x + random.nextDouble(0, 0.01),
        y,
        y + random.nextDouble(0, 0.01)
      );
      if (GF.toGeometry(envelope).intersects(lineString)) {
        assertThat(index.query(envelope)).containsExactly("line");
      }
    }
  }

  @Test
  void queryAwayFromADiagonalLineDoesNotFindIt() {
    unitIndex.insert(line(0.0, 0.0, 10.0, 10.0), "diagonal");

    assertThat(unitIndex.query(new Envelope(5, 5, 5, 5))).containsExactly("diagonal");
    // These are inside the envelope of the line, but far from it
    assertThat(unitIndex.query(new Envelope(10, 10, 0, 0))).isEmpty();
    assertThat(unitIndex.query(new Envelope(0, 0, 10, 10))).isEmpty();
    assertThat(unitIndex.query(new Envelope(2, 2, 8, 8))).isEmpty();
  }

  @Test
  void itemIsStoredOnceInABinSharedBySeveralSegments() {
    unitIndex.insert(line(0.0, 0.0, 0.2, 0.3, -0.1, 0.4, 0.3, -0.2, 2.0, 0.0), "zigzag");

    List<String> visited = new ArrayList<>();
    unitIndex.forEachCandidate(new Envelope(0, 0, 0, 0), visited::add);
    assertThat(visited).containsExactly("zigzag");

    // bins (0,0), (1,0) and (2,0)
    assertThat(unitIndex.toString()).contains("3 bins allocated, 1 objs, 3 entries");
  }

  @Test
  void eachLineStringIsStoredInItsOwnBins() {
    unitIndex.insert(line(0.0, 0.0, 4.0, 0.0), "horizontal");
    unitIndex.insert(line(2.0, -2.0, 2.0, 2.0), "vertical");

    assertThat(unitIndex.query(new Envelope(2, 2, 0, 0))).containsExactly("horizontal", "vertical");
    assertThat(unitIndex.query(new Envelope(0, 0, 0, 0))).containsExactly("horizontal");
    assertThat(unitIndex.query(new Envelope(2, 2, 2, 2))).containsExactly("vertical");
    // 5 + 5 bins, sharing (2, 0)
    assertThat(unitIndex.toString()).contains("9 bins allocated, 2 objs, 10 entries");
  }

  @Test
  void removeByEnvelopeRemovesAnItemInsertedAsLineString() {
    var lineString = line(0.0, 0.0, 7.0, 3.0, 9.0, -4.0);
    unitIndex.insert(lineString, "line");
    unitIndex.insert(line(0.0, 0.0, 1.0, 0.0), "other");

    assertThat(unitIndex.remove(lineString.getEnvelopeInternal(), "line")).isTrue();

    assertThat(unitIndex.query(lineString.getEnvelopeInternal())).containsExactly("other");
    assertThat(unitIndex.toString()).contains("2 bins allocated, 1 objs, 2 entries");
  }

  @Test
  void veryLongDiagonalSegmentUsesFewBins() {
    // A bogus GTFS pathway between a stop in the Atlantic and a stop in southern Germany. Its
    // envelope covers about 25 million bins of the default size.
    double x1 = 3.0122;
    double y1 = -11.7636;
    double x2 = 10.2368;
    double y2 = 48.5328;
    var index = new HashGridSpatialIndex<String>(DEFAULT_X_BIN_SIZE, DEFAULT_Y_BIN_SIZE);

    var keys = segmentKeys(index, x1, y1, x2, y2);

    long columns = Math.round(x2 / DEFAULT_X_BIN_SIZE) - Math.round(x1 / DEFAULT_X_BIN_SIZE) + 1;
    long rows = Math.round(y2 / DEFAULT_Y_BIN_SIZE) - Math.round(y1 / DEFAULT_Y_BIN_SIZE) + 1;
    assertThat(columns * rows).isGreaterThan(24_000_000L);
    assertThat((long) keys.size()).isAtMost(columns + rows);

    index.insert(line(x1, y1, x2, y2), "pathway");
    assertThat(index.query(new Envelope(x1, x1, y1, y1))).containsExactly("pathway");
    assertThat(index.query(new Envelope(x2, x2, y2, y2))).containsExactly("pathway");
    double midX = (x1 + x2) / 2;
    double midY = (y1 + y2) / 2;
    assertThat(index.query(new Envelope(midX, midX, midY, midY))).containsExactly("pathway");
    // the corners of the envelope
    assertThat(index.query(new Envelope(x1, x1, y2, y2))).isEmpty();
    assertThat(index.query(new Envelope(x2, x2, y1, y1))).isEmpty();
  }

  @Test
  void segmentWithEndPointOutsideTheValidRangeUsesTheClampedEnvelope() {
    var index = new HashGridSpatialIndex<String>(10.0, 10.0);
    assertEquals(
      envelopeKeys(10.0, 10.0, 160.0, 0.0, 180.0, 40.0),
      segmentKeys(index, 160.0, 0.0, 200.0, 40.0)
    );
    assertEquals(
      envelopeKeys(10.0, 10.0, -175.0, 60.0, -170.0, 90.0),
      segmentKeys(index, -170.0, 60.0, -175.0, 95.0)
    );

    index.insert(line(160.0, 0.0, 200.0, 40.0), "outside");
    // where the line leaves the valid range
    assertThat(index.query(new Envelope(180, 180, 20, 20))).containsExactly("outside");
    // a query outside the valid range is clamped too
    assertThat(index.query(new Envelope(190, 190, 30, 30))).containsExactly("outside");
  }

  @Test
  void segmentAlongTheValidRangeBorder() {
    var index = new HashGridSpatialIndex<String>(10.0, 10.0);
    assertEquals(
      envelopeKeys(10.0, 10.0, 180.0, -90.0, 180.0, 90.0),
      segmentKeys(index, 180.0, -90.0, 180.0, 90.0)
    );
    assertOracle(10.0, 10.0, -180.0, -90.0, 180.0, 90.0);
  }

  @Test
  void emptyLineStringIsNotStored() {
    unitIndex.insert(GF.createLineString(), "empty");
    assertThat(unitIndex.toString()).contains("0 bins allocated, 1 objs, 0 entries");
  }

  // Helpers ---------------------------------------------------------------------------------------

  private static long key(long xKey, long yKey) {
    return HashGridSpatialIndex.binKey(xKey, yKey);
  }

  private static Set<Long> segmentKeys(
    HashGridSpatialIndex<?> index,
    double x1,
    double y1,
    double x2,
    double y2
  ) {
    var keys = new LongHashSet();
    index.collectSegmentBinKeys(x1, y1, x2, y2, keys);
    Set<Long> result = new HashSet<>();
    for (LongCursor c : keys) {
      result.add(c.value);
    }
    return result;
  }

  /** The bins of the envelope of the given points, which is what an envelope insert uses. */
  private static Set<Long> envelopeKeys(
    double xBinSize,
    double yBinSize,
    double x1,
    double y1,
    double x2,
    double y2
  ) {
    Set<Long> result = new HashSet<>();
    for (
      long xKey = binIndex(Math.min(x1, x2), xBinSize);
      xKey <= binIndex(Math.max(x1, x2), xBinSize);
      xKey++
    ) {
      for (
        long yKey = binIndex(Math.min(y1, y2), yBinSize);
        yKey <= binIndex(Math.max(y1, y2), yBinSize);
        yKey++
      ) {
        result.add(key(xKey, yKey));
      }
    }
    return result;
  }

  private static long binIndex(double value, double binSize) {
    return Math.round(value / binSize);
  }

  /**
   * Check the bins of a segment against the geometry of the bins: every bin whose rectangle,
   * shrunk by a tiny tolerance, intersects the segment must be used, and every used bin must
   * intersect the segment when grown by the same tolerance. All bins must be within the envelope
   * of the segment.
   */
  private static void assertOracle(
    double xBinSize,
    double yBinSize,
    double x1,
    double y1,
    double x2,
    double y2
  ) {
    var index = new HashGridSpatialIndex<String>(xBinSize, yBinSize);
    var actual = segmentKeys(index, x1, y1, x2, y2);
    var envelope = envelopeKeys(xBinSize, yBinSize, x1, y1, x2, y2);
    var segment = line(x1, y1, x2, y2);
    String message = "segment (%s, %s) - (%s, %s)".formatted(x1, y1, x2, y2);

    assertWithMessage(message).that(envelope).containsAtLeastElementsIn(actual);

    for (
      long xKey = binIndex(Math.min(x1, x2), xBinSize) - 1;
      xKey <= binIndex(Math.max(x1, x2), xBinSize) + 1;
      xKey++
    ) {
      for (
        long yKey = binIndex(Math.min(y1, y2), yBinSize) - 1;
        yKey <= binIndex(Math.max(y1, y2), yBinSize) + 1;
        yKey++
      ) {
        var shrunk = binRectangle(xKey, yKey, xBinSize, yBinSize, -ORACLE_TOLERANCE);
        var grown = binRectangle(xKey, yKey, xBinSize, yBinSize, ORACLE_TOLERANCE);
        long key = key(xKey, yKey);
        if (GF.toGeometry(shrunk).intersects(segment)) {
          assertWithMessage("%s must use bin (%s, %s)", message, xKey, yKey)
            .that(actual)
            .contains(key);
        }
        if (!GF.toGeometry(grown).intersects(segment)) {
          assertWithMessage("%s must not use bin (%s, %s)", message, xKey, yKey)
            .that(actual)
            .doesNotContain(key);
        }
      }
    }
  }

  private static Envelope binRectangle(
    long xKey,
    long yKey,
    double xBinSize,
    double yBinSize,
    double tolerance
  ) {
    double halfWidth = (0.5 + tolerance) * xBinSize;
    double halfHeight = (0.5 + tolerance) * yBinSize;
    return new Envelope(
      xKey * xBinSize - halfWidth,
      xKey * xBinSize + halfWidth,
      yKey * yBinSize - halfHeight,
      yKey * yBinSize + halfHeight
    );
  }

  private static LineString line(double... xy) {
    var coordinates = new Coordinate[xy.length / 2];
    for (int i = 0; i < coordinates.length; i++) {
      coordinates[i] = new Coordinate(xy[i * 2], xy[i * 2 + 1]);
    }
    return GF.createLineString(coordinates);
  }
}

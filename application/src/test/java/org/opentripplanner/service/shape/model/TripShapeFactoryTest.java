package org.opentripplanner.service.shape.model;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Exercises {@link TripShapeFactory} and the resulting {@link CompactLineStringSequence} (the
 * per-pattern hop geometries + cumulative distance table).
 */
class TripShapeFactoryTest {

  // Three stops roughly 2 km apart along a north-south line at 60° N.
  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A", 60.0000, 10.0000).build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B", 60.0180, 10.0000).build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C", 60.0360, 10.0000).build();
  // A point midway between A and B so the A→B hop is a polyline of three points.
  private static final WgsCoordinate MID_AB = new WgsCoordinate(60.0090, 10.0000);

  private static final List<LineString> HOP_GEOMETRIES = List.of(
    makeLineString(STOP_A.getCoordinate(), MID_AB, STOP_B.getCoordinate()),
    makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate())
  );

  private static final DeduplicatorService NOOP = DeduplicatorService.NOOP;

  private static double exactDistance(StopLocation from, StopLocation to) {
    return SphericalDistanceLibrary.distance(
      from.getCoordinate().latitude(),
      from.getCoordinate().longitude(),
      to.getCoordinate().latitude(),
      to.getCoordinate().longitude()
    );
  }

  @Test
  void distanceBetweenWithHopGeometriesMatchesHaversineWithinOneMeter() {
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);

    // Contract: any distanceBetween(board, alight) is within 1 m of the exact haversine sum.
    double exactAtoB = exactDistance(STOP_A, STOP_B);
    double exactBtoC = exactDistance(STOP_B, STOP_C);

    assertThat((double) subject.distanceBetween(0, 1))
      .isWithin(1.0)
      .of(exactAtoB);
    assertThat((double) subject.distanceBetween(1, 2))
      .isWithin(1.0)
      .of(exactBtoC);
    assertThat((double) subject.distanceBetween(0, 2))
      .isWithin(1.0)
      .of(exactAtoB + exactBtoC);
  }

  @Test
  void distanceBetweenFallsBackToStraightLineWhenHopGeometriesAreNull() {
    var subject = TripShapeFactory.straightLines(
      TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C),
      NOOP
    );

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    assertThat((double) subject.distanceBetween(0, 1))
      .isWithin(1.0)
      .of(exactDistance(STOP_A, STOP_B));
    assertThat((double) subject.distanceBetween(1, 2))
      .isWithin(1.0)
      .of(exactDistance(STOP_B, STOP_C));
  }

  @Test
  void cumulativeRoundingErrorIsBoundedByOneMeter() {
    // Build a long pattern (20 stops). Per-hop rounding would accumulate linearly here;
    // verify that accumulating in double and rounding once per entry keeps the error on the
    // full-pattern leg within 1 m.
    int numberOfStops = 20;
    var stops = new ArrayList<StopLocation>();
    for (int i = 0; i < numberOfStops; i++) {
      // ~111 m spacing (at 60°N, 0.001° latitude ≈ 111 m).
      stops.add(TEST_MODEL.stop("S" + i, 60.0 + i * 0.001, 10.0).build());
    }

    var subject = TripShapeFactory.straightLines(TransitRepositoryForTest.stopPattern(stops), NOOP);

    double exactSum = 0;
    for (int i = 0; i < numberOfStops - 1; i++) {
      exactSum += exactDistance(stops.get(i), stops.get(i + 1));
    }
    assertThat((double) subject.distanceBetween(0, numberOfStops - 1))
      .isWithin(1.0)
      .of(exactSum);
  }

  @Test
  void distanceBetweenIsAdditive() {
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    int partA = subject.distanceBetween(0, 1);
    int partB = subject.distanceBetween(1, 2);
    int whole = subject.distanceBetween(0, 2);

    // Strict equality: differences of cumulative entries cancel exactly.
    assertThat(whole).isEqualTo(partA + partB);
  }

  @Test
  void hopGeometryReturnsCompressedRoundTrip() {
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString hop0 = subject.get(0);
    assertThat(hop0.getNumPoints()).isEqualTo(3);
    assertThat(hop0.getCoordinateN(0).y).isWithin(1e-4).of(STOP_A.getCoordinate().latitude());
    assertThat(hop0.getCoordinateN(0).x).isWithin(1e-4).of(STOP_A.getCoordinate().longitude());
    assertThat(hop0.getCoordinateN(2).y).isWithin(1e-4).of(STOP_B.getCoordinate().latitude());
    assertThat(hop0.getCoordinateN(2).x).isWithin(1e-4).of(STOP_B.getCoordinate().longitude());

    LineString hop1 = subject.get(1);
    assertThat(hop1.getNumPoints()).isEqualTo(2);
    assertThat(hop1.getCoordinateN(0).y).isWithin(1e-4).of(STOP_B.getCoordinate().latitude());
    assertThat(hop1.getCoordinateN(1).y).isWithin(1e-4).of(STOP_C.getCoordinate().latitude());
  }

  @Test
  void hopGeometrySynthesizesStraightLineWhenHopGeometriesAreNull() {
    var subject = TripShapeFactory.straightLines(
      TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C),
      NOOP
    );

    LineString hop0 = subject.get(0);
    assertThat(hop0.getNumPoints()).isEqualTo(2);
    assertThat(hop0.getCoordinateN(0).y).isEqualTo(STOP_A.getCoordinate().latitude());
    assertThat(hop0.getCoordinateN(1).y).isEqualTo(STOP_B.getCoordinate().latitude());
  }

  @Test
  void concatenateConcatenatesHopsInRange() {
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString full = subject.concatenate(0, 2);
    // Hop 0: A, MID, B (3 points). Hop 1: B, C (2 points). Duplicate B at the join is dropped.
    assertThat(full.getNumPoints()).isEqualTo(4);
    assertThat(full.getCoordinateN(0).y).isWithin(1e-4).of(STOP_A.getCoordinate().latitude());
    assertThat(full.getCoordinateN(3).y).isWithin(1e-4).of(STOP_C.getCoordinate().latitude());

    assertThat(subject.concatenate(0, 1).getNumPoints()).isEqualTo(3);
    assertThat(subject.concatenate(1, 2).getNumPoints()).isEqualTo(2);
  }

  @Test
  void concatenateAllReturnsFullPatternWhenShapeAvailable() {
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString full = subject.concatenate();
    assertThat(full).isNotNull();
    assertThat(full.getNumPoints()).isEqualTo(4);
  }

  @Test
  void concatenateAllSynthesizesStraightLineWhenShapeMissing() {
    // Shapeless patterns expose a non-null straight-line geometry made of one 2-point segment per
    // hop, rather than the historical null sentinel.
    var subject = TripShapeFactory.straightLines(
      TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C),
      NOOP
    );

    LineString full = subject.concatenate();
    assertThat(full).isNotNull();
    // Two hops of 2 points each, with the duplicated junction at B dropped → 3 points.
    assertThat(full.getNumPoints()).isEqualTo(3);
    assertThat(full.getCoordinateN(0).y).isWithin(1e-4).of(STOP_A.getCoordinate().latitude());
    assertThat(full.getCoordinateN(2).y).isWithin(1e-4).of(STOP_C.getCoordinate().latitude());
  }

  @Test
  void factoryAcceptsZeroStopPattern() {
    // Degenerate edge case: a pattern with zero stops has no hops and no meaningful vertex
    // positions. The factory must still produce a valid (empty) sequence whose cumulative table
    // has the contract-mandated single zero entry — even though numberOfStops is 0.
    var subject = TripShapeFactory.straightLines(TransitRepositoryForTest.stopPattern(), NOOP);

    assertThat(subject.size()).isEqualTo(0);
    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    assertThat(subject.concatenate(0, subject.size()).isEmpty()).isTrue();
  }

  @Test
  void factoryAcceptsSingleStopPattern() {
    // Degenerate edge case: a pattern with a single stop has no hops. cumulative must still exist
    // and distanceBetween(0,0) must return 0 without throwing. concatenate(0, size) has nothing
    // to concatenate, so it returns an empty (but non-null) LineString.
    var subject = TripShapeFactory.straightLines(
      TransitRepositoryForTest.stopPattern(STOP_A),
      NOOP
    );

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    LineString empty = subject.concatenate(0, subject.size());
    assertThat(empty).isNotNull();
    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void factoryAcceptsEmptyHopGeometries() {
    // A non-null but empty list must be treated the same as the single-stop degenerate case:
    // factory succeeds, distanceBetween(0,0) is zero, concatenate(0, size) returns an empty
    // (non-null) LineString because there are no hops to concatenate.
    var subject = TripShapeFactory.fromLineStrings(List.of(), NOOP);

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    LineString empty = subject.concatenate(0, subject.size());
    assertThat(empty).isNotNull();
    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void concatenateWithSameBoardAndAlightReturnsEmptyGeometry() {
    // Board == alight is outside the ScheduledTransitLeg contract but the method is also reachable
    // directly on the sequence. Lock in the behaviour (empty LineString, no throw).
    var subject = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString empty = subject.concatenate(1, 1);
    assertThat(empty).isNotNull();
    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void identicalHopsShareOnePointerWhenDeduplicated() {
    var deduplicator = new Deduplicator();
    var first = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);
    var second = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);

    assertThat(first.getCompact(0)).isSameInstanceAs(second.getCompact(0));
    assertThat(first.getCompact(1)).isSameInstanceAs(second.getCompact(1));
    assertThat(first.getCompact(0)).isNotSameInstanceAs(first.getCompact(1));
  }

  @Test
  void sharedHopPointersSurviveSerialization() {
    var deduplicator = new Deduplicator();
    var first = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);
    var second = TripShapeFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);

    var kryo = KryoBuilder.create();
    var bytes = new ByteArrayOutputStream();
    try (var out = new Output(bytes)) {
      kryo.writeClassAndObject(out, List.of(first, second));
    }
    var read = (List<?>) kryo.readClassAndObject(new Input(bytes.toByteArray()));
    var a = (CompactLineStringSequence) read.get(0);
    var b = (CompactLineStringSequence) read.get(1);

    assertThat(a.getCompact(0)).isSameInstanceAs(b.getCompact(0));
    assertThat(a.getCompact(1)).isSameInstanceAs(b.getCompact(1));
    assertThat(a.getCompact(0)).isNotSameInstanceAs(a.getCompact(1));
  }
}

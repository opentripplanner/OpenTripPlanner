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
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * Exercises {@link TripGeometryFactory} and the resulting {@link CompactLineStringSequence} (the
 * per-pattern hop geometries + cumulative distance table).
 */
class TripGeometryFactoryTest {

  // Three stops roughly 2 km apart along a north-south line at 60° N.
  private static final WgsCoordinate A = new WgsCoordinate(60.0000, 10.0000);
  private static final WgsCoordinate B = new WgsCoordinate(60.0180, 10.0000);
  private static final WgsCoordinate C = new WgsCoordinate(60.0360, 10.0000);
  // A point midway between A and B so the A→B hop is a polyline of three points.
  private static final WgsCoordinate MID_AB = new WgsCoordinate(60.0090, 10.0000);

  private static final List<LineString> HOP_GEOMETRIES = List.of(
    makeLineString(A, MID_AB, B),
    makeLineString(B, C)
  );

  private static final DeduplicatorService NOOP = DeduplicatorService.NOOP;

  private static double exactDistance(WgsCoordinate from, WgsCoordinate to) {
    return SphericalDistanceLibrary.distance(
      from.latitude(),
      from.longitude(),
      to.latitude(),
      to.longitude()
    );
  }

  @Test
  void distanceBetweenWithHopGeometriesMatchesHaversineWithinOneMeter() {
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);

    // Contract: any distanceBetween(board, alight) is within 1 m of the exact haversine sum.
    double exactAtoB = exactDistance(A, B);
    double exactBtoC = exactDistance(B, C);

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
    var subject = TripGeometryFactory.straightLines(List.of(A, B, C), NOOP);

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    assertThat((double) subject.distanceBetween(0, 1))
      .isWithin(1.0)
      .of(exactDistance(A, B));
    assertThat((double) subject.distanceBetween(1, 2))
      .isWithin(1.0)
      .of(exactDistance(B, C));
  }

  @Test
  void cumulativeRoundingErrorIsBoundedByOneMeter() {
    // Build a long pattern (20 stops). Per-hop rounding would accumulate linearly here;
    // verify that accumulating in double and rounding once per entry keeps the error on the
    // full-pattern leg within 1 m.
    int numberOfStops = 20;
    var stops = new ArrayList<WgsCoordinate>();
    for (int i = 0; i < numberOfStops; i++) {
      // ~111 m spacing (at 60°N, 0.001° latitude ≈ 111 m).
      stops.add(new WgsCoordinate(60.0 + i * 0.001, 10.0));
    }

    var subject = TripGeometryFactory.straightLines(stops, NOOP);

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
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    int partA = subject.distanceBetween(0, 1);
    int partB = subject.distanceBetween(1, 2);
    int whole = subject.distanceBetween(0, 2);

    // Strict equality: differences of cumulative entries cancel exactly.
    assertThat(whole).isEqualTo(partA + partB);
  }

  @Test
  void hopGeometryReturnsCompressedRoundTrip() {
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString hop0 = subject.get(0);
    assertThat(hop0.getNumPoints()).isEqualTo(3);
    assertThat(hop0.getCoordinateN(0).y).isWithin(1e-4).of(A.latitude());
    assertThat(hop0.getCoordinateN(0).x).isWithin(1e-4).of(A.longitude());
    assertThat(hop0.getCoordinateN(2).y).isWithin(1e-4).of(B.latitude());
    assertThat(hop0.getCoordinateN(2).x).isWithin(1e-4).of(B.longitude());

    LineString hop1 = subject.get(1);
    assertThat(hop1.getNumPoints()).isEqualTo(2);
    assertThat(hop1.getCoordinateN(0).y).isWithin(1e-4).of(B.latitude());
    assertThat(hop1.getCoordinateN(1).y).isWithin(1e-4).of(C.latitude());
  }

  @Test
  void hopGeometrySynthesizesStraightLineWhenHopGeometriesAreNull() {
    var subject = TripGeometryFactory.straightLines(List.of(A, B, C), NOOP);

    LineString hop0 = subject.get(0);
    assertThat(hop0.getNumPoints()).isEqualTo(2);
    assertThat(hop0.getCoordinateN(0).y).isEqualTo(A.latitude());
    assertThat(hop0.getCoordinateN(1).y).isEqualTo(B.latitude());
  }

  @Test
  void concatenateConcatenatesHopsInRange() {
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString full = subject.concatenate(0, 2);
    // Hop 0: A, MID, B (3 points). Hop 1: B, C (2 points). Duplicate B at the join is dropped.
    assertThat(full.getNumPoints()).isEqualTo(4);
    assertThat(full.getCoordinateN(0).y).isWithin(1e-4).of(A.latitude());
    assertThat(full.getCoordinateN(3).y).isWithin(1e-4).of(C.latitude());

    assertThat(subject.concatenate(0, 1).getNumPoints()).isEqualTo(3);
    assertThat(subject.concatenate(1, 2).getNumPoints()).isEqualTo(2);
  }

  @Test
  void concatenateAllReturnsFullPatternWhenShapeAvailable() {
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString full = subject.concatenate();
    assertThat(full).isNotNull();
    assertThat(full.getNumPoints()).isEqualTo(4);
  }

  @Test
  void concatenateAllSynthesizesStraightLineWhenShapeMissing() {
    // Shapeless patterns expose a non-null straight-line geometry made of one 2-point segment per
    // hop, rather than the historical null sentinel.
    var subject = TripGeometryFactory.straightLines(List.of(A, B, C), NOOP);

    LineString full = subject.concatenate();
    assertThat(full).isNotNull();
    // Two hops of 2 points each, with the duplicated junction at B dropped → 3 points.
    assertThat(full.getNumPoints()).isEqualTo(3);
    assertThat(full.getCoordinateN(0).y).isWithin(1e-4).of(A.latitude());
    assertThat(full.getCoordinateN(2).y).isWithin(1e-4).of(C.latitude());
  }

  @Test
  void factoryAcceptsZeroStopPattern() {
    // Degenerate edge case: a pattern with zero stops has no hops and no meaningful vertex
    // positions. The factory must still produce a valid (empty) sequence whose cumulative table
    // has the contract-mandated single zero entry — even though numberOfStops is 0.
    var subject = TripGeometryFactory.straightLines(List.of(), NOOP);

    assertThat(subject.size()).isEqualTo(0);
    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    assertThat(subject.concatenate(0, subject.size()).isEmpty()).isTrue();
  }

  @Test
  void factoryAcceptsSingleStopPattern() {
    // Degenerate edge case: a pattern with a single stop has no hops. cumulative must still exist
    // and distanceBetween(0,0) must return 0 without throwing. concatenate(0, size) has nothing
    // to concatenate, so it returns an empty (but non-null) LineString.
    var subject = TripGeometryFactory.straightLines(List.of(A), NOOP);

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
    var subject = TripGeometryFactory.fromLineStrings(List.of(), NOOP);

    assertThat(subject.distanceBetween(0, 0)).isEqualTo(0);
    LineString empty = subject.concatenate(0, subject.size());
    assertThat(empty).isNotNull();
    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void concatenateWithSameBoardAndAlightReturnsEmptyGeometry() {
    // Board == alight is outside the ScheduledTransitLeg contract but the method is also reachable
    // directly on the sequence. Lock in the behaviour (empty LineString, no throw).
    var subject = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, NOOP);

    LineString empty = subject.concatenate(1, 1);
    assertThat(empty).isNotNull();
    assertThat(empty.isEmpty()).isTrue();
  }

  @Test
  void identicalHopsShareOnePointerWhenDeduplicated() {
    var deduplicator = new Deduplicator();
    var first = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);
    var second = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);

    assertThat(first.getCompact(0)).isSameInstanceAs(second.getCompact(0));
    assertThat(first.getCompact(1)).isSameInstanceAs(second.getCompact(1));
    assertThat(first.getCompact(0)).isNotSameInstanceAs(first.getCompact(1));
  }

  @Test
  void sharedHopPointersSurviveSerialization() {
    var deduplicator = new Deduplicator();
    var first = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);
    var second = TripGeometryFactory.fromLineStrings(HOP_GEOMETRIES, deduplicator);

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

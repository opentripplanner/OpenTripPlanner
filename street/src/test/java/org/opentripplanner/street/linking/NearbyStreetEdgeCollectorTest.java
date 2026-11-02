package org.opentripplanner.street.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

class NearbyStreetEdgeCollectorTest {

  /** At the equator the equirectangular projection does not scale longitudes. */
  private static final double XSCALE = 1.0;
  /**
   * The edges run east-west at this latitude, so their squared distance to the origin is exactly
   * representable and the radius boundary can be tested without rounding slack.
   */
  private static final double EDGE_LAT = 0.5;
  private static final double EDGE_SQUARED_DISTANCE = EDGE_LAT * EDGE_LAT;
  private static final double RADIUS_INSIDE = 0.75;

  private static final IntersectionVertex ORIGIN = intersectionVertex(0.0, 0.0);
  private static final TraverseModeSet WALK = new TraverseModeSet(TraverseMode.WALK);
  private static final TraverseModeSet CAR = new TraverseModeSet(TraverseMode.CAR);

  @Test
  void collectsTraversableEdgeWithinRadius() {
    var edge = edge(StreetTraversalPermission.ALL);
    var collector = collector(WALK, RADIUS_INSIDE);

    collector.accept(edge);

    assertThat(collector.nearbyEdges()).hasSize(1);
    var candidate = collector.nearbyEdges().getFirst();
    assertThat(candidate.edge()).isSameInstanceAs(edge);
    assertThat(candidate.squaredDistanceDegreesLat()).isEqualTo(EDGE_SQUARED_DISTANCE);
  }

  @Test
  void rejectsEdgeNotTraversableByAnyMode() {
    var collector = collector(CAR, RADIUS_INSIDE);

    collector.accept(edge(StreetTraversalPermission.PEDESTRIAN));

    assertThat(collector.nearbyEdges()).isEmpty();
  }

  @Test
  void keepsEdgeTraversableByOneOfTheModes() {
    var edge = edge(StreetTraversalPermission.PEDESTRIAN);
    var collector = collector(
      new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK),
      RADIUS_INSIDE
    );

    collector.accept(edge);

    assertThat(collector.nearbyEdges()).hasSize(1);
  }

  @Test
  void rejectsEdgeExactlyOnTheRadius() {
    var collector = collector(WALK, EDGE_LAT);

    collector.accept(edge(StreetTraversalPermission.ALL));

    assertThat(collector.nearbyEdges()).isEmpty();
  }

  @Test
  void rejectsEdgeBeyondTheRadius() {
    var collector = collector(WALK, 0.25);

    collector.accept(edge(StreetTraversalPermission.ALL));

    assertThat(collector.nearbyEdges()).isEmpty();
  }

  @Test
  void ignoresNonStreetEdges() {
    var collector = collector(WALK, RADIUS_INSIDE);

    collector.accept(
      FreeEdge.createFreeEdge(intersectionVertex(EDGE_LAT, -1), intersectionVertex(EDGE_LAT, 1))
    );

    assertThat(collector.nearbyEdges()).isEmpty();
  }

  @Test
  void deduplicatesEdgeReportedByMultipleGridCells() {
    var edge = edge(StreetTraversalPermission.ALL);
    var collector = collector(WALK, RADIUS_INSIDE);

    collector.accept(edge);
    collector.accept(edge);

    assertThat(collector.nearbyEdges()).hasSize(1);
  }

  @Test
  void rejectsEdgeNoLongerReachableFromGraph() {
    var edge = edge(StreetTraversalPermission.ALL);
    edge.getToVertex().removeIncoming(edge);
    var collector = collector(WALK, RADIUS_INSIDE);

    collector.accept(edge);

    assertThat(collector.nearbyEdges()).isEmpty();
  }

  private static NearbyStreetEdgeCollector collector(TraverseModeSet modes, double radiusDeg) {
    return new NearbyStreetEdgeCollector(ORIGIN, modes, radiusDeg, XSCALE);
  }

  private static StreetEdge edge(StreetTraversalPermission permission) {
    return streetEdge(
      intersectionVertex(EDGE_LAT, -1),
      intersectionVertex(EDGE_LAT, 1),
      permission
    );
  }
}

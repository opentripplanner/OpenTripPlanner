package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;

class SubgraphOnlyFerryTest {

  private static final FeedScopedId REGULAR_STOP1 = id("TEST-1");
  private static final FeedScopedId REGULAR_STOP2 = id("TEST-2");

  @Test
  void subgraphHasOnlyFerry() {
    var transitStopVertex = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();

    var subgraph = subgraph(transitStopVertex);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoFerry() {
    var transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(false).build();

    final Subgraph subgraph = subgraph(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyFerryMoreStops() {
    var transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();
    var transitStopVertex2 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();

    var subgraph = subgraph(transitStopVertex1, transitStopVertex2);
    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasNotOnlyFerryMoreStops() {
    var transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();
    var transitStopVertex2 = vertexBuilder(REGULAR_STOP2).withIsFerry(false).build();
    var subgraph = subgraph(transitStopVertex1, transitStopVertex2);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  private static Subgraph subgraph(TransitStopVertex... transitStopVertex) {
    var index = new VertexIndex(Arrays.asList(transitStopVertex));
    Subgraph subgraph = new Subgraph(index);
    for (var v : transitStopVertex) {
      subgraph.addVertex(index.idOf(v));
    }

    return subgraph;
  }

  private static TransitStopVertexBuilder vertexBuilder(FeedScopedId id) {
    return TransitStopVertex.of()
      .withId(id)
      .withPoint(GeometryUtils.getGeometryFactory().createPoint(Coordinates.BERLIN));
  }
}

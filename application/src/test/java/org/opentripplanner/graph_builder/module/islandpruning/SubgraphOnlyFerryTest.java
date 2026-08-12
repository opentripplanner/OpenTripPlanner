package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

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
    TransitStopVertex transitStopVertex = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoFerry() {
    TransitStopVertex transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(false).build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyFerryMoreStops() {
    TransitStopVertex transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();
    TransitStopVertex transitStopVertex2 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasNotOnlyFerryMoreStops() {
    TransitStopVertex transitStopVertex1 = vertexBuilder(REGULAR_STOP1).withIsFerry(true).build();
    TransitStopVertex transitStopVertex2 = vertexBuilder(REGULAR_STOP2).withIsFerry(false).build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  private static TransitStopVertexBuilder vertexBuilder(FeedScopedId id) {
    return TransitStopVertex.of()
      .withId(id)
      .withPoint(GeometryUtils.getGeometryFactory().createPoint(Coordinates.BERLIN));
  }
}

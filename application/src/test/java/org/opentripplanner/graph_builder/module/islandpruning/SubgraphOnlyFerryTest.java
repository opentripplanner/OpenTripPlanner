package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;

class SubgraphOnlyFerryTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final RegularStop REGULAR_STOP1 = TEST_MODEL.stop("TEST-1").build();
  private static final RegularStop REGULAR_STOP2 = TEST_MODEL.stop("TEST-2").build();

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

  private static TransitStopVertexBuilder vertexBuilder(RegularStop stop) {
    return TransitStopVertex.of().withId(stop.getId()).withPoint(stop.getGeometry());
  }
}

package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class SubgraphOnlyFerryTest {

  private static RegularStop regularStop1;
  private static RegularStop regularStop2;

  @BeforeAll
  static void setUp() {
    regularStop1 =
      RegularStop
        .of(new FeedScopedId("HH-GTFS", "TEST1"), () -> 0)
        .withCoordinate(53.54948, 9.98455)
        .build();
    regularStop2 =
      RegularStop
        .of(new FeedScopedId("HH-GTFS", "TEST2"), () -> 0)
        .withCoordinate(53.55272, 9.99480)
        .build();
  }

  @Test
  void subgraphHasOnlyFerry() {
    TransitStopVertex transitStopVertex = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoFerry() {
    TransitStopVertex transitStopVertex1 = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.BUS))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoMode() {
    TransitStopVertex transitStopVertex1 = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of())
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyFerryMoreStops() {
    TransitStopVertex transitStopVertex1 = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = new TransitStopVertexBuilder()
      .withStop(regularStop2)
      .withModes(Set.of(TransitMode.FERRY))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasNotOnlyFerryMoreStops() {
    TransitStopVertex transitStopVertex1 = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = new TransitStopVertexBuilder()
      .withStop(regularStop2)
      .withModes(Set.of(TransitMode.BUS))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasNoModeMoreStops() {
    TransitStopVertex transitStopVertex1 = new TransitStopVertexBuilder()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = new TransitStopVertexBuilder()
      .withStop(regularStop2)
      .withModes(Set.of())
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertFalse(subgraph.hasOnlyFerryStops());
  }
}

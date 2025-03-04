package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

class SubgraphOnlyFerryTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final RegularStop regularStop1 = TEST_MODEL.stop("TEST-1").build();
  private static final RegularStop regularStop2 = TEST_MODEL.stop("TEST-2").build();

  @Test
  void subgraphHasOnlyFerry() {
    TransitStopVertex transitStopVertex = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex);

    assertTrue(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoFerry() {
    TransitStopVertex transitStopVertex1 = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.BUS))
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyNoMode() {
    TransitStopVertex transitStopVertex1 = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of())
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);

    assertFalse(subgraph.hasOnlyFerryStops());
  }

  @Test
  void subgraphHasOnlyFerryMoreStops() {
    TransitStopVertex transitStopVertex1 = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = TransitStopVertex.of()
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
    TransitStopVertex transitStopVertex1 = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = TransitStopVertex.of()
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
    TransitStopVertex transitStopVertex1 = TransitStopVertex.of()
      .withStop(regularStop1)
      .withModes(Set.of(TransitMode.FERRY))
      .build();
    TransitStopVertex transitStopVertex2 = TransitStopVertex.of()
      .withStop(regularStop2)
      .withModes(Set.of())
      .build();

    Subgraph subgraph = new Subgraph();
    subgraph.addVertex(transitStopVertex1);
    subgraph.addVertex(transitStopVertex2);

    assertFalse(subgraph.hasOnlyFerryStops());
  }
}

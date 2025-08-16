package org.opentripplanner.street.search;

import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;

class TemporaryVerticesContainerTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final RegularStop stopA = testModel.stop("A").build();
  private final RegularStop stopB = testModel.stop("B").build();
  private final RegularStop stopC = testModel.stop("C").build();

  @Test
  void foo() {
    var graph = new Graph();

    Stream.of(stopA, stopB, stopC).forEach(s ->
      graph.addVertex(TransitStopVertex.of().withStop(s).build())
    );

    graph.index();
    var container = new TemporaryVerticesContainer(
      graph,
      TestVertexLinker.of(graph),
      id -> Set.of(),
      GenericLocation.fromStopId("a", "F", "A"),
      GenericLocation.fromStopId("a", "F", "B"),
      WALK,
      WALK
    );
    var x = container.getToVertices();
    System.out.println(x);
  }
}

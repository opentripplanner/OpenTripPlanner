package org.opentripplanner.street.search;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.WALK;

import java.util.List;
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
  void stopId() {
    var graph = new Graph();

    Stream.of(stopA, stopB, stopC).forEach(s ->
      graph.addVertex(TransitStopVertex.of().withStop(s).build())
    );

    graph.index();
    var container = new TemporaryVerticesContainer(
      graph,
      TestVertexLinker.of(graph),
      id -> Set.of(),
      stopToLocation(stopA),
      stopToLocation(stopB),
      WALK,
      WALK
    );
    var from = container.getFromVertices();
    assertThat(from).hasSize(1);
    var fromStop = ((TransitStopVertex) List.copyOf(from).getFirst()).getStop();

    assertEquals(stopA, fromStop);
  }

  private GenericLocation stopToLocation(RegularStop s) {
    return GenericLocation.fromStopId(
      s.getName().toString(),
      s.getId().getFeedId(),
      s.getId().getId()
    );
  }
}

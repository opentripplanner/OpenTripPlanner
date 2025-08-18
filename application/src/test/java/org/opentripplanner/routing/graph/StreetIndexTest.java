package org.opentripplanner.routing.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.StationCentroidVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

class StreetIndexTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final RegularStop stop = testModel.stop("A").build();
  private final TransitStopVertex stopVertex = TransitStopVertex.of().withStop(stop).build();
  private final Station station = testModel
    .station("OMEGA")
    .withShouldRouteToCentroid(true)
    .build();
  private final StationCentroidVertex centroidVertex = new StationCentroidVertex(station);

  @Test
  void stopId() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStopVertex(stop.getId())).hasValue(stopVertex);
  }

  @Test
  void nonExistentId() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStopVertex(id("non-existent-stop-id"))).isEmpty();
  }

  @Test
  void stationCentroid() {
    var streetIndex = buildIndex();
    assertThat(streetIndex.findStationCentroidVertex(station.getId())).hasValue(centroidVertex);
  }

  private StreetIndex buildIndex() {
    var graph = new Graph();
    graph.addVertex(stopVertex);
    graph.addVertex(centroidVertex);
    return new StreetIndex(graph);
  }
}

package org.opentripplanner.routing.linking;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.test.support.GeoJsonIo;

class VertexLinkerTest {

  @Test
  void flex() {
    var v1 = StreetModelForTest.intersectionVertex(0, 0);
    var v2 = StreetModelForTest.intersectionVertex(0.1, 0.1);
    var edge = StreetModelForTest.streetEdge(v1, v2);

    var graph = new Graph();

    graph.addVertex(v1);
    graph.addVertex(v2);
    graph.index();

    System.out.println(GeoJsonIo.toUrl(graph));
  }
}

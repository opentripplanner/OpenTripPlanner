package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.test.support.GeoJsonIo;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class SimpleAreaTest {

  @Test
  void walkableArea() {
    var inside0 = node(0, new WgsCoordinate(0, 0));
    var inside1 = node(2, new WgsCoordinate(5, 5));
    var area = List.of(
      inside0,
      node(1, new WgsCoordinate(0, 5)),
      inside1,
      node(4, new WgsCoordinate(5, 0))
    );

    var outside0 = node(5, new WgsCoordinate(-1, 0));
    var outside1 = node(6, new WgsCoordinate(6, 5));

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(area)
      .addWayFromNodes(outside0, inside0)
      .addWayFromNodes(outside1, inside1)
      .build();

    var graph = new Graph(new Deduplicator());
    var osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    )
      .withAreaVisibility(true)
      .withMaxAreaNodes(10)
      .build();

    osmModule.buildGraph();

    assertFalse(graph.getVertices().isEmpty());

    System.out.println(GeoJsonIo.toUrl(graph));
    assertEquals(10, graph.getEdgesOfType(AreaEdge.class).size());
  }
}

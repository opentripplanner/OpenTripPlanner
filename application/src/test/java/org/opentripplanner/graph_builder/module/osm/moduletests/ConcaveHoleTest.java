package org.opentripplanner.graph_builder.module.osm.moduletests;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class ConcaveHoleTest {

  @Test
  void oneWayPlatform() {
    var outer = List.of(
      new WgsCoordinate(0,0),
      new WgsCoordinate(0,5),
      new WgsCoordinate(5,5),
      new WgsCoordinate(5,0)
    );
    var hole = List.of(
      new WgsCoordinate(1,1),
      new WgsCoordinate(1,4),
      new WgsCoordinate(4,4),
      new WgsCoordinate(3,3),
      new WgsCoordinate(3,2),
      new WgsCoordinate(2,2),
      new WgsCoordinate(3,2),
      new WgsCoordinate(3,1)
    );


    var provider = TestOsmProvider.of().build();

    var graph = new Graph(new Deduplicator());
    var osmModule = OsmModule
      .of(provider, graph, new DefaultOsmInfoGraphBuildRepository(), new DefaultVehicleParkingRepository())
      .withAreaVisibility(true)
      .build();

    osmModule.buildGraph();

    assertFalse(graph.getVertices().isEmpty());

  }

}

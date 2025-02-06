package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class BoardingLocationTest {

  /**
   * There is a one-way road which is also marked as a platform in Sky Campus which crashed OSM.
   */
  @Test
  void oneWayPlatform() {
    var way = WayTestData.platform();
    way.addTag("access", "no");
    way.addTag("motor_vehicle", "permissive");
    way.addTag("oneway", "yes");
    var provider = TestOsmProvider.of().addWay(way).build();

    var graph = new Graph(new Deduplicator());
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var osmModule = OsmModule
      .of(provider, graph, osmInfoRepository, new DefaultVehicleParkingRepository())
      .withBoardingAreaRefTags(Set.of("ref"))
      .build();

    osmModule.buildGraph();
    var edges = List.copyOf(graph.getEdges());
    assertThat(edges).hasSize(1);

    var platform = osmInfoRepository.findPlatform(edges.getFirst());

    assertTrue(platform.isPresent());
    assertEquals(Set.of("123"), platform.get().references());
  }

  @Test
  void skipPlatformsWithoutReferences() {
    var way = new OsmWay();
    way.addTag("public_transport", "platform");
    var provider = TestOsmProvider.of().addWay(way).build();

    var graph = new Graph(new Deduplicator());
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var osmModule = OsmModule
      .of(provider, graph, osmInfoRepository, new DefaultVehicleParkingRepository())
      .withBoardingAreaRefTags(Set.of("ref"))
      .build();

    osmModule.buildGraph();
    var edges = List.copyOf(graph.getEdges());
    assertThat(edges).hasSize(2);

    var platform = osmInfoRepository.findPlatform(edges.getFirst());
    assertTrue(platform.isEmpty());
  }
}

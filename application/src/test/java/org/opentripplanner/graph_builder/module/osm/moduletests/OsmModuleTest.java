package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class OsmModuleTest {


  /**
   * There is a one-way road which is also marked as a platform in Sky Campus which crashed OSM
   */
  @Test
  void testCrappyOsmPlatform() {

    var provider = new OsmProvider() {
      @Override
      public void readOsm(OsmDatabase osmdb) {

        var way = new OsmWay();
        way.addTag("public_transport", "platform");
        way.addTag("access", "no");
        way.addTag("motor_vehicle", "permissive");
        way.addTag("ref", "123");
        way.addTag("oneway", "yes");
        way.setOsmProvider(this);
        way.getNodeRefs().add(1);
        way.getNodeRefs().add(2);
        osmdb.addWay(way);

        osmdb.doneSecondPhaseWays();

        var node1 = new OsmNode(1,1);
        node1.setId(1);
        var node2 = new OsmNode(1.1,1.1);
        node2.setId(2);

        osmdb.addNode(node1);
        osmdb.addNode(node2);

      }

      @Override
      public OsmTagMapper getOsmTagMapper() {
        return new OsmTagMapper();
      }

      @Override
      public void checkInputs() {

      }

      @Override
      public WayPropertySet getWayPropertySet() {
        return new WayPropertySet();
      }

      @Override
      public ZoneId getZoneId() {
        return ZoneIds.LONDON;
      }
    };

    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var osmModule = OsmModule
      .of(
        provider,
        graph,
        osmInfoRepository,
        new DefaultVehicleParkingRepository()
      )
      .withBoardingAreaRefTags(Set.of("naptan:AtcoCode"))
      .build();

    osmModule.buildGraph();
    assertThat(graph.getEdges()).isNotEmpty();
  }

}

package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.RelationBuilder;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.test.support.GeoJsonIo;
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * Checks that turn restrictions are processed even if they don't strictly adhere to their
 * stated type, for example that a "no straight on" restriction is added to two edges even if
 * - geometrically speaking - you are not going straight.
 * <p>
 * https://github.com/opentripplanner/OpenTripPlanner/issues/6536
 */
class TurnRestrictionsTest {

  @Test
  void noStraightOn() {
    var node0 = node(0, new WgsCoordinate(0, 0));
    var node1 = node(1, new WgsCoordinate(0, -1));
    var node2 = node(2, new WgsCoordinate(1, 0));

    long way1Id = 100;
    long way2Id = 101;

    var turnRestriction = RelationBuilder.ofTurnRestriction("no_straight_on")
      .withWayMember(way1Id, "from")
      .withWayMember(way2Id, "to")
      .withNodeMember(node0.getId(), "via")
      .build();

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way1Id, List.of(node0, node1))
      .addWayFromNodes(way2Id, List.of(node0, node2))
      .addRelation(turnRestriction)
      .build();

    var graph = new Graph(new Deduplicator());

    var osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();

    osmModule.buildGraph();

    System.out.println(GeoJsonIo.toUrl(graph));

    var edgesWithTurnRestrictions = graph
      .getStreetEdges()
      .stream()
      .filter(e -> !e.getTurnRestrictions().isEmpty())
      .toList();
    assertThat(edgesWithTurnRestrictions).hasSize(1);
  }
}

package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.RelationBuilder;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * Checks that turn restrictions are processed even if they don't strictly adhere to their
 * stated type, for example that a "no straight on" restriction is added to two edges even if
 * - geometrically speaking - you are not going straight.
 * <p>
 * https://github.com/opentripplanner/OpenTripPlanner/issues/6536
 */
class TurnRestrictionsTest {

  static Stream<Arguments> testTurnRestrictionsSource() {
    var node05 = node(0, new WgsCoordinate(0, 5));
    var node05b = node(1, new WgsCoordinate(0, 5.1));
    var node15 = node(2, new WgsCoordinate(1, 5));
    var node16 = node(3, new WgsCoordinate(1, 6));
    var node24 = node(4, new WgsCoordinate(2, 4));
    var node25 = node(5, new WgsCoordinate(2, 5));
    var node26 = node(6, new WgsCoordinate(2, 6));
    return Stream.of(
      Arguments.of("no_straight_on", false, node05, node15, node25),
      Arguments.of("no_left_turn", false, node05, node15, node24),
      Arguments.of("no_right_turn", false, node05, node15, node26),
      Arguments.of("no_u_turn", false, node05, node15, node05b),
      Arguments.of("no_straight_on", true, node05, node15, node24),
      Arguments.of("no_left_turn", true, node05, node15, node26),
      Arguments.of("no_right_turn", true, node05, node15, node24),
      Arguments.of("no_u_turn", true, node05, node15, node16)
    );
  }

  @ParameterizedTest
  @MethodSource("testTurnRestrictionsSource")
  void testTurnRestrictions(
    String restrictionType,
    boolean shouldWarn,
    OsmNode node0,
    OsmNode node1,
    OsmNode node2
  ) {
    long way1Id = 100;
    long way2Id = 101;

    var turnRestriction = RelationBuilder.ofTurnRestriction(restrictionType)
      .withWayMember(way1Id, "from")
      .withWayMember(way2Id, "to")
      .withNodeMember(node1.getId(), "via")
      .build();

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way1Id, List.of(node0, node1))
      .addWayFromNodes(way2Id, List.of(node1, node2))
      .addRelation(turnRestriction)
      .build();

    var graph = new Graph(new Deduplicator());

    var issueStore = new DefaultDataImportIssueStore();

    var osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    )
      .withIssueStore(issueStore)
      .build();

    osmModule.buildGraph();

    assertThat(
      issueStore.listIssues().stream().filter(i -> i instanceof TurnRestrictionBad).toList()
    ).hasSize(shouldWarn ? 1 : 0);
  }
}

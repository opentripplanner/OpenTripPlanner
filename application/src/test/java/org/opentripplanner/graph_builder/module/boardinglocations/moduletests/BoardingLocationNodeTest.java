package org.opentripplanner.graph_builder.module.boardinglocations.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.OSM;
import static org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource.TRANSIT;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.BoardingLocationCoordinateSource;
import org.opentripplanner.graph_builder.module.boardinglocations.BoardingLocationsEnvironment;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;

/** Linking stops to a tagged OSM boarding location node. */
class BoardingLocationNodeTest {

  private static final WgsCoordinate NODE = new WgsCoordinate(53.55, 10.0);

  /**
   * With {@link BoardingLocationCoordinateSource#TRANSIT} the stop is placed at its own coordinate
   * and reaches the node over an edge of that length, rather than taking the node's coordinate as
   * its own. The node itself is not moved - it keeps its coordinate and its street links.
   */
  @Test
  void aStopKeepsItsOwnCoordinateAndReachesTheNode() {
    var test = BoardingLocationsEnvironment.of(TRANSIT, footwayWithNode("node-stop"));
    var stop = test.stop("node-stop", NODE.moveNorthMeters(20));

    var result = test.build();

    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.55,10) → (53.55,9.999546) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000454) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55018,10) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(53.55,9.999546) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000454) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55018,10) → (53.55,10) PEDESTRIAN_AND_BICYCLE ♿✅",
        "(53.55018,10)[F:node-stop] linked to (53.55018,10)",
        "(53.55018,10) linked to (53.55018,10)[F:node-stop]"
      );
    // The length is the point of TRANSIT mode and is not part of the edge summary above.
    assertWithMessage("the 20 m offset must cost its real distance")
      .that(result.connectors(stop).getFirst().getDistanceMeters())
      .isWithin(0.5)
      .of(20);
    // The summary only shows coordinates: make sure the vertex at the node is the node itself.
    var osmNode = result.connectors(stop).getFirst().getToVertex();
    assertThat(osmNode).isInstanceOf(OsmBoardingLocationVertex.class);
    assertWithMessage("the OSM node must not be moved")
      .that(osmNode.getCoordinate())
      .isEqualTo(NODE.asJtsCoordinate());
    assertThat(result.issueTypes()).contains("StopFarFromBoardingLocation");
  }

  /**
   * In {@code OSM} mode the stop takes the node's coordinate as its position on the street network,
   * as it always has.
   */
  @Test
  void osmModeLinksTheStopStraightToTheNode() {
    var test = BoardingLocationsEnvironment.of(OSM, footwayWithNode("node-stop"));
    test.stop("node-stop", NODE.moveNorthMeters(20));

    var result = test.build();

    assertWithMessage("Unexpected edges. Check graph at %s", result.geoJsonUrl())
      .that(result.summarizeEdges())
      .containsExactly(
        "(53.55,10) → (53.55,9.999546) PEDESTRIAN ♿✅",
        "(53.55,10) → (53.55,10.000454) PEDESTRIAN ♿✅",
        "(53.55,10) linked to (53.55018,10)[F:node-stop]",
        "(53.55,9.999546) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55,10.000454) → (53.55,10) PEDESTRIAN ♿✅",
        "(53.55018,10)[F:node-stop] linked to (53.55,10)"
      );
  }

  /**
   * A footway whose middle node is tagged as a boarding location. A tagged node only becomes a
   * vertex when it is part of a way, which is what makes it linkable.
   */
  private static TestOsmProvider footwayWithNode(String ref) {
    return TestOsmProvider.of()
      .addWayThroughNode(
        NodeBuilder.of(1, NODE).withTag("highway", "bus_stop").withTag("ref", ref).build()
      )
      .build();
  }
}

package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.osm.model.RelationBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/**
 * Extends the stop-area platform scene with an underpass running beneath the platform and an
 * elevator connecting the two levels.
 *
 * <p>The platform (level 0) and the elevator node are both members of a
 * {@code public_transport=stop_area} relation. The platform is reached by a stair at its bottom-left
 * corner. The elevator is a single node in the middle of the platform. On the top level a footway
 * runs straight north from the elevator to an extra node inserted into the northern platform edge;
 * the underpass ({@code tunnel=yes}, level -1) shares the same elevator node from below. OTP
 * therefore builds elevator board/alight/hop edges linking the platform (level 0) to the underpass
 * (level -1).
 *
 * <p>Platform entry linking is disabled, but area visibility is enabled. Because the elevator node
 * sits inside the walkable area and is shared with the footway, the visibility builder attaches it
 * to the platform corners in addition to the explicit footway. The node therefore gets two level-0
 * attachments — one from the platform area (WAY:100) and one from the footway (WAY:2) — joined by a
 * zero-level elevator hop, and the footway to the northern edge is mirrored by a visibility edge.
 */
class StopAreaWithUnderpassAndElevatorTest {

  @Test
  void underpassReachesPlatformViaElevator() {
    var bl = node(0, new WgsCoordinate(0, 0));
    var tl = node(1, new WgsCoordinate(5, 0));
    // extra node inserted into the northern platform edge, due north of the elevator
    var north = node(7, new WgsCoordinate(5, 2.5));
    var tr = node(2, new WgsCoordinate(5, 5));
    var br = node(3, new WgsCoordinate(0, 5));
    var platform = List.of(bl, tl, north, tr, br);

    // Stair rises from outside to the bottom-left corner.
    var stairBottom = node(4, new WgsCoordinate(-1, 0));

    // Elevator is a single node in the middle of the platform, shared with the underpass below.
    var elevator = NodeBuilder.of(5, new WgsCoordinate(2.5, 2.5))
      .withTag("highway", "elevator")
      .withTag("wheelchair", "yes")
      .build();

    // Underpass runs beneath the platform and surfaces outside it.
    var underpassEnd = node(6, new WgsCoordinate(2.5, 7));

    var platformId = 100;
    var stopArea = RelationBuilder.ofStopArea()
      .withWayMember(platformId, "platform")
      .withNodeMember(elevator.getId())
      .build();

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(way -> way.withTag("public_transport", "platform"), platformId, platform)
      .addWayFromNodes(way -> way.withTag("highway", "steps"), stairBottom, bl)
      // top-level footway running straight north from the elevator to the northern platform edge
      .addWayFromNodes(
        way -> way.withTag("highway", "footway").withTag("level", "0"),
        elevator,
        north
      )
      // underpass one level below, sharing the elevator node
      .addWayFromNodes(
        way -> way.withTag("highway", "footway").withTag("tunnel", "yes").withTag("level", "-1"),
        elevator,
        underpassEnd
      )
      .addRelation(stopArea)
      .build();

    var graph = new Graph();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withAreaVisibility(true)
      .withMaxAreaNodes(10)
      .withPlatformEntriesLinking(false)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly(
        // stair meeting the bottom-left corner (wheelchair-inaccessible steps)
        "(0,0) → (-1,0) PEDESTRIAN ♿❌",
        "(-1,0) → (0,0) PEDESTRIAN ♿❌",
        // platform ring (5 sides × 2 directions; the northern side is split by the north node)
        "(0,0) → (5,0) PEDESTRIAN ♿✅",
        "(5,0) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,5) PEDESTRIAN ♿✅",
        "(0,5) → (0,0) PEDESTRIAN ♿✅",
        "(5,0) → (5,2.5) PEDESTRIAN ♿✅",
        "(5,2.5) → (5,0) PEDESTRIAN ♿✅",
        "(5,5) → (5,2.5) PEDESTRIAN ♿✅",
        "(5,2.5) → (5,5) PEDESTRIAN ♿✅",
        "(5,5) → (0,5) PEDESTRIAN ♿✅",
        "(0,5) → (5,5) PEDESTRIAN ♿✅",
        // visibility edges from the platform corners to the interior nodes they can see
        "(0,0) → (5,2.5) PEDESTRIAN ♿✅",
        "(5,2.5) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (2.5,2.5) PEDESTRIAN ♿✅",
        "(2.5,2.5) → (0,0) PEDESTRIAN ♿✅",
        "(5,5) → (2.5,2.5) PEDESTRIAN ♿✅",
        "(2.5,2.5) → (5,5) PEDESTRIAN ♿✅",
        // north-node-to-elevator connection: the explicit level-0 footway plus a mirroring
        // visibility edge, so each direction appears twice
        "(5,2.5) → (2.5,2.5) PEDESTRIAN ♿✅",
        "(5,2.5) → (2.5,2.5) PEDESTRIAN ♿✅",
        "(2.5,2.5) → (5,2.5) PEDESTRIAN ♿✅",
        "(2.5,2.5) → (5,2.5) PEDESTRIAN ♿✅",
        // underpass (tunnel, level -1) surfacing outside the platform
        "(2.5,7) → (2.5,2.5) PEDESTRIAN ♿✅",
        "(2.5,2.5) → (2.5,7) PEDESTRIAN ♿✅",
        // elevator level 0: the node is attached both by the platform area (WAY:100) and the footway
        // (WAY:2), joined by a zero-level hop
        "osm:node:5:WAY:100 → elevator/osm:node:5:WAY:100 ELEVATOR board",
        "elevator/osm:node:5:WAY:100 → osm:node:5:WAY:100 ELEVATOR alight",
        "elevator/osm:node:5:WAY:100 → elevator/osm:node:5:WAY:2 ELEVATOR hop levels=0 ♿✅",
        "elevator/osm:node:5:WAY:2 → elevator/osm:node:5:WAY:100 ELEVATOR hop levels=0 ♿✅",
        // elevator between the footway (level 0) and the underpass (WAY:3, level -1)
        "osm:node:5:WAY:2 → elevator/osm:node:5:WAY:2 ELEVATOR board",
        "elevator/osm:node:5:WAY:2 → osm:node:5:WAY:2 ELEVATOR alight",
        "osm:node:5:WAY:3 → elevator/osm:node:5:WAY:3 ELEVATOR board",
        "elevator/osm:node:5:WAY:3 → osm:node:5:WAY:3 ELEVATOR alight",
        "elevator/osm:node:5:WAY:2 → elevator/osm:node:5:WAY:3 ELEVATOR hop levels=1 ♿✅",
        "elevator/osm:node:5:WAY:3 → elevator/osm:node:5:WAY:2 ELEVATOR hop levels=1 ♿✅"
      );
  }
}

package org.opentripplanner.graph_builder.module.osm.moduletests.walkablearea;

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
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.GeoJsonIo;
import org.opentripplanner.transit.model.framework.Deduplicator;

/**
 * Checks that concave areas of inner rings/holes in multipolygons are connected to the outer ring.
 * This is to avoid a situation where you cannot route out of the concave part (of the inner ring).
 * <p>
 * An example of such a geometry is https://www.openstreetmap.org/relation/8513460.
 * <p>
 * There we want to make sure that the node https://www.openstreetmap.org/node/6136980344
 * can be used as the start point of the search, and you can leave the area.
 * <p>
 * Further reading: https://github.com/opentripplanner/OpenTripPlanner/pull/6486
 */
public class ConcaveHoleTest {

  @Test
  void visibilityNodes() {
    var inside0 = node(0, new WgsCoordinate(0, 0));
    var inside1 = node(2, new WgsCoordinate(5, 5));
    var outerRing = List.of(
      inside0,
      node(1, new WgsCoordinate(0, 5)),
      inside1,
      node(4, new WgsCoordinate(5, 0))
    );

    int visibilityNodeId = 107;
    var hole = List.of(
      node(100, new WgsCoordinate(1, 1)),
      node(101, new WgsCoordinate(1, 4)),
      node(102, new WgsCoordinate(4, 4)),
      node(103, new WgsCoordinate(4, 3)),
      node(104, new WgsCoordinate(3, 3)),
      node(105, new WgsCoordinate(3, 2)),
      node(106, new WgsCoordinate(4, 2)),
      // this is the node in the hole that will be connected to the outer ring
      node(visibilityNodeId, new WgsCoordinate(4, 1))
    );

    var outside0 = node(5, new WgsCoordinate(-1, 0));
    var outside1 = node(6, new WgsCoordinate(6, 5));

    var outerRingId = 1000;
    var holeId = 1001;

    var relation = RelationBuilder.ofMultiPolygon()
      .withWayMember(outerRingId, "outer")
      .withWayMember(holeId, "inner")
      .build();

    var provider = TestOsmProvider.of()
      .addAreaFromNodes(outerRingId, outerRing)
      .addAreaFromNodes(holeId, hole)
      .addWayFromNodes(outside0, inside0)
      .addWayFromNodes(outside1, inside1)
      .addRelation(relation)
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

    assertEquals(
      28,
      graph.getEdgesOfType(AreaEdge.class).size(),
      "Incorrect number of edges, check %s".formatted(GeoJsonIo.toUrl(graph))
    );

    var vertex = graph.getVertex(VertexLabel.osm(visibilityNodeId));
    assertThat(vertex.getOutgoing()).hasSize(4);
  }
}

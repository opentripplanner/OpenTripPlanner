package org.opentripplanner.graph_builder.module.osm.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

/**
 * Verifies that a node which is wheelchair-inaccessible but does not restrict any other travel
 * mode (e.g. {@code barrier=kerb} with a raised/rolled kerb) still gets its own
 * {@link BarrierVertex}, even when it sits in the middle of a way rather than at an
 * OSM-level intersection.
 */
class WheelchairBarrierNodeTest {

  private static final long FROM_ID = 1;
  private static final long KERB_ID = 2;
  private static final long TO_ID = 3;

  @Test
  void rawKerbNodeInMiddleOfWayCreatesBarrierVertex() {
    var n1 = node(FROM_ID, new WgsCoordinate(0, 0));
    var kerb = node(KERB_ID, new WgsCoordinate(0, 1))
      .copy()
      .withTag("barrier", "kerb")
      .withTag("kerb", "raised")
      .build();
    var n3 = node(TO_ID, new WgsCoordinate(0, 2));

    var vertex = buildGraphAndGetVertex(n1, kerb, n3);

    var barrierVertex = assertInstanceOf(BarrierVertex.class, vertex);
    assertEquals(Accessibility.NOT_POSSIBLE, barrierVertex.wheelchairAccessibility());
  }

  @Test
  void plainWheelchairNoNodeInMiddleOfWayCreatesBarrierVertex() {
    var n1 = node(FROM_ID, new WgsCoordinate(0, 0));
    var inaccessibleNode = node(KERB_ID, new WgsCoordinate(0, 1))
      .copy()
      .withTag("wheelchair", "no")
      .build();
    var n3 = node(TO_ID, new WgsCoordinate(0, 2));

    var vertex = buildGraphAndGetVertex(n1, inaccessibleNode, n3);

    var barrierVertex = assertInstanceOf(BarrierVertex.class, vertex);
    assertEquals(Accessibility.NOT_POSSIBLE, barrierVertex.wheelchairAccessibility());
  }

  @Test
  void droppedKerbNodeInMiddleOfWayDoesNotCreateAnyVertex() {
    var n1 = node(FROM_ID, new WgsCoordinate(0, 0));
    var kerb = node(KERB_ID, new WgsCoordinate(0, 1))
      .copy()
      .withTag("barrier", "kerb")
      .withTag("kerb", "flush")
      .build();
    var n3 = node(TO_ID, new WgsCoordinate(0, 2));

    var vertex = buildGraphAndGetVertex(n1, kerb, n3);

    assertNull(vertex);
  }

  @Test
  void kerbNodeWithSemicolonSeparatedAccessibleValueDoesNotCreateAnyVertex() {
    var n1 = node(FROM_ID, new WgsCoordinate(0, 0));
    // semicolon-separated values are alternatives: if any one of them is accessible, the
    // whole node is considered accessible.
    var kerb = node(KERB_ID, new WgsCoordinate(0, 1))
      .copy()
      .withTag("barrier", "kerb")
      .withTag("kerb", "lowered;no")
      .build();
    var n3 = node(TO_ID, new WgsCoordinate(0, 2));

    var vertex = buildGraphAndGetVertex(n1, kerb, n3);

    assertNull(vertex);
  }

  private Vertex buildGraphAndGetVertex(OsmNode n1, OsmNode middle, OsmNode n3) {
    var provider = TestOsmProvider.of().addWayFromNodes(n1, middle, n3).build();
    var graph = new Graph();
    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    return graph.getVertex(VertexLabel.osm(middle.getId()));
  }
}

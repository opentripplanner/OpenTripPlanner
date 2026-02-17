package org.opentripplanner.routing.linking.moduletests;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.Scope;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;

class ScopedLinkingTest {

  public static final FeedScopedId AREA_STOP_1 = id("area-stop-1");
  public static final FeedScopedId AREA_STOP_2 = id("area-stop-2");
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.######");
  public static final DecimalFormatSymbols SYMBOLS = DECIMAL_FORMAT.getDecimalFormatSymbols();

  {
    SYMBOLS.setDecimalSeparator('.');
    DECIMAL_FORMAT.setDecimalFormatSymbols(SYMBOLS);
  }

  @Test
  void flex() {
    OTPFeature.FlexRouting.testOn(() -> {
      var v1 = StreetModelForTest.intersectionVertex(0.0, 0.0);
      v1.addAreaStops(Set.of(AREA_STOP_1));
      var v2 = StreetModelForTest.intersectionVertex(0.001, 0.001);
      v2.addAreaStops(Set.of(AREA_STOP_2));

      var toBeLinked = StreetModelForTest.intersectionVertex(0.0005, 0.0006);

      assertThat(toBeLinked.areaStops()).isEmpty();

      StreetModelForTest.streetEdge(v1, v2);

      var graph = new Graph();

      graph.addVertex(v1);
      graph.addVertex(v2);
      graph.index();

      var linker = VertexLinkerTestFactory.of(graph);

      linker.linkVertexPermanently(
        toBeLinked,
        TraverseModeSet.allModes(),
        BIDIRECTIONAL,
        (vertex, streetVertex) ->
          List.of(
            StreetModelForTest.streetEdge((StreetVertex) vertex, streetVertex),
            StreetModelForTest.streetEdge(streetVertex, (StreetVertex) vertex)
          )
      );

      var splitterVertices = graph.getVerticesOfType(SplitterVertex.class);
      assertThat(splitterVertices).hasSize(1);
      var splitter = splitterVertices.getFirst();

      assertThat(splitter.areaStops()).containsExactly(AREA_STOP_1, AREA_STOP_2);
    });
  }

  @Test
  void splitPermanently() {
    var model = buildModel();
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(1);

    model
      .linker()
      .linkVertexPermanently(
        model.split(),
        TraverseModeSet.allModes(),
        BIDIRECTIONAL,
        (vertex, streetVertex) -> List.of(model.edge())
      );

    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(2);
  }

  @Test
  void splitRequestScoped() {
    var model = buildModel();
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(1);
    var temp = model
      .linker()
      .linkVertexForRequest(model.split(), TraverseModeSet.allModes(), BIDIRECTIONAL, (v1, v2) ->
        List.of()
      );
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(2);
    temp.disposeEdges();
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(1);
  }

  @Test
  void splitRealtime() {
    var model = buildModel();
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(1);
    var temp = model
      .linker()
      .linkVertexForRealTime(model.split(), TraverseModeSet.allModes(), BIDIRECTIONAL, (v1, v2) ->
        List.of()
      );
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(2);
    temp.disposeEdges();
    assertThat(model.graph().getEdgesOfType(StreetEdge.class)).hasSize(1);
  }

  @Test
  void multiModeLinking() {
    // test model has 3 parallel horizontal edges, of which uppermost allows car driving
    IntersectionVertex[] vertices = {
      StreetModelForTest.intersectionVertex(0.0, 0.0),
      StreetModelForTest.intersectionVertex(0.01, 0.0),
      StreetModelForTest.intersectionVertex(0.0, 0.0001),
      StreetModelForTest.intersectionVertex(0.01, 0.0001),
      StreetModelForTest.intersectionVertex(0.0, 0.0002),
      StreetModelForTest.intersectionVertex(0.01, 0.0002),
    };

    var walkEdge1 = StreetModelForTest.streetEdge(
      vertices[0],
      vertices[1],
      0.01,
      StreetTraversalPermission.PEDESTRIAN
    );
    var walkEdge2 = StreetModelForTest.streetEdge(
      vertices[2],
      vertices[3],
      0.01,
      StreetTraversalPermission.PEDESTRIAN
    );
    var carEdge = StreetModelForTest.streetEdge(
      vertices[4],
      vertices[5],
      0.01,
      StreetTraversalPermission.CAR
    );

    // link point below all edges, in the middle
    var split = StreetModelForTest.intersectionVertex(0.005, -0.0001);

    var g = new Graph();
    for (IntersectionVertex vertex : vertices) {
      g.addVertex(vertex);
    }
    g.index();
    g.insert(walkEdge1, Scope.PERMANENT);
    g.insert(walkEdge2, Scope.PERMANENT);
    g.insert(carEdge, Scope.PERMANENT);
    assertThat(g.getEdgesOfType(StreetEdge.class)).hasSize(3);
    var linker = VertexLinkerTestFactory.of(g);
    var temp = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      BIDIRECTIONAL,
      (v1, v2) -> List.of()
    );
    // vertex is linked to closest walk edge and to the car edge, not to all 3 edges
    assertThat(summarizeLinks(g)).containsExactly(
      "(0,0) → (0.005,0) PEDESTRIAN ♿✅",
      "(0,0.0002) → (0.005,0.0002) CAR ♿✅"
    );
    temp.disposeEdges();
    assertThat(summarizeLinks(g)).isEmpty();
  }

  private static List<String> summarizeLinks(Graph graph) {
    return graph
      .getEdgesOfType(TemporaryPartialStreetEdge.class)
      .stream()
      .map(e ->
        String.format(
          "%s → %s %s ♿%s",
          summarizeVertex(e.getFromVertex()),
          summarizeVertex(e.getToVertex()),
          e.getPermission(),
          summarizeBoolean(e.isWheelchairAccessible())
        )
      )
      .toList();
  }

  private static String summarizeBoolean(boolean b) {
    if (b) {
      return "✅";
    } else {
      return "❌";
    }
  }

  private static String summarizeVertex(Vertex e) {
    return String.format(
      "(%s,%s)".formatted(DECIMAL_FORMAT.format(e.getLat()), DECIMAL_FORMAT.format(e.getLon()))
    );
  }

  private static TestModel buildModel() {
    var v1 = StreetModelForTest.intersectionVertex(0.0, 0.0);
    var v2 = StreetModelForTest.intersectionVertex(0.1, 0.1);
    var split = StreetModelForTest.intersectionVertex(0.05, 0.05);

    var edge = StreetModelForTest.streetEdge(v1, v2);

    var g = new Graph();
    g.addVertex(v1);
    g.addVertex(v2);
    g.index();
    g.insert(edge, Scope.PERMANENT);
    var linker = VertexLinkerTestFactory.of(g);
    return new TestModel(split, edge, g, linker);
  }

  private record TestModel(
    IntersectionVertex split,
    StreetEdge edge,
    Graph graph,
    VertexLinker linker
  ) {}
}

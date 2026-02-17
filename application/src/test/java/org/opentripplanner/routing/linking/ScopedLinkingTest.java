package org.opentripplanner.routing.linking;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
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
    assertThat(model.graph().listStreetEdges()).hasSize(1);

    model
      .linker()
      .linkVertexPermanently(
        model.split(),
        TraverseModeSet.allModes(),
        BIDIRECTIONAL,
        (vertex, streetVertex) -> List.of(model.edge())
      );

    assertThat(model.graph().listStreetEdges()).hasSize(2);
  }

  @Test
  void splitRequestScoped() {
    var model = buildModel();
    assertThat(model.graph().listStreetEdges()).hasSize(1);
    var temp = model
      .linker()
      .linkVertexForRequest(model.split(), TraverseModeSet.allModes(), BIDIRECTIONAL, (v1, v2) ->
        List.of()
      );
    assertThat(model.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    assertThat(model.graph().listStreetEdges()).hasSize(1);
  }

  @Test
  void splitRealtime() {
    var model = buildModel();
    assertThat(model.graph().listStreetEdges()).hasSize(1);
    var temp = model
      .linker()
      .linkVertexForRealTime(model.split(), TraverseModeSet.allModes(), BIDIRECTIONAL, (v1, v2) ->
        List.of()
      );
    assertThat(model.graph().listStreetEdges()).hasSize(2);
    temp.disposeEdges();
    assertThat(model.graph().listStreetEdges()).hasSize(1);
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
    return new TestModel(split, edge, new GraphDataFetcher(g), linker);
  }

  private record TestModel(
    IntersectionVertex split,
    StreetEdge edge,
    GraphDataFetcher graph,
    VertexLinker linker
  ) {}
}

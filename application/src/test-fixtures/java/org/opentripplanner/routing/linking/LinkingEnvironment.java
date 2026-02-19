package org.opentripplanner.routing.linking;

import static org.opentripplanner.street.model.edge.LinkingDirection.BIDIRECTIONAL;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.routing.graph.DisposableEdgeDataFetcher;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * A reusable environment for linking vertices to the graph. Encapsulates the graph, its fetcher,
 * the linker, and the disposable edge collection and its fetcher.
 */
public class LinkingEnvironment {

  private final Graph graph;
  private final GraphDataFetcher graphFetcher;
  private final VertexLinker linker;

  @Nullable
  private DisposableEdgeCollection disposable;

  public LinkingEnvironment(Graph graph) {
    this.graph = graph;
    this.graphFetcher = new GraphDataFetcher(graph);
    this.linker = VertexLinkerTestFactory.of(graph);
  }

  public LinkingEnvironment(Vertex... vertices) {
    this(new Graph());
    for (var v : vertices) {
      graph.addVertex(v);
    }
    graph.index();
  }

  public DisposableEdgeCollection linkVertexForRequest(double lat, double lon) {
    var split = new TemporaryStreetLocation(new Coordinate(lon, lat), I18NString.of("split"));
    disposable = linker.linkVertexForRequest(
      split,
      TraverseModeSet.allModes(),
      BIDIRECTIONAL,
      (v1, v2) ->
        List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v1, v2))
    );
    return disposable;
  }

  public void disposeEdges() {
    if (disposable != null) {
      disposable.disposeEdges();
    }
  }

  public DisposableEdgeDataFetcher disposable() {
    var t = Objects.requireNonNull(disposable, "Link a vertex before calling this method.");
    return new DisposableEdgeDataFetcher(t);
  }

  public GraphDataFetcher graph() {
    return graphFetcher;
  }

  public VertexLinker linker() {
    return linker;
  }
}

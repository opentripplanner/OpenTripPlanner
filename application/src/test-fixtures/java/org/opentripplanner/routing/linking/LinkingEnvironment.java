package org.opentripplanner.routing.linking;

import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.routing.graph.DisposableEdgeDataFetcher;
import org.opentripplanner.routing.graph.GraphDataFetcher;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * A reusable environment for linking vertices to the graph. Encapsulates the graph, its fetcher,
 * the linker, and the disposable edge collection and its fetcher.
 */
public class LinkingEnvironment {

  private final GraphDataFetcher graphFetcher;
  private final VertexLinker linker;

  @Nullable
  private DisposableEdgeCollection disposable;

  public LinkingEnvironment(Vertex... vertices) {
    var graph = new Graph();
    for (var v : vertices) {
      graph.addVertex(v);
    }
    graph.index();
    graphFetcher = new GraphDataFetcher(graph);
    linker = VertexLinkerTestFactory.of(graph);
  }

  public DisposableEdgeCollection linkVertexForRequest(double lat, double lon) {
    return linkVertexForRequest(lat, lon, TraverseModeSet.allModes(), TraverseModeSet.allModes());
  }

  public DisposableEdgeCollection linkVertexForRequest(
    double lat,
    double lon,
    TraverseModeSet incoming,
    TraverseModeSet outgoing
  ) {
    var split = new TemporaryStreetLocation(new Coordinate(lon, lat), I18NString.of("split"));
    disposable = linker.linkVertexForRequest(
      split,
      incoming,
      outgoing,
      TemporaryFreeEdge::createTemporaryFreeEdge
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

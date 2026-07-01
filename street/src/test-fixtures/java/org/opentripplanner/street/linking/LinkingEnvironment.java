package org.opentripplanner.street.linking;

import static org.opentripplanner.street.linking.LinkingDirection.BIDIRECTIONAL;
import static org.opentripplanner.street.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.DisposableEdgeDataFetcher;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.GraphDataFetcher;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
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
    linker = new VertexLinker(
      graph,
      GeofencingZoneService.EMPTY,
      COMPUTE_AREA_VISIBILITY_LINES,
      StreetConstants.DEFAULT_MAX_AREA_NODES,
      true
    );
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

  public DisposableEdgeCollection linkVertexForRealTime(double lat, double lon) {
    var split = new TemporaryStreetLocation(new Coordinate(lon, lat), I18NString.of("split"));
    disposable = linker.linkVertexForRealTime(
      split,
      TraverseModeSet.allModes(),
      BIDIRECTIONAL,
      (v1, v2) ->
        List.of(TemporaryFreeEdge.createTemporaryFreeEdge((TemporaryStreetLocation) v1, v2))
    );
    return disposable;
  }

  public void linkVertexPermanently(TransitStopVertex linkedVertex) {
    linker.linkVertexPermanently(
      linkedVertex,
      new TraverseModeSet(TraverseMode.WALK),
      BIDIRECTIONAL,
      ((vertex, streetVertex) -> {
        var s = (TransitStopVertex) vertex;
        return List.of(
          StreetTransitStopLink.createStreetTransitStopLink(s, streetVertex),
          StreetTransitStopLink.createStreetTransitStopLink(streetVertex, s)
        );
      })
    );
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

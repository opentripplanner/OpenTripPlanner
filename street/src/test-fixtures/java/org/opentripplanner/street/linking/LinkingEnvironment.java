package org.opentripplanner.street.linking;

import static org.opentripplanner.street.linking.LinkingDirection.BIDIRECTIONAL;
import static org.opentripplanner.street.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.DisposableEdgeDataFetcher;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
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

  private final GraphSummarizer graphFetcher;
  private final VertexLinker linker;

  @Nullable
  private DisposableEdgeCollection disposable;

  public LinkingEnvironment(Vertex... vertices) {
    var graph = new Graph();
    for (var v : vertices) {
      graph.addVertex(v);
    }
    graph.index();
    graphFetcher = new GraphSummarizer(graph);
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

  public GraphSummarizer graph() {
    return graphFetcher;
  }

  public VertexLinker linker() {
    return linker;
  }

  public AreaGroupBuilder areaGroup(IntersectionVertex... boundaryVertices) {
    return new AreaGroupBuilder(List.of(boundaryVertices));
  }

  public final class AreaGroupBuilder {

    private final List<IntersectionVertex> boundaryVertices;
    private Set<IntersectionVertex> visibilityVertices = Set.of();

    private AreaGroupBuilder(List<IntersectionVertex> boundaryVertices) {
      if (boundaryVertices.size() < 3) {
        throw new IllegalArgumentException("An area must have at least three boundary vertices.");
      }
      this.boundaryVertices = boundaryVertices;
    }

    public AreaGroupBuilder withVisibilityVertices(IntersectionVertex... visibilityVertices) {
      this.visibilityVertices = Set.of(visibilityVertices);
      return this;
    }

    public AreaGroup build() {
      var coordinates = new Coordinate[boundaryVertices.size() + 1];
      for (int i = 0; i < boundaryVertices.size(); i++) {
        coordinates[i] = boundaryVertices.get(i).getCoordinate();
      }
      coordinates[boundaryVertices.size()] = coordinates[0];

      var polygon = GeometryUtils.getGeometryFactory().createPolygon(coordinates);
      var areaGroup = new AreaGroup(polygon);
      areaGroup.addVisibilityVertices(visibilityVertices);

      var area = new Area();
      area.setName(I18NString.of("test area"));
      area.setWalkSafety(0.5f);
      area.setBicycleSafety(0.5f);
      area.setPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
      area.setGeometry(polygon);
      areaGroup.addArea(area);

      for (int i = 0; i < boundaryVertices.size(); i++) {
        var from = boundaryVertices.get(i);
        var to = boundaryVertices.get((i + 1) % boundaryVertices.size());
        connectAreaEdge(from, to, areaGroup, false);
        connectAreaEdge(to, from, areaGroup, true);
      }

      graphFetcher.graph().index();
      return areaGroup;
    }

    private void connectAreaEdge(
      IntersectionVertex from,
      IntersectionVertex to,
      AreaGroup areaGroup,
      boolean back
    ) {
      var geometry = GeometryUtils.getGeometryFactory().createLineString(
        new Coordinate[] { from.getCoordinate(), to.getCoordinate() }
      );
      new AreaEdgeBuilder()
        .withFromVertex(from)
        .withToVertex(to)
        .withGeometry(geometry)
        .withName(I18NString.of("area boundary"))
        .withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE)
        .withBack(back)
        .withArea(areaGroup)
        .buildAndConnect();
    }
  }
}

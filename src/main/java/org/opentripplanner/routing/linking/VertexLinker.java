package org.opentripplanner.routing.linking;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.EdgeSpatialIndex;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporarySplitterVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.service.StopModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class links transit stops to streets by splitting the streets (unless the stop is extremely
 * close to the street intersection).
 * <p>
 * It is intended to eventually completely replace the existing stop linking code, which had been
 * through so many revisions and adaptations to different street and turn representations that it
 * was very glitchy. This new code is also intended to be deterministic in linking to streets,
 * independent of the order in which the JVM decides to iterate over Maps and even in the presence
 * of points that are exactly halfway between multiple candidate linking points.
 * <p>
 * It would be wise to keep this new incarnation of the linking code relatively simple, considering
 * what happened before.
 * <p>
 * See discussion in pull request #1922, follow up issue #1934, and the original issue calling for
 * replacement of the stop linker, #1305.
 */
public class VertexLinker {

  private static final Logger LOG = LoggerFactory.getLogger(VertexLinker.class);
  /**
   * if there are two ways and the distances to them differ by less than this value, we link to both
   * of them
   */
  private static final double DUPLICATE_WAY_EPSILON_METERS = 0.001;
  private static final int INITIAL_SEARCH_RADIUS_METERS = 100;
  private static final int MAX_SEARCH_RADIUS_METERS = 1000;
  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();
  /**
   * Spatial index of StreetEdges in the graph.
   */
  private final EdgeSpatialIndex edgeSpatialIndex;

  private final Graph graph;

  private final StopModel stopModel;

  // TODO Temporary code until we refactor WalkableAreaBuilder  (#3152)
  private Boolean addExtraEdgesToAreas = false;

  /**
   * Construct a new VertexLinker. NOTE: Only one VertexLinker should be active on a graph at any
   * given time.
   */
  public VertexLinker(Graph graph, StopModel stopModel, EdgeSpatialIndex edgeSpatialIndex) {
    this.edgeSpatialIndex = edgeSpatialIndex;
    this.graph = graph;
    this.stopModel = stopModel;
  }

  public void linkVertexPermanently(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    BiFunction<Vertex, StreetVertex, List<Edge>> edgeFunction
  ) {
    link(vertex, traverseModes, direction, Scope.PERMANENT, edgeFunction);
  }

  public DisposableEdgeCollection linkVertexForRealTime(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    BiFunction<Vertex, StreetVertex, List<Edge>> edgeFunction
  ) {
    return link(vertex, traverseModes, direction, Scope.REALTIME, edgeFunction);
  }

  public DisposableEdgeCollection linkVertexForRequest(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    BiFunction<Vertex, StreetVertex, List<Edge>> edgeFunction
  ) {
    return link(vertex, traverseModes, direction, Scope.REQUEST, edgeFunction);
  }

  public void removeEdgeFromIndex(Edge edge, Scope scope) {
    // Edges without geometry will not have been added to the index in the first place
    if (edge.getGeometry() != null) {
      edgeSpatialIndex.remove(edge.getGeometry().getEnvelopeInternal(), edge, scope);
    }
  }

  public void removePermanentEdgeFromIndex(Edge edge) {
    removeEdgeFromIndex(edge, Scope.PERMANENT);
  }

  // TODO Temporary code until we refactor WalkableAreaBuilder (#3152)
  public void setAddExtraEdgesToAreas(Boolean addExtraEdgesToAreas) {
    this.addExtraEdgesToAreas = addExtraEdgesToAreas;
  }

  /**
   * While in destructive splitting mode (during graph construction rather than handling routing
   * requests), we remove edges that have been split and may then re-split the resulting segments
   * recursively, so parts of them are also removed. Newly created edge fragments are added to the
   * spatial index; the edges that were split are removed (disconnected) from the graph but were
   * previously not removed from the spatial index, so for all subsequent splitting operations we
   * had to check whether any edge coming out of the spatial index had been "soft deleted".
   * <p>
   * I believe this was compensating for the fact that STRTrees are optimized at construction and
   * read-only. That restriction no longer applies since we've been using our own hash grid spatial
   * index instead of the STRTree. So rather than filtering out soft deleted edges, this is now an
   * assertion that the system behaves as intended, and will log an error if the spatial index is
   * returning edges that have been disconnected from the graph.
   */
  private static boolean edgeReachableFromGraph(Edge edge) {
    boolean edgeReachableFromGraph = edge.getToVertex().getIncoming().contains(edge);
    if (!edgeReachableFromGraph) {
      LOG.error(
        "Edge returned from spatial index is no longer reachable from graph. That is not expected."
      );
    }
    return edgeReachableFromGraph;
  }

  /** projected distance from stop to edge, in latitude degrees */
  private static double distance(Vertex tstop, StreetEdge edge, double xscale) {
    // Despite the fact that we want to use a fast somewhat inaccurate projection, still use JTS library tools
    // for the actual distance calculations.
    LineString transformed = equirectangularProject(edge.getGeometry(), xscale);
    return transformed.distance(
      GEOMETRY_FACTORY.createPoint(new Coordinate(tstop.getLon() * xscale, tstop.getLat()))
    );
  }

  /** project this linestring to an equirectangular projection */
  private static LineString equirectangularProject(LineString geometry, double xScale) {
    Coordinate[] coords = new Coordinate[geometry.getNumPoints()];

    for (int i = 0; i < coords.length; i++) {
      Coordinate c = geometry.getCoordinateN(i);
      c = (Coordinate) c.clone();
      c.x *= xScale;
      coords[i] = c;
    }

    return GEOMETRY_FACTORY.createLineString(coords);
  }

  /**
   * This method will link the provided vertex into the street graph. This may involve splitting an
   * existing edge (if the scope is not PERMANENT, the existing edge will be kept).
   * <p>
   * In OTP2 where the transit search can be quite fast, searching for a good linking point can be a
   * significant fraction of response time. Hannes Junnila has reported >70% speedups in searches by
   * making the search radius smaller. Therefore we use an expanding-envelope search, which is more
   * efficient in dense areas.
   *
   * @param vertex        Vertex to be linked into the street graph
   * @param traverseModes Only street edges allowing one of these modes will be linked
   * @param direction     The direction of the new edges to be created
   * @param scope         The scope of the split
   * @param edgeFunction  How the provided vertex should be linked into the street graph
   * @return A DisposableEdgeCollection with edges created by this method. It is the caller's
   * responsibility to call the dispose method on this object when the edges are no longer needed.
   */
  private DisposableEdgeCollection link(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    Scope scope,
    BiFunction<Vertex, StreetVertex, List<Edge>> edgeFunction
  ) {
    DisposableEdgeCollection tempEdges = (scope != Scope.PERMANENT)
      ? new DisposableEdgeCollection(graph, scope)
      : null;

    try {
      Set<StreetVertex> streetVertices = linkToStreetEdges(
        vertex,
        traverseModes,
        direction,
        scope,
        INITIAL_SEARCH_RADIUS_METERS,
        tempEdges
      );
      if (streetVertices.isEmpty()) {
        streetVertices =
          linkToStreetEdges(
            vertex,
            traverseModes,
            direction,
            scope,
            MAX_SEARCH_RADIUS_METERS,
            tempEdges
          );
      }

      for (StreetVertex streetVertex : streetVertices) {
        List<Edge> edges = edgeFunction.apply(vertex, streetVertex);
        if (tempEdges != null) {
          for (Edge edge : edges) {
            tempEdges.addEdge(edge);
          }
        }
      }
    } catch (Exception e) {
      if (tempEdges != null) {
        tempEdges.disposeEdges();
      }
      throw e;
    }

    return tempEdges;
  }

  private Set<StreetVertex> linkToStreetEdges(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    Scope scope,
    int radiusMeters,
    DisposableEdgeCollection tempEdges
  ) {
    final double radiusDeg = SphericalDistanceLibrary.metersToDegrees(radiusMeters);

    Envelope env = new Envelope(vertex.getCoordinate());

    // Perform a simple local equirectangular projection, so distances are expressed in degrees latitude.
    final double xscale = Math.cos(vertex.getLat() * Math.PI / 180);

    // Expand more in the longitude direction than the latitude direction to account for converging meridians.
    env.expandBy(radiusDeg / xscale, radiusDeg);

    // Perform several transformations at once on the edges returned by the index. Only consider
    // street edges traversable by at least one of the given modes and are still present in the
    // graph. Calculate a distance to each of those edges, and keep only the ones within the search
    // radius.
    List<DistanceTo<StreetEdge>> candidateEdges = edgeSpatialIndex
      .query(env, scope)
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .filter(e -> e.canTraverse(traverseModes) && edgeReachableFromGraph(e))
      .map(e -> new DistanceTo<>(e, distance(vertex, e, xscale)))
      .filter(ead -> ead.distanceDegreesLat < radiusDeg)
      .collect(Collectors.toList());

    if (candidateEdges.isEmpty()) {
      return Set.of();
    }

    Set<DistanceTo<StreetEdge>> closesEdges = getClosestEdgesPerMode(traverseModes, candidateEdges);

    return closesEdges
      .stream()
      .map(ce -> link(vertex, ce.item, xscale, scope, direction, tempEdges))
      .collect(Collectors.toSet());
  }

  /**
   * We need to get the closest edges per mode to be sure that we are linking to edges traversable
   * by all the specified modes. We use a set here to avoid duplicates in the case that edges are
   * traversable by more than one of the modes specified.
   */
  private Set<DistanceTo<StreetEdge>> getClosestEdgesPerMode(
    TraverseModeSet traverseModeSet,
    List<DistanceTo<StreetEdge>> candidateEdges
  ) {
    final double DUPLICATE_WAY_EPSILON_DEGREES = SphericalDistanceLibrary.metersToDegrees(
      DUPLICATE_WAY_EPSILON_METERS
    );

    // The following logic has gone through several different versions using different approaches.
    // The core idea is to find all edges that are roughly the same distance from the given vertex, which will
    // catch things like superimposed edges going in opposite directions.
    // First, all edges within DUPLICATE_WAY_EPSILON_METERS of of the best distance were selected.
    // More recently, the edges were sorted in order of increasing distance, and all edges in the list were selected
    // up to the point where a distance increase of DUPLICATE_WAY_EPSILON_DEGREES from one edge to the next.
    // This was in response to concerns about arbitrary cutoff distances: at any distance, it's always possible
    // one half of a dual carriageway (or any other pair of edges in opposite directions) will be caught and the
    // other half lost. It seems like this was based on some incorrect premises about floating point calculations
    // being non-deterministic.

    Set<DistanceTo<StreetEdge>> closesEdges = new HashSet<>();
    for (TraverseMode mode : traverseModeSet.getModes()) {
      TraverseModeSet modeSet = new TraverseModeSet(mode);
      // There is at least one appropriate edge within range.

      var candidateEdgesForMode = candidateEdges
        .stream()
        .filter(e -> e.item.canTraverse(modeSet))
        .toList();

      if (candidateEdgesForMode.isEmpty()) {
        continue;
      }

      double closestDistance = candidateEdgesForMode
        .stream()
        .mapToDouble(ce -> ce.distanceDegreesLat)
        .min()
        .getAsDouble();

      // Because this is a set, each instance of DistanceTo<StreetEdge> will only be added once
      closesEdges.addAll(
        candidateEdges
          .stream()
          .filter(ce -> ce.distanceDegreesLat <= closestDistance + DUPLICATE_WAY_EPSILON_DEGREES)
          .collect(Collectors.toSet())
      );
    }
    return closesEdges;
  }

  /** Split the edge if necessary return the closest vertex */
  private StreetVertex link(
    Vertex vertex,
    StreetEdge edge,
    double xScale,
    Scope scope,
    LinkingDirection direction,
    DisposableEdgeCollection tempEdges
  ) {
    // TODO: we've already built this line string, we should save it
    LineString orig = edge.getGeometry();
    LineString transformed = equirectangularProject(orig, xScale);
    LocationIndexedLine il = new LocationIndexedLine(transformed);
    LinearLocation ll = il.project(new Coordinate(vertex.getLon() * xScale, vertex.getLat()));
    double length = SphericalDistanceLibrary.length(orig);

    // if we're very close to one end of the line or the other, or endwise, don't bother to split,
    // cut to the chase and link directly
    // We use a really tiny epsilon here because we only want points that actually snap to exactly the same location on the
    // street to use the same vertices. Otherwise the order the stops are loaded in will affect where they are snapped.
    if (
      ll.getSegmentIndex() == 0 &&
      (ll.getSegmentFraction() < 1e-8 || ll.getSegmentFraction() * length < 0.1)
    ) {
      return (StreetVertex) edge.getFromVertex();
    }
    // -1 converts from count to index. Because of the fencepost problem, npoints - 1 is the "segment"
    // past the last point
    else if (ll.getSegmentIndex() == orig.getNumPoints() - 1) {
      return (StreetVertex) edge.getToVertex();
    }
    // nPoints - 2: -1 to correct for index vs count, -1 to account for fencepost problem
    else if (
      ll.getSegmentIndex() == orig.getNumPoints() - 2 &&
      (ll.getSegmentFraction() > 1 - 1e-8 || (1 - ll.getSegmentFraction()) * length < 0.1)
    ) {
      return (StreetVertex) edge.getToVertex();
    } else {
      // split the edge, get the split vertex
      SplitterVertex v0 = split(edge, ll, scope, direction, tempEdges);

      // If splitter vertex is part of area; link splittervertex to all other vertexes in area, this creates
      // edges that were missed by WalkableAreaBuilder
      // TODO Temporary code until we refactor the WalkableAreaBuilder (#3152)
      if (scope == Scope.PERMANENT && this.addExtraEdgesToAreas && edge instanceof AreaEdge) {
        ((AreaEdge) edge).getArea().addVertex(v0);
      }

      // TODO Consider moving this code
      if (OTPFeature.FlexRouting.isOn()) {
        FlexLocationAdder.addFlexLocations(edge, v0, stopModel);
      }

      return v0;
    }
  }

  /**
   * Split the street edge at the given fraction
   *
   * @param originalEdge to be split
   * @param ll           fraction at which to split the edge
   * @param scope        the scope of the split
   * @param direction    what direction to link the edges
   * @param tempEdges    collection of temporary edges
   * @return Splitter vertex with added new edges
   */
  private SplitterVertex split(
    StreetEdge originalEdge,
    LinearLocation ll,
    Scope scope,
    LinkingDirection direction,
    DisposableEdgeCollection tempEdges
  ) {
    LineString geometry = originalEdge.getGeometry();

    // create the geometries
    Coordinate splitPoint = ll.getCoordinate(geometry);

    SplitterVertex v;
    String uniqueSplitLabel = "split_" + graph.nextSplitNumber++;

    if (scope != Scope.PERMANENT) {
      TemporarySplitterVertex tsv = new TemporarySplitterVertex(
        uniqueSplitLabel,
        splitPoint.x,
        splitPoint.y,
        originalEdge,
        direction == LinkingDirection.OUTGOING
      );
      tsv.setWheelchairAccessible(originalEdge.isWheelchairAccessible());
      v = tsv;
    } else {
      v =
        new SplitterVertex(
          graph,
          uniqueSplitLabel,
          splitPoint.x,
          splitPoint.y,
          originalEdge.getName()
        );
    }

    // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
    // existing vertices
    var newEdges = scope == Scope.PERMANENT
      ? originalEdge.splitDestructively(v)
      : originalEdge.splitNonDestructively(v, tempEdges, direction);

    if (scope == Scope.REALTIME || scope == Scope.PERMANENT) {
      // update indices of new edges
      if (newEdges.head() != null) {
        edgeSpatialIndex.insert(newEdges.head().getGeometry(), newEdges.head(), scope);
      }
      if (newEdges.tail() != null) {
        edgeSpatialIndex.insert(newEdges.tail().getGeometry(), newEdges.tail(), scope);
      }

      if (scope == Scope.PERMANENT) {
        // remove original edges from the spatial index
        // This iterates over the entire rectangular envelope of the edge rather than the segments making it up.
        // It will be inefficient for very long edges, but creating a new remove method mirroring the more efficient
        // insert logic is not trivial and would require additional testing of the spatial index.
        removeEdgeFromIndex(originalEdge, scope);
        // remove original edge from the graph
        graph.removeEdge(originalEdge);
      }
    }

    return v;
  }

  private static class DistanceTo<T> {

    T item;
    // Possible optimization: store squared lat to skip thousands of sqrt operations
    // However we're using JTS distance functions that probably won't allow us to skip the final sqrt call.
    double distanceDegreesLat;

    public DistanceTo(T item, double distanceDegreesLat) {
      this.item = item;
      this.distanceDegreesLat = distanceDegreesLat;
    }

    @Override
    public int hashCode() {
      return Objects.hash(item);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DistanceTo<?> that = (DistanceTo<?>) o;
      return Objects.equals(item, that.item);
    }
  }

  private record StreetEdgePair(StreetEdge e0, StreetEdge e1) {}
}

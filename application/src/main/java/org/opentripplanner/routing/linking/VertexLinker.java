package org.opentripplanner.routing.linking;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporarySplitterVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
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
  private static final double DUPLICATE_WAY_EPSILON_DEGREES =
    SphericalDistanceLibrary.metersToDegrees(0.001);

  /**
   * Minimal distance for considering two nodes the same
   */
  private static final double DUPLICATE_NODE_EPSILON_DEGREES_SQUARED =
    SphericalDistanceLibrary.metersToDegrees(1) * SphericalDistanceLibrary.metersToDegrees(1);

  private static final double INITIAL_SEARCH_RADIUS_DEGREES =
    SphericalDistanceLibrary.metersToDegrees(100);
  private static final double MAX_SEARCH_RADIUS_DEGREES = SphericalDistanceLibrary.metersToDegrees(
    1000
  );
  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  private static final Set<TraverseMode> NO_THRU_MODES = Set.of(
    TraverseMode.WALK,
    TraverseMode.BICYCLE,
    TraverseMode.CAR
  );

  private final Graph graph;

  private final VertexFactory vertexFactory;

  private boolean areaVisibility = true;
  private int maxAreaNodes = StreetConstants.DEFAULT_MAX_AREA_NODES;

  /**
   * Construct a new VertexLinker. NOTE: Only one VertexLinker should be active on a graph at any
   * given time.
   */
  public VertexLinker(Graph graph) {
    this.graph = graph;
    this.vertexFactory = new VertexFactory(graph);
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

  private void removeEdgeFromIndex(Edge edge, Scope scope) {
    // Edges without geometry will not have been added to the index in the first place
    if (edge.getGeometry() != null) {
      graph.removeEdge(edge, scope);
    }
  }

  public void setAreaVisibility(boolean areaVisibility) {
    this.areaVisibility = areaVisibility;
  }

  public void setMaxAreaNodes(int maxAreaNodes) {
    this.maxAreaNodes = maxAreaNodes;
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
        INITIAL_SEARCH_RADIUS_DEGREES,
        tempEdges
      );
      if (streetVertices.isEmpty()) {
        streetVertices = linkToStreetEdges(
          vertex,
          traverseModes,
          direction,
          scope,
          MAX_SEARCH_RADIUS_DEGREES,
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

  /**
   * Link a boarding location vertex to specific street edges.
   * <p>
   * This is used if a platform is mapped as a linear way, where the given edges form the platform.
   */
  public Set<StreetVertex> linkToSpecificStreetEdgesPermanently(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    Set<StreetEdge> edges
  ) {
    var xscale = getXscale(vertex);
    return linkToCandidateEdges(
      vertex,
      traverseModes,
      direction,
      Scope.PERMANENT,
      null,
      edges.stream().map(e -> new DistanceTo<>(e, distance(vertex, e, xscale))).toList(),
      xscale
    );
  }

  private Set<StreetVertex> linkToStreetEdges(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    Scope scope,
    double radiusDeg,
    @Nullable DisposableEdgeCollection tempEdges
  ) {
    Envelope env = new Envelope(vertex.getCoordinate());

    // Perform a simple local equirectangular projection, so distances are expressed in degrees latitude.
    final double xscale = getXscale(vertex);

    // Expand more in the longitude direction than the latitude direction to account for converging meridians.
    env.expandBy(radiusDeg / xscale, radiusDeg);

    // Perform several transformations at once on the edges returned by the index. Only consider
    // street edges traversable by at least one of the given modes and are still present in the
    // graph. Calculate a distance to each of those edges, and keep only the ones within the search
    // radius.
    List<DistanceTo<StreetEdge>> candidateEdges = graph
      .findEdges(env, scope)
      .stream()
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .filter(e -> e.canTraverse(traverseModes) && e.isReachableFromGraph())
      .map(e -> new DistanceTo<>(e, distance(vertex, e, xscale)))
      .filter(ead -> ead.distanceDegreesLat < radiusDeg)
      .toList();

    return linkToCandidateEdges(
      vertex,
      traverseModes,
      direction,
      scope,
      tempEdges,
      candidateEdges,
      xscale
    );
  }

  private static double getXscale(Vertex vertex) {
    return Math.cos((vertex.getLat() * Math.PI) / 180);
  }

  private Set<StreetVertex> linkToCandidateEdges(
    Vertex vertex,
    TraverseModeSet traverseModes,
    LinkingDirection direction,
    Scope scope,
    @Nullable DisposableEdgeCollection tempEdges,
    List<DistanceTo<StreetEdge>> candidateEdges,
    double xscale
  ) {
    if (candidateEdges.isEmpty()) {
      return Set.of();
    }
    Set<DistanceTo<StreetEdge>> closestEdges = getClosestEdgesPerMode(
      traverseModes,
      candidateEdges
    );
    HashMap<AreaGroup, IntersectionVertex> linkedAreas = new HashMap<>();
    return closestEdges
      .stream()
      .map(ce -> snapAndLink(vertex, ce.item, xscale, scope, direction, tempEdges, linkedAreas))
      .filter(Objects::nonNull)
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
    // The following logic has gone through several different versions using different approaches.
    // The core idea is to find all edges that are roughly the same distance from the given vertex, which will
    // catch things like superimposed edges going in opposite directions.
    // First, all edges within INITIAL_SEARCH_RADIUS_DEGREES of of the best distance were selected.
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

  /* Snap a vertex to and edge if necessary, create required linking and return the applied entry vertex */
  private StreetVertex snapAndLink(
    Vertex vertex,
    StreetEdge edge,
    double xScale,
    Scope scope,
    LinkingDirection direction,
    DisposableEdgeCollection tempEdges,
    HashMap<AreaGroup, IntersectionVertex> linkedAreas
  ) {
    IntersectionVertex start = null;

    // Always consider linking to closest point on the edge
    IntersectionVertex split = findSplitVertex(vertex, edge, xScale, scope, direction, tempEdges);

    // check if vertex is inside an area
    if (this.areaVisibility && edge instanceof AreaEdge aEdge) {
      AreaGroup ag = aEdge.getArea();
      // is area already linked ?
      start = linkedAreas.get(ag);
      if (start == null) {
        if (ag.getGeometry().contains(GEOMETRY_FACTORY.createPoint(vertex.getCoordinate()))) {
          // vertex is inside an area
          if (distSquared(vertex, split) <= DUPLICATE_NODE_EPSILON_DEGREES_SQUARED) {
            // vertex is so close to the edge that we can use the split point directly
            start = split;
          } else {
            if (vertex instanceof IntersectionVertex iv) {
              start = iv;
            } else {
              start = createSplitVertex(aEdge, scope, vertex.getLon(), vertex.getLat());
            }
            linkedAreas.put(ag, start);
          }
        }
      }
      if (start != null && start != split) {
        // vertex is inside the area. try connecting the vertex to the edge's split point, because
        // connections to visibility vertices may fail or do not always provide an optimal route
        // note that by definition, connection to closest edge cannot be blocked and edge can be forced
        addVisibilityEdges(start, split, ag, scope, tempEdges, true);
      } else {
        // vertex is outside an area. Use split point for area connections
        start = split;
      }
      // connect start point to area visibility points to achieve optimal paths
      if (!ag.visibilityVertices().contains(start)) {
        addAreaVertex(start, ag, scope, tempEdges, false);
      }
    } else {
      start = split;
    }

    if (OTPFeature.FlexRouting.isOn()) {
      var areaStops = Stream.concat(start.getIncoming().stream(), start.getOutgoing().stream())
        .flatMap(e ->
          Stream.concat(
            e.getFromVertex().areaStops().stream(),
            e.getToVertex().areaStops().stream()
          )
        )
        .toList();
      start.addAreaStops(areaStops);
    }

    return start;
  }

  /**
   * Check if an edge needs splitting and split if necessary
   */
  private IntersectionVertex findSplitVertex(
    Vertex vertex,
    StreetEdge edge,
    double xScale,
    Scope scope,
    LinkingDirection direction,
    DisposableEdgeCollection tempEdges
  ) {
    LineString geom = edge.getGeometry();
    LineString transformed = equirectangularProject(geom, xScale);
    LocationIndexedLine il = new LocationIndexedLine(transformed);
    LinearLocation ll = il.project(new Coordinate(vertex.getLon() * xScale, vertex.getLat()));
    double length = SphericalDistanceLibrary.length(geom);

    // if we're very close to one end of the edge, don't split
    if (
      ll.getSegmentIndex() == 0 &&
      (ll.getSegmentFraction() < 1e-8 || ll.getSegmentFraction() * length < 0.1)
    ) {
      return (IntersectionVertex) edge.getFromVertex();
    } else if (ll.getSegmentIndex() == geom.getNumPoints() - 1) {
      return (IntersectionVertex) edge.getToVertex();
    } else if (
      ll.getSegmentIndex() == geom.getNumPoints() - 2 &&
      (ll.getSegmentFraction() > 1 - 1e-8 || (1 - ll.getSegmentFraction()) * length < 0.1)
    ) {
      return (IntersectionVertex) edge.getToVertex();
    }
    // split the edge and return the split vertex
    return split(edge, ll.getCoordinate(geom), scope, direction, tempEdges);
  }

  /**
   * Split the street edge at the given fraction
   *
   * @param originalEdge to be split
   * @param scope        the scope of the split
   * @param direction    what direction to link the edges
   * @param tempEdges    collection of temporary edges
   * @return Splitter vertex with added new edges
   */
  private SplitterVertex split(
    StreetEdge originalEdge,
    Coordinate splitPoint,
    Scope scope,
    LinkingDirection direction,
    DisposableEdgeCollection tempEdges
  ) {
    SplitterVertex v = createSplitVertex(originalEdge, scope, splitPoint.x, splitPoint.y);

    // Split the 'edge' at 'v' in 2 new edges and connect these 2 edges to the
    // existing vertices
    var newEdges = scope == Scope.PERMANENT
      ? originalEdge.splitDestructively(v)
      : originalEdge.splitNonDestructively(v, tempEdges, direction);

    if (scope == Scope.REALTIME || scope == Scope.PERMANENT) {
      // update indices of new edges
      if (newEdges.head() != null) {
        graph.insert(newEdges.head(), scope);
      }
      if (newEdges.tail() != null) {
        graph.insert(newEdges.tail(), scope);
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

  private SplitterVertex createSplitVertex(
    StreetEdge originalEdge,
    Scope scope,
    double x,
    double y
  ) {
    SplitterVertex v;
    String uniqueSplitLabel = "split_" + graph.nextSplitNumber++;

    if (scope != Scope.PERMANENT) {
      TemporarySplitterVertex tsv = new TemporarySplitterVertex(
        uniqueSplitLabel,
        x,
        y,
        originalEdge
      );
      tsv.setWheelchairAccessible(originalEdge.isWheelchairAccessible());
      v = tsv;
    } else {
      v = vertexFactory.splitter(originalEdge, x, y, uniqueSplitLabel);
    }
    v.addRentalRestriction(originalEdge.getFromVertex().rentalRestrictions());
    v.addRentalRestriction(originalEdge.getToVertex().rentalRestrictions());

    return v;
  }

  private static class DistanceTo<T> {

    T item;
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

  /**
   * Link a new vertex permanently with area geometry
   */
  public boolean addPermanentAreaVertex(IntersectionVertex newVertex, AreaGroup areaGroup) {
    return addAreaVertex(newVertex, areaGroup, Scope.PERMANENT, null, true);
  }

  /**
   * Inaccurate but fast distance estimate for sorting
   */
  private double distSquared(Vertex a, Vertex b) {
    var aco = a.getCoordinate();
    var bco = b.getCoordinate();
    aco.x -= bco.x;
    aco.y -= bco.y;
    return aco.x * aco.x + aco.y * aco.y;
  }

  /**
   * Add a vertex to an area. This creates edges to all visibility vertices
   * unless those edges would cross one of the area boundary edges
   */
  private boolean addAreaVertex(
    IntersectionVertex newVertex,
    AreaGroup areaGroup,
    Scope scope,
    DisposableEdgeCollection tempEdges,
    boolean force
  ) {
    Geometry polygon = areaGroup.getGeometry();

    int added = 0;

    var visibilityVertices = areaGroup.visibilityVertices();
    if (scope != Scope.PERMANENT) {
      // heuristics to ensure reasonable computation time
      // The more complex the area polygon is, the less visibility connections should be tried
      var areaComplexity = polygon.getNumPoints();
      var totalCount = visibilityVertices.size();
      // take min. 6 closest visibility points
      var appliedCount = (long) Math.max(
        6,
        Math.floor((2 * maxAreaNodes * maxAreaNodes) / areaComplexity)
      );
      if (appliedCount < totalCount) {
        visibilityVertices = visibilityVertices
          .stream()
          .sorted((v1, v2) -> Double.compare(distSquared(v1, newVertex), distSquared(v2, newVertex))
          )
          .limit(appliedCount)
          .collect(Collectors.toSet());
      }
    }
    for (IntersectionVertex v : visibilityVertices) {
      if (addVisibilityEdges(newVertex, v, areaGroup, scope, tempEdges, false)) {
        added++;
      }
    }
    // return false if new vertex could not be connected without intersecting the boundary
    // this happens when the added vertex is outside the area or all visibility edges are blocked
    if (added == 0) {
      if (force) {
        // link with nearest visibility vertex which does not overlap
        var nearest = areaGroup
          .visibilityVertices()
          .stream()
          .filter(v -> distSquared(v, newVertex) >= DUPLICATE_NODE_EPSILON_DEGREES_SQUARED)
          .sorted((v1, v2) -> Double.compare(distSquared(v1, newVertex), distSquared(v2, newVertex))
          )
          .findFirst();
        if (!nearest.isPresent()) {
          // This can happen when all (probably the single one) visibility points are very close
          // to the linked vertex. Such situation can arise in boarding location linking which skips
          // the snapping logic of normal linking and calls addPermanentAreaVertex directly
          nearest = areaGroup.visibilityVertices().stream().findFirst();
        }
        if (nearest.isPresent()) {
          return addVisibilityEdges(newVertex, nearest.get(), areaGroup, scope, tempEdges, true);
        }
      }
      return false;
    } else if (scope == Scope.PERMANENT) {
      // marking the new vertex as visibilityVertex enables direct connections to it
      areaGroup.addVisibilityVertices(Set.of(newVertex));
    }
    return true;
  }

  private static Set<TraverseMode> getNoThruModes(Collection<Edge> edges) {
    var modes = new HashSet<>(NO_THRU_MODES);
    for (Edge e : edges) {
      if (e instanceof StreetEdge se) {
        for (TraverseMode tm : NO_THRU_MODES) {
          if (!se.isNoThruTraffic(tm)) {
            modes.remove(tm);
          }
        }
      }
    }
    return modes;
  }

  /* Check if an edge candiate does not cross the area boundary and add it if it does not */
  private boolean addVisibilityEdges(
    IntersectionVertex from,
    IntersectionVertex to,
    AreaGroup ag,
    Scope scope,
    DisposableEdgeCollection tempEdges,
    boolean force
  ) {
    // Check that vertices are not yet linked
    if (from.isConnected(to)) {
      return true;
    }
    // Check that vertices are not overlapping
    if (!force && distSquared(from, to) < DUPLICATE_NODE_EPSILON_DEGREES_SQUARED) {
      return false;
    }
    LineString line = GEOMETRY_FACTORY.createLineString(
      new Coordinate[] { from.getCoordinate(), to.getCoordinate() }
    );
    // ensure that new edge does not leave the bounds of the area or hit any holes
    if (!force && !ag.getGeometry().contains(line)) {
      return false;
    }
    // add connecting edges
    createEdges(line, from, to, ag, scope, tempEdges);

    return true;
  }

  /* Create a new visibility edge pair within an AreaGroup */
  private void createEdges(
    LineString line,
    IntersectionVertex from,
    IntersectionVertex to,
    AreaGroup ag,
    Scope scope,
    DisposableEdgeCollection tempEdges
  ) {
    Area hit = null;
    var areas = ag.getAreas();
    if (areas.size() == 1) {
      hit = areas.getFirst();
    } else {
      // If more than one area intersects, we pick first one for the name & properties
      for (Area area : areas) {
        Geometry polygon = area.getGeometry();
        Geometry intersection = polygon.intersection(line);
        if (intersection.getLength() > 0.000001) {
          hit = area;
          break;
        }
      }
    }
    // hit may be null when force linking a point from outside
    if (hit == null) {
      LOG.warn("No intersecting area found. This may indicate a bug.");
      hit = areas.getFirst();
    }
    double length = SphericalDistanceLibrary.distance(to.getCoordinate(), from.getCoordinate());
    // apply consistent NoThru restrictions
    // if all joining edges are nothru, then the new edge should be as well
    var incomingNoThruModes = getNoThruModes(to.getIncoming());
    var outgoingNoThruModes = getNoThruModes(to.getIncoming());
    AreaEdgeBuilder areaEdgeBuilder = new AreaEdgeBuilder()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(line)
      .withName(hit.getName())
      .withMeterLength(length)
      .withPermission(hit.getPermission())
      .withBack(false)
      .withArea(ag);
    for (TraverseMode tm : outgoingNoThruModes) {
      areaEdgeBuilder.withNoThruTrafficTraverseMode(tm);
    }
    AreaEdge areaEdge = areaEdgeBuilder.buildAndConnect();
    if (scope != Scope.PERMANENT) {
      tempEdges.addEdge(areaEdge);
    }

    AreaEdgeBuilder reverseAreaEdgeBuilder = new AreaEdgeBuilder()
      .withFromVertex(to)
      .withToVertex(from)
      .withGeometry(line.reverse())
      .withName(hit.getName())
      .withMeterLength(length)
      .withPermission(hit.getPermission())
      .withBack(true)
      .withArea(ag);
    for (TraverseMode tm : incomingNoThruModes) {
      reverseAreaEdgeBuilder.withNoThruTrafficTraverseMode(tm);
    }
    AreaEdge reverseAreaEdge = reverseAreaEdgeBuilder.buildAndConnect();
    if (scope != Scope.PERMANENT) {
      tempEdges.addEdge(reverseAreaEdge);
    }
  }
}

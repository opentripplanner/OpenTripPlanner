package org.opentripplanner.street.linking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import org.opentripplanner.street.Scope;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.TemporarySplitterVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.utils.collection.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class links any vertices to streets by splitting the streets (unless the vertex is extremely
 * close to the street intersection). The linking can be done permanently, or temporarily for the
 * scope of a request or for a real-time update.
 * <p>
 * This class is intended to be deterministic in linking to streets, independent of the order in
 * which the JVM decides to iterate over Maps and even in the presence of points that are exactly
 * halfway between multiple candidate linking points.
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
   * Edge - area intersection often tests edges which start/end at area edge.
   * Shrink egde slightly to avoid accuracy errors
   */
  private static final double AREA_INTERSECTION_SHRINKING = 0.0001;

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

  private final VisibilityMode visibilityMode;
  private final int maxAreaNodes;
  private final boolean shouldLinkFlex;

  /**
   * Construct a new VertexLinker. NOTE: Only one VertexLinker should be active on a graph at any
   * given time.
   */
  public VertexLinker(
    Graph graph,
    VisibilityMode visibilityMode,
    int maxAreaNodes,
    boolean linkFlex
  ) {
    this.graph = Objects.requireNonNull(graph);
    this.visibilityMode = Objects.requireNonNull(visibilityMode);
    this.maxAreaNodes = maxAreaNodes;
    this.shouldLinkFlex = linkFlex;
  }

  /**
   * Attempts to link the given vertex to the street network permanently through two or more links
   * that allow the vertex to be visited from the street network and vice versa.
   *
   * @param modes       Linking is done to one (sometimes more if there are multiple options within
   *                    almost identical distance) of the nearest street edges for each mode (i.e.
   *                    traversal with the mode needs to be allowed). Sometimes links allow multiple
   *                    traversal modes and sometimes each mode gets its own link to a different
   *                    edge. There is also a risk that we cannot find edges nearby that allow
   *                    traversal with one or more of the given modes.
   * @param edgeCreator Used to create linking edges between the given vertex and the street
   *                    network. The same edge creator is used for both directions of linking, so it
   *                    needs to be able to handle both cases (i.e. the from and to vertex will
   *                    switch places).
   */
  public void linkVertexBidirectionallyPermanently(
    Vertex vertex,
    Set<TraverseMode> modes,
    EdgeCreator edgeCreator
  ) {
    var traverseModeSets = modes.stream().map(TraverseModeSet::new).collect(Collectors.toSet());
    link(vertex, traverseModeSets, traverseModeSets, Scope.PERMANENT, edgeCreator);
  }

  /**
   * Attempts to link the given vertex to the street network permanently through two or more links
   * that allow the vertex to be visited from the street network and vice versa.
   *
   * @param modes               Linking is done to one (sometimes more if there are multiple options
   *                            within almost identical distance) of the nearest street edges for
   *                            each mode (i.e. traversal with the mode needs to be allowed).
   *                            Sometimes links allow multiple traversal modes and sometimes each
   *                            mode gets its own link to a different edge. There is also a risk
   *                            that we cannot find edges nearby that allow traversal with one or
   *                            more of the given modes.
   * @param incomingEdgeCreator Used to create links between splitter vertices and the given vertex.
   *                            The from location here is the splitter vertex and the to location is
   *                            the input vertex.
   * @param outgoingEdgeCreator Used to create links between the given vertex and splitter vertices.
   *                            The from location here is the input vertex and the to location is
   *                            the splitter vertex.
   */
  public void linkVertexBidirectionallyPermanently(
    Vertex vertex,
    Set<TraverseMode> modes,
    EdgeCreator incomingEdgeCreator,
    EdgeCreator outgoingEdgeCreator
  ) {
    var traverseModeSets = modes.stream().map(TraverseModeSet::new).collect(Collectors.toSet());
    link(
      vertex,
      traverseModeSets,
      traverseModeSets,
      Scope.PERMANENT,
      incomingEdgeCreator,
      outgoingEdgeCreator
    );
  }

  /**
   * Attempts to link the given vertex originating from a real-time update to the street network
   * temporarily through two links or more links that allow the vertex to be visited from the street
   * network and vice versa.
   *
   * @param modes       Linking is done to one (sometimes more if there are multiple options within
   *                    almost identical distance) of the nearest street edges for each mode (i.e.
   *                    traversal with the mode needs to be allowed). Sometimes links allow multiple
   *                    traversal modes and sometimes each mode gets its own link to a different
   *                    edge. There is also a risk that we cannot find edges nearby that allow
   *                    traversal with one or more of the given modes.
   * @param edgeCreator Used to create linking edges between the given vertex and the street
   *                    network. The same edge creator is used for both directions of linking, so it
   *                    needs to be able to handle both cases (i.e. the from and to vertex will
   *                    switch places).
   */
  public DisposableEdgeCollection linkVertexBidirectionallyForRealTime(
    Vertex vertex,
    Set<TraverseMode> modes,
    EdgeCreator edgeCreator
  ) {
    var traverseModeSets = modes.stream().map(TraverseModeSet::new).collect(Collectors.toSet());
    return link(vertex, traverseModeSets, traverseModeSets, Scope.REALTIME, edgeCreator);
  }

  /**
   * Attempts to link the given vertex to the street network temporarily (for the duration of a
   * request) through one or more links that allow the vertex to be visited from the street network
   * and vice versa.
   *
   * @param incomingModes Linking is done from one (sometimes more if there are multiple options
   *                      within almost identical distance) of the nearest street edges for each
   *                      mode set (i.e. traversal with at least one of the modes in the
   *                      {@link TraverseModeSet} needs to be allowed). If this list is empty, no
   *                      linking is done to this direction. Sometimes links allow multiple
   *                      traversal modes and sometimes each mode gets its own link to a different
   *                      edge. There is also a risk that we cannot find edges nearby that allow
   *                      traversal with one or more of the given modes.
   * @param outgoingModes Linking is done to one (sometimes more if there are multiple options
   *                      within almost identical distance) of the nearest street edges for each
   *                      mode set (i.e. traversal with at least one of the modes in the
   *                      {@link TraverseModeSet} needs to be allowed). If this list is empty, no
   *                      linking is done to this direction. Sometimes links allow multiple
   *                      traversal modes and sometimes each mode gets its own link to a different
   *                      edge. There is also a risk that we cannot find edges nearby that allow
   *                      traversal with one or more of the given modes.
   * @param edgeCreator   Used to create linking edges between the given vertex and the street
   *                      network. The same edge creator is used for both directions of linking, so
   *                      it needs to be able to handle both cases (i.e. the from and to vertex will
   *                      switch places).
   */
  public DisposableEdgeCollection linkVertexForRequest(
    Vertex vertex,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes,
    RestrictedEdgeCreator edgeCreator
  ) {
    return link(vertex, incomingModes, outgoingModes, Scope.REQUEST, edgeCreator, edgeCreator);
  }

  /**
   * Attempts to link the given vertex to the street network temporarily (for the duration of a
   * request) through two links or more links that allow the vertex to be visited from the street
   * network and vice versa.
   *
   * @param incomingModes              Linking is done from one (sometimes more if there are
   *                                   multiple options within almost identical distance) of the
   *                                   nearest street edges for each mode set (i.e. traversal with
   *                                   at least one of the modes in the {@link TraverseModeSet}
   *                                   needs to be allowed). If this list is empty, no linking is
   *                                   done to this direction. Sometimes links allow multiple
   *                                   traversal modes and sometimes each mode gets its own link to
   *                                   a different edge. There is also a risk that we cannot find
   *                                   edges nearby that allow traversal with one or more of the
   *                                   given modes.
   * @param outgoingModes              Linking is done to one (sometimes more if there are multiple
   *                                   options within almost identical distance) of the nearest
   *                                   street edges for each mode set (i.e. traversal with at least
   *                                   one of the modes in the {@link TraverseModeSet} needs to be
   *                                   allowed). If this list is empty, no linking is done to this
   *                                   direction. Sometimes links allow multiple traversal modes and
   *                                   sometimes each mode gets its own link to a different edge.
   *                                   There is also a risk that we cannot find edges nearby that
   *                                   allow traversal with one or more of the given modes.
   * @param edgeCreator                Used to create linking edges between the given vertex and the
   *                                   street network. The same edge creator is used for both
   *                                   directions of linking, so it needs to be able to handle both
   *                                   cases (i.e. the from and to vertex will switch places).
   * @param initialSearchRadiusDegrees The search radius for the initial (and the only attempt if
   *                                   the linking is not request scoped) linking attempt.
   * @param maxSearchRadiusDegrees     The search radius for the second linking attempt. Only used
   *                                   for request scoped linking when the first attempt didn't find
   *                                   link for all the requested modes.
   */
  DisposableEdgeCollection linkVertexForRequest(
    Vertex vertex,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes,
    RestrictedEdgeCreator edgeCreator,
    double initialSearchRadiusDegrees,
    double maxSearchRadiusDegrees
  ) {
    return link(
      vertex,
      incomingModes,
      outgoingModes,
      Scope.REQUEST,
      edgeCreator,
      edgeCreator,
      initialSearchRadiusDegrees,
      maxSearchRadiusDegrees
    );
  }

  private void removeEdgeFromIndex(Edge edge, Scope scope) {
    // Edges without geometry will not have been added to the index in the first place
    if (edge.getGeometry() != null) {
      graph.removeEdge(edge, scope);
    }
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

  private DisposableEdgeCollection link(
    Vertex vertex,
    Set<TraverseModeSet> incomingTraverseModes,
    Set<TraverseModeSet> outgoingTraverseModes,
    Scope scope,
    EdgeCreator edgeCreator
  ) {
    return link(
      vertex,
      incomingTraverseModes,
      outgoingTraverseModes,
      scope,
      (fromVertex, toVertex, _) -> edgeCreator.create(fromVertex, toVertex),
      (fromVertex, toVertex, _) -> edgeCreator.create(fromVertex, toVertex)
    );
  }

  private DisposableEdgeCollection link(
    Vertex vertex,
    Set<TraverseModeSet> incomingTraverseModes,
    Set<TraverseModeSet> outgoingTraverseModes,
    Scope scope,
    EdgeCreator incomingEdgeCreator,
    EdgeCreator outgoingEdgeCreator
  ) {
    return link(
      vertex,
      incomingTraverseModes,
      outgoingTraverseModes,
      scope,
      (fromVertex, toVertex, _) -> incomingEdgeCreator.create(fromVertex, toVertex),
      (fromVertex, toVertex, _) -> outgoingEdgeCreator.create(fromVertex, toVertex)
    );
  }

  private DisposableEdgeCollection link(
    Vertex vertex,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes,
    Scope scope,
    RestrictedEdgeCreator incomingEdgeCreator,
    RestrictedEdgeCreator outgoingEdgeCreator
  ) {
    return link(
      vertex,
      incomingModes,
      outgoingModes,
      scope,
      incomingEdgeCreator,
      outgoingEdgeCreator,
      INITIAL_SEARCH_RADIUS_DEGREES,
      MAX_SEARCH_RADIUS_DEGREES
    );
  }

  /**
   * This method will link the provided vertex into the street graph. This may involve splitting an
   * existing edge (if the scope is not PERMANENT, the existing edge will be kept).
   * <p>
   * Searching for a good linking point can be a significant fraction of response time. Hannes
   * Junnila has reported >70% speedups in searches by making the search radius smaller. Therefore,
   * we use an expanding-envelope search, which is more efficient in dense areas.
   *
   * @param vertex                     Vertex to be linked into the street graph.
   * @param incomingModes              Linking is done from one (sometimes more if there are
   *                                   multiple options within almost identical distance) of the
   *                                   nearest street edges for each mode set (i.e. traversal with
   *                                   at least one of the modes in the {@link TraverseModeSet}
   *                                   needs to be allowed). If this list is empty, no linking is
   *                                   done to this direction. Sometimes links allow multiple
   *                                   traversal modes and sometimes each mode gets its own link to
   *                                   a different edge. There is also a risk that we cannot find
   *                                   edges nearby that allow traversal with one or more of the
   *                                   given modes.
   * @param outgoingModes              Linking is done to one (sometimes more if there are multiple
   *                                   options within almost identical distance) of the nearest
   *                                   street edges for each mode set (i.e. traversal with at least
   *                                   one of the modes in the {@link TraverseModeSet} needs to be
   *                                   allowed). If this list is empty, no linking is done to this
   *                                   direction. Sometimes links allow multiple traversal modes and
   *                                   sometimes each mode gets its own link to a different edge.
   *                                   There is also a risk that we cannot find edges nearby that
   *                                   allow traversal with one or more of the given modes.
   * @param scope                      The scope of the split.
   * @param incomingEdgeCreator        Used to create links between splitter vertices and the given
   *                                   vertex. The from location here is the splitter vertex and the
   *                                   to location is the input vertex.
   * @param outgoingEdgeCreator        Used to create links between the given vertex and splitter
   *                                   vertices. The from location here is the input vertex and the
   *                                   to location is the splitter vertex.
   * @param initialSearchRadiusDegrees The search radius for the initial (and the only attempt if
   *                                   the linking is not request scoped) linking attempt.
   * @param maxSearchRadiusDegrees     The search radius for the second linking attempt. Only used
   *                                   for request scoped linking when the first attempt didn't find
   *                                   link for all the requested modes.
   * @return A DisposableEdgeCollection with edges created by this method. It is the caller's
   * responsibility to call the dispose method on this object when the edges are no longer needed.
   */
  private DisposableEdgeCollection link(
    Vertex vertex,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes,
    Scope scope,
    RestrictedEdgeCreator incomingEdgeCreator,
    RestrictedEdgeCreator outgoingEdgeCreator,
    double initialSearchRadiusDegrees,
    double maxSearchRadiusDegrees
  ) {
    DisposableEdgeCollection tempEdges = (scope != Scope.PERMANENT)
      ? new DisposableEdgeCollection(graph, scope)
      : null;

    try {
      var usedModes = linkToStreetEdges(
        vertex,
        incomingModes,
        outgoingModes,
        scope,
        initialSearchRadiusDegrees,
        tempEdges,
        incomingEdgeCreator,
        outgoingEdgeCreator
      );
      var unlinkedIncomingModes = incomingModes.stream().filter(set -> set.isDisjoint(usedModes.incomingModes())).collect(Collectors.toSet());
      var unlinkedOutgoingModes = outgoingModes.stream().filter(set -> set.isDisjoint(usedModes.outgoingModes())).collect(Collectors.toSet());
      if (
        (!unlinkedIncomingModes.isEmpty() || !unlinkedOutgoingModes.isEmpty()) &&
        scope == Scope.REQUEST
      ) {
        // Expand the search for the modes we were unable to find nearby edges for. We don't want to
        // allow this large search radius for permanent or real-time linking as it can lead to weird
        // teleportation between islands and so on.
        linkToStreetEdges(
          vertex,
          unlinkedIncomingModes,
          unlinkedOutgoingModes,
          scope,
          maxSearchRadiusDegrees,
          tempEdges,
          incomingEdgeCreator,
          outgoingEdgeCreator
        );
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
   * Link a boarding location vertex to specific street edges bidirectionally.
   * <p>
   * This is used if a platform is mapped as a linear way, where the given edges form the platform.
   */
  public void linkToSpecificStreetEdgesPermanently(
    Vertex vertex,
    TraverseMode mode,
    Set<StreetEdge> edges,
    EdgeCreator incomingEdgeCreator,
    EdgeCreator outgoingEdgeCreator
  ) {
    var xscale = getXscale(vertex);
    var modeSet = new TraverseModeSet(mode);
    linkToCandidateEdges(
      vertex,
      Set.of(modeSet),
      Set.of(modeSet),
      Scope.PERMANENT,
      null,
      (from, to, _) -> incomingEdgeCreator.create(from, to),
      (from, to, _) -> outgoingEdgeCreator.create(from, to),
      edges
        .stream()
        .map(e -> new DistanceTo<>(e, distance(vertex, e, xscale)))
        .toList(),
      xscale
    );
  }

  private TraverseModeSetPair linkToStreetEdges(
    Vertex vertex,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes,
    Scope scope,
    double radiusDeg,
    @Nullable DisposableEdgeCollection tempEdges,
    RestrictedEdgeCreator incomingEdgeCreator,
    RestrictedEdgeCreator outgoingEdgeCreator
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
    var candidateEdges = graph.findEdges(env, scope);
    List<DistanceTo<StreetEdge>> candidateDistanceToEdges = candidateEdges
      .stream()
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .filter(
        e ->
          (incomingModes.stream().anyMatch(e::isLinkableWith) ||
            outgoingModes.stream().anyMatch(e::isLinkableWith)) &&
          e.isReachableFromGraph()
      )
      .map(e -> new DistanceTo<>(e, distance(vertex, e, xscale)))
      .filter(ead -> ead.distanceDegreesLat < radiusDeg)
      .toList();

    return linkToCandidateEdges(
      vertex,
      incomingModes,
      outgoingModes,
      scope,
      tempEdges,
      incomingEdgeCreator,
      outgoingEdgeCreator,
      candidateDistanceToEdges,
      xscale
    );
  }

  private static double getXscale(Vertex vertex) {
    return Math.cos((vertex.getLat() * Math.PI) / 180);
  }

  private TraverseModeSetPair linkToCandidateEdges(
    Vertex vertex,
    Set<TraverseModeSet> incomingTraverseModes,
    Set<TraverseModeSet> outgoingTraverseModes,
    Scope scope,
    @Nullable DisposableEdgeCollection tempEdges,
    RestrictedEdgeCreator incomingEdgeCreator,
    RestrictedEdgeCreator outgoingEdgeCreator,
    List<DistanceTo<StreetEdge>> candidateEdges,
    double xscale
  ) {
    var linkedIncomingModes = new TraverseModeSet();
    var linkedOutgoingModes = new TraverseModeSet();
    if (candidateEdges.isEmpty()) {
      return new TraverseModeSetPair(linkedIncomingModes, linkedOutgoingModes);
    }
    var allModes = ListUtils.combine(incomingTraverseModes, outgoingTraverseModes);
    var closestEdges = getClosestEdges(allModes, candidateEdges);
    var linkedAreas = new HashMap<AreaGroup, IntersectionVertex>();
    var snapResults = new HashSet<VertexPairWithPermission>();
    for (var edge : closestEdges) {
      var snapResult = snapAndLink(
        vertex,
        edge,
        xscale,
        scope,
        incomingTraverseModes,
        outgoingTraverseModes,
        tempEdges,
        linkedAreas
      );
      if (snapResult != null) {
        snapResults.add(snapResult);
      }
    }
    for (var vertexPair : snapResults) {
      var permission = vertexPair.permission();
          var incomingModes = incomingTraverseModes
          .stream()
          .reduce(new TraverseModeSet(), TraverseModeSet::merge);
      linkedIncomingModes = linkedIncomingModes.merge(
        createAndStoreEdge(
          vertexPair.intersection(),
          vertexPair.vertex(),
          permission.intersection(incomingModes),
          incomingEdgeCreator,
          tempEdges
        )
      );
      var outgoingModes = outgoingTraverseModes
        .stream()
        .reduce(new TraverseModeSet(), TraverseModeSet::merge);
      linkedOutgoingModes = linkedOutgoingModes.merge(
        createAndStoreEdge(
          vertexPair.vertex(),
          vertexPair.intersection(),
          permission.intersection(outgoingModes),
          outgoingEdgeCreator,
          tempEdges
        )
      );
    }
    return new TraverseModeSetPair(linkedIncomingModes, linkedOutgoingModes);
  }

  /**
   * Creates an edge between vertices if the given permission allows traversal with at least one
   * mode. If an edge is created and tempEdges is not null, the edge will be added to tempEdges.
   *
   * @return the modes for which an edge was created for. Empty set is returned if no edge was
   * created.
   */
  private TraverseModeSet createAndStoreEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    RestrictedEdgeCreator edgeCreator,
    @Nullable DisposableEdgeCollection tempEdges
  ) {
    if (permission.allowsAnything()) {
      var newEdge = edgeCreator.create(from, to, permission);
      if (newEdge != null && tempEdges != null) {
        tempEdges.addEdge(newEdge);
      }
      return permission.asTraverseModeSet();
    }
    return new TraverseModeSet();
  }

  /**
   * Find the closest edges. If the same edge(s) are the closest for multiple modes, pair the
   * edge(s) with all of those modes for traversal permission.
   *
   * @param candidateEdges these edges should be already filtered to only traversable with the given
   *                       modes and within a max search radius.
   */
  private List<EdgeWithPermission> getClosestEdges(
    List<TraverseModeSet> traverseModeSet,
    List<DistanceTo<StreetEdge>> candidateEdges
  ) {
    // The following logic has gone through several different versions using different approaches.
    // The core idea is to find all edges that are roughly the same distance (within
    // DUPLICATE_WAY_EPSILON_DEGREES from each other) from the given vertex, which will catch things
    // like superimposed edges going in opposite directions. This is done for all the requested
    // modes.

    var closestEdgesForModes = new HashMap<TraverseModeSet, List<StreetEdge>>();
    for (var modeSet : traverseModeSet) {
      var candidateEdgesForMode = candidateEdges
        .stream()
        .filter(e -> e.item.isLinkableWith(modeSet))
        .toList();

      if (candidateEdgesForMode.isEmpty()) {
        continue;
      }

      double closestDistance = candidateEdgesForMode
        .stream()
        .mapToDouble(ce -> ce.distanceDegreesLat)
        .min()
        .getAsDouble();

      closestEdgesForModes.put(
        modeSet,
        candidateEdgesForMode
          .stream()
          .filter(ce -> ce.distanceDegreesLat <= closestDistance + DUPLICATE_WAY_EPSILON_DEGREES)
          .map(ce -> ce.item)
          .toList()
      );
    }

    // To avoid duplicates, we merge the nearest edges from all the requested modes while keeping
    // information of what modes they were the nearest for.
    var closestEdges = new ArrayList<EdgeWithPermission>();
    for (var edge : candidateEdges) {
      var permission = StreetTraversalPermission.NONE;
      for (var edgeForMode : closestEdgesForModes.entrySet()) {
        if (edgeForMode.getValue().contains(edge.item)) {
          permission = permission.add(edge.item.getPermission().intersection(edgeForMode.getKey()));
        }
      }
      if (permission.allowsAnything()) {
        closestEdges.add(new EdgeWithPermission(edge.item, permission));
      }
    }

    return closestEdges;
  }

  /* Snap a vertex to and edge if necessary, create required linking and return the applied entry vertex */
  @Nullable
  private VertexPairWithPermission snapAndLink(
    Vertex vertex,
    EdgeWithPermission edgeWithPermission,
    double xScale,
    Scope scope,
    Set<TraverseModeSet> incomingTraverseModes,
    Set<TraverseModeSet> outgoingTraverseModes,
    DisposableEdgeCollection tempEdges,
    HashMap<AreaGroup, IntersectionVertex> linkedAreas
  ) {
    IntersectionVertex start = null;
    var edge = edgeWithPermission.edge();

    var direction = getLinkingDirection(incomingTraverseModes, outgoingTraverseModes);
    // Always consider linking to closest point on the edge
    IntersectionVertex split = findSplitVertex(vertex, edge, xScale, scope, direction, tempEdges);

    // check if vertex is inside an area
    if (
      this.visibilityMode == VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES &&
      edge instanceof AreaEdge aEdge
    ) {
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

    if (shouldLinkFlex) {
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

    return start != null
      ? new VertexPairWithPermission(vertex, start, edgeWithPermission.permission())
      : null;
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
      : originalEdge.splitNonDestructively(v, direction);

    if (scope != Scope.PERMANENT) {
      newEdges.forEach(tempEdges::addEdge);
    }

    if (scope == Scope.REALTIME || scope == Scope.PERMANENT) {
      // update indices of new edges

      newEdges.forEach(e -> graph.insert(e, scope));

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
      v = tsv;
    } else {
      v = splitterVertex(originalEdge, x, y, uniqueSplitLabel);
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
          .sorted((v1, v2) ->
            Double.compare(distSquared(v1, newVertex), distSquared(v2, newVertex))
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
          .sorted((v1, v2) ->
            Double.compare(distSquared(v1, newVertex), distSquared(v2, newVertex))
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

  public static Set<TraverseMode> getNoThruModes(Collection<Edge> edges) {
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

  /**
   * Create a slightly shortened line between two coordinates.
   * This is used when testing if a polygon contains a line between two
   * of its boundary points. Floating point math cannot represent boundaries
   * precisely, so we need to shrink the line to ensure robust testing.
   */
  private LineString createShrunkLine(Coordinate from, Coordinate to) {
    var dx = AREA_INTERSECTION_SHRINKING * (to.x - from.x);
    var dy = AREA_INTERSECTION_SHRINKING * (to.y - from.y);
    var c1 = new Coordinate(from.x + dx, from.y + dy);
    var c2 = new Coordinate(to.x - dx, to.y - dy);
    return GEOMETRY_FACTORY.createLineString(new Coordinate[] { c1, c2 });
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
    var c1 = from.getCoordinate();
    var c2 = to.getCoordinate();
    // ensure that new edge does not leave the bounds of the area or hit any holes
    if (!force && !ag.getGeometry().contains(createShrunkLine(c1, c2))) {
      return false;
    }
    LineString line = GEOMETRY_FACTORY.createLineString(new Coordinate[] { c1, c2 });
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
    // 'from' is the new vertex to be connected, so check the 'to' vertex connections
    var incomingNoThruModes = getNoThruModes(to.getIncoming());
    var outgoingNoThruModes = getNoThruModes(to.getOutgoing());
    AreaEdgeBuilder areaEdgeBuilder = new AreaEdgeBuilder()
      .withFromVertex(from)
      .withToVertex(to)
      .withGeometry(line)
      .withName(hit.getName())
      .withMeterLength(length)
      .withPermission(hit.getPermission())
      .withBicycleSafetyFactor(hit.getBicycleSafety())
      .withWalkSafetyFactor(hit.getWalkSafety())
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
      .withBicycleSafetyFactor(hit.getBicycleSafety())
      .withWalkSafetyFactor(hit.getWalkSafety())
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

  private SplitterVertex splitterVertex(
    StreetEdge originalEdge,
    double x,
    double y,
    String uniqueSplitLabel
  ) {
    var vertex = new SplitterVertex(uniqueSplitLabel, x, y, originalEdge.getName());
    graph.addVertex(vertex);
    return vertex;
  }

  /**
   * Returns the linking direction from the perspective of the main graph. The parameters are from
   * the perspective of the vertex that is being linked to the graph.
   */
  private LinkingDirection getLinkingDirection(
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes
  ) {
    if (!incomingModes.isEmpty() && !outgoingModes.isEmpty()) {
      return LinkingDirection.BIDIRECTIONAL;
    }
    if (!incomingModes.isEmpty()) {
      return LinkingDirection.OUTGOING;
    }
    if (!outgoingModes.isEmpty()) {
      return LinkingDirection.INCOMING;
    }
    throw new IllegalArgumentException("Both incoming and outgoing modes cannot be empty.");
  }

  private record EdgeWithPermission(StreetEdge edge, StreetTraversalPermission permission) {}

  private record VertexPairWithPermission(
    Vertex vertex,
    IntersectionVertex intersection,
    StreetTraversalPermission permission
  ) {}

  private record TraverseModeSetPair(
    TraverseModeSet incomingModes,
    TraverseModeSet outgoingModes
  ) {}
}

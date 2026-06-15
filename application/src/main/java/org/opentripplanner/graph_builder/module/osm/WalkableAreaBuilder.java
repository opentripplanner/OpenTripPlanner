package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.cache.KeyValueCache;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

class WalkableAreaBuilder {

  private final DataImportIssueStore issueStore;
  private final int maxAreaNodes;
  private final Graph graph;
  private final OsmDatabase osmdb;
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private final Map<OsmEntity, WayProperties> wayPropertiesCache = new HashMap<>();

  private final VertexGenerator vertexBuilder;

  private final boolean platformEntriesLinking;

  private final List<OsmVertex> platformLinkingPoints;
  private final Set<String> boardingLocationRefTags;
  private final EdgeNamer namer;
  private final SafetyValueApplier safetyValueApplier;

  /**
   * Visibility cache loaded from disk before processing begins. Key: area group hash.
   * Value: survived visibility-edge pairs as {@code {fromX, fromY, toX, toY}} per entry.
   * {@code null} when visibility caching is disabled.
   */
  @Nullable
  private final KeyValueCache<Long, double[][]> visibilityCache;

  // template for AreaEdge names
  private static final String LABEL_TEMPLATE = "way (area) %s from %s to %s";

  public WalkableAreaBuilder(
    Graph graph,
    OsmDatabase osmdb,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VertexGenerator vertexBuilder,
    EdgeNamer namer,
    SafetyValueApplier safetyValueApplier,
    DataImportIssueStore issueStore,
    int maxAreaNodes,
    boolean platformEntriesLinking,
    Set<String> boardingLocationRefTags
  ) {
    this(
      graph,
      osmdb,
      osmInfoGraphBuildRepository,
      vertexBuilder,
      namer,
      safetyValueApplier,
      issueStore,
      maxAreaNodes,
      platformEntriesLinking,
      boardingLocationRefTags,
      null
    );
  }

  public WalkableAreaBuilder(
    Graph graph,
    OsmDatabase osmdb,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VertexGenerator vertexBuilder,
    EdgeNamer namer,
    SafetyValueApplier safetyValueApplier,
    DataImportIssueStore issueStore,
    int maxAreaNodes,
    boolean platformEntriesLinking,
    Set<String> boardingLocationRefTags,
    @Nullable KeyValueCache<Long, double[][]> visibilityCache
  ) {
    this.graph = graph;
    this.osmdb = osmdb;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.vertexBuilder = vertexBuilder;
    this.namer = namer;
    this.safetyValueApplier = safetyValueApplier;
    this.issueStore = issueStore;
    this.maxAreaNodes = maxAreaNodes;
    this.platformEntriesLinking = platformEntriesLinking;
    this.boardingLocationRefTags = boardingLocationRefTags;
    this.visibilityCache = visibilityCache;
    this.platformLinkingPoints = platformEntriesLinking
      ? graph
          .getVertices()
          .stream()
          .filter(OsmVertex.class::isInstance)
          .map(OsmVertex.class::cast)
          .filter(this::isPlatformLinkingPoint)
          .collect(Collectors.toList())
      : List.of();
  }

  /**
   * For all areas just use outermost rings as edges so that areas can be routable without
   * visibility calculations
   */
  public void buildWithoutVisibility(OsmAreaGroup group) {
    // create polygon and accumulate nodes for area
    for (Ring ring : group.outermostRings) {
      Set<AreaEdge> edges = new HashSet<>();
      AreaGroup areaGroup = new AreaGroup(ring.jtsPolygon);
      HashSet<NodeEdge> alreadyAddedEdges = new HashSet<>();
      for (OsmArea area : group.areas) {
        if (!ring.jtsPolygon.contains(area.jtsMultiPolygon)) {
          continue;
        }

        for (Ring outerRing : area.outermostRings) {
          for (int i = 0; i < outerRing.nodes.size(); ++i) {
            edges.addAll(
              createEdgesForRingSegment(areaGroup, area, outerRing, i, alreadyAddedEdges)
            );
          }
          for (Ring innerRing : outerRing.getHoles()) {
            for (int j = 0; j < innerRing.nodes.size(); ++j) {
              edges.addAll(
                createEdgesForRingSegment(areaGroup, area, innerRing, j, alreadyAddedEdges)
              );
            }
          }
        }
      }
      var vertices = edges
        .stream()
        .flatMap(v ->
          Stream.of(v.getFromVertex(), v.getToVertex())
            .filter(IntersectionVertex.class::isInstance)
            .map(IntersectionVertex.class::cast)
        )
        .collect(Collectors.toSet());
      areaGroup.addVisibilityVertices(vertices);

      createAreas(areaGroup, ring, group.areas);
    }
  }

  /**
   * Build walkable area edges using visibility graph computation.
   *
   * <p>Three phases:
   * <ol>
   *   <li>Build ring edges and collect vertex candidates for each ring.
   *   <li>Compute which vertex pairs have line-of-sight within the polygon (cache miss),
   *       or replay previously computed pairs (cache hit).
   *   <li>Add visibility edges, prune unused ones, and finalise area group vertex sets.
   * </ol>
   */
  public void buildWithVisibility(OsmAreaGroup group) {
    long cacheKey = group.cacheKey();
    double[][] cachedPairs = visibilityCache != null ? visibilityCache.get(cacheKey) : null;

    Set<Long> osmWayIds = collectOsmWayIds(group);
    RingSetData ringSetData = buildAllRingEdges(group, osmWayIds);

    if (ringSetData.allEdges().isEmpty()) {
      return;
    }

    if (cachedPairs != null) {
      replayVisibilityEdges(cachedPairs, ringSetData, group);
    } else {
      List<VisibilityPair> visiblePairs = computeVisiblePairs(ringSetData);
      VisibilityEdgesResult visEdges = addVisibilityEdges(visiblePairs, group);

      Set<Edge> allEdges = new HashSet<>(ringSetData.allEdges());
      allEdges.addAll(visEdges.allEdges());

      Set<Edge> edgesToKeep = new HashSet<>(ringSetData.ringEdges());
      edgesToKeep.addAll(visEdges.platformLinkedEdges());

      Set<Edge> surviving = pruneAreaEdges(ringSetData.startingVertices(), allEdges, edgesToKeep);

      if (visibilityCache != null) {
        double[][] pairs = surviving
          .stream()
          .map(e ->
            new double[] {
              e.getFromVertex().getX(),
              e.getFromVertex().getY(),
              e.getToVertex().getX(),
              e.getToVertex().getY(),
            }
          )
          .toArray(double[][]::new);
        visibilityCache.put(cacheKey, pairs);
      }
    }

    ringSetData
      .visibilityVertexCandidates()
      .forEach((areaGroup, vertices) -> {
        if (vertices.size() > maxAreaNodes) {
          areaGroup.addVisibilityVertices(
            vertices
              .stream()
              .sorted((v1, v2) -> Long.compare(v2.getDegreeOut(), v1.getDegreeOut()))
              .limit(maxAreaNodes)
              .collect(Collectors.toSet())
          );
        } else {
          areaGroup.addVisibilityVertices(vertices);
        }
      });
  }

  // ---- Phase 1: ring traversal -------------------------------------------------------

  /**
   * Traverse all rings in the group: create ring edges, collect visibility vertex candidates,
   * and register area metadata. Returns combined data for subsequent phases.
   */
  private RingSetData buildAllRingEdges(OsmAreaGroup group, Set<Long> osmWayIds) {
    Set<Edge> allEdges = new HashSet<>();
    Set<Edge> ringEdges = new HashSet<>();
    Set<Vertex> startingVertices = new HashSet<>();
    Map<AreaGroup, HashSet<IntersectionVertex>> visibilityVertexCandidates = new HashMap<>();
    Map<IntersectionVertex, AreaGroup> vertexToAreaGroup = new HashMap<>();
    List<PerRingData> perRingData = new ArrayList<>();

    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    for (Ring ring : group.outermostRings) {
      Polygon polygon = ring.jtsPolygon;
      AreaGroup areaGroup = new AreaGroup(polygon);
      HashSet<NodeEdge> alreadyAddedEdges = new HashSet<>();
      HashSet<IntersectionVertex> platformLinkingVertices = new HashSet<>();
      HashSet<IntersectionVertex> visibilityVertices = new HashSet<>();

      for (OsmArea area : group.areas) {
        OsmEntity areaEntity = area.parent;

        if (!group.isSimpleAreaGroup() && !polygon.contains(area.jtsMultiPolygon)) {
          continue;
        }

        // Add stops/entrances from public transit relations — they may be the only entrance to
        // a platform, which otherwise would be pruned as an unconnected island.
        Collection<OsmNode> entrances = osmdb.getStopsInArea(area.parent);
        for (OsmNode node : entrances) {
          var vertex = vertexBuilder.getVertexForOsmNode(node, areaEntity, SPLIT);
          platformLinkingVertices.add(vertex);
          visibilityVertices.add(vertex);
          startingVertices.add(vertex);
        }

        for (Ring outerRing : area.outermostRings) {
          boolean linkPointsAdded = !entrances.isEmpty();
          if (platformEntriesLinking && area.parent.isPlatform()) {
            List<OsmVertex> verticesWithin = platformLinkingPoints
              .stream()
              .filter(t ->
                outerRing.jtsPolygon.contains(geometryFactory.createPoint(t.getCoordinate()))
              )
              .toList();
            platformLinkingVertices.addAll(verticesWithin);
            for (OsmVertex v : verticesWithin) {
              startingVertices.add(v);
              visibilityVertices.add(v);
              linkPointsAdded = true;
            }
          }

          for (int i = 0; i < outerRing.nodes.size(); ++i) {
            OsmNode node = outerRing.nodes.get(i);
            Set<AreaEdge> newEdges = createEdgesForRingSegment(
              areaGroup,
              area,
              outerRing,
              i,
              alreadyAddedEdges
            );
            allEdges.addAll(newEdges);
            ringEdges.addAll(newEdges);

            // Convex corners and mid-points when link points are present are visibility candidates.
            boolean convex =
              outerRing.isNodeConvex(i) ||
              (linkPointsAdded && (i == 0 || i == outerRing.nodes.size() / 2));
            boolean starting = isStartingNode(node, osmWayIds);
            if (convex || starting) {
              var v = vertexBuilder.getVertexForOsmNode(node, areaEntity, SPLIT);
              visibilityVertices.add(v);
              if (starting) {
                startingVertices.add(v);
              }
            }
          }

          for (Ring innerRing : outerRing.getHoles()) {
            for (int j = 0; j < innerRing.nodes.size(); ++j) {
              OsmNode node = innerRing.nodes.get(j);
              var newEdges = createEdgesForRingSegment(
                areaGroup,
                area,
                innerRing,
                j,
                alreadyAddedEdges
              );
              allEdges.addAll(newEdges);
              ringEdges.addAll(newEdges);
              // For holes the convexity condition is inverted.
              boolean concave = !innerRing.isNodeConvex(j);
              boolean starting = isStartingNode(node, osmWayIds);
              if (concave || starting) {
                var v = vertexBuilder.getVertexForOsmNode(node, areaEntity, SPLIT);
                visibilityVertices.add(v);
                if (starting) {
                  startingVertices.add(v);
                }
              }
            }
          }
        }
      }

      if (visibilityVertices.isEmpty()) {
        issueStore.add(new UnconnectedArea(group));
        for (Edge edge : allEdges) {
          graph.removeEdge(edge);
        }
        allEdges.clear();
        ringEdges.clear();
        continue;
      }

      if (visibilityVertices.size() > maxAreaNodes) {
        issueStore.add(new AreaTooComplicated(group, visibilityVertices.size(), maxAreaNodes));
      }

      visibilityVertexCandidates.put(areaGroup, visibilityVertices);
      for (IntersectionVertex v : visibilityVertices) {
        vertexToAreaGroup.putIfAbsent(v, areaGroup);
      }
      createAreas(areaGroup, ring, group.areas);
      perRingData.add(
        new PerRingData(
          areaGroup,
          polygon,
          visibilityVertices,
          platformLinkingVertices,
          alreadyAddedEdges
        )
      );
    }

    return new RingSetData(
      allEdges,
      ringEdges,
      startingVertices,
      visibilityVertexCandidates,
      vertexToAreaGroup,
      perRingData
    );
  }

  // ---- Phase 2: visibility computation -----------------------------------------------

  /**
   * For each ring, test all candidate vertex pairs for line-of-sight within the ring polygon.
   * This is a pure computation — no graph mutations.
   *
   * <p>When the area has more vertices than {@code maxAreaNodes}, the vertex set is sampled
   * uniformly so that at least some cross-edges are added even for complex areas.
   */
  private List<VisibilityPair> computeVisiblePairs(RingSetData ringSetData) {
    List<VisibilityPair> pairs = new ArrayList<>();
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

    for (PerRingData ringData : ringSetData.perRingData()) {
      float skipRatio = (float) maxAreaNodes / (float) ringData.visibilityVertices().size();
      int i = 0;
      float sumI = 0;
      for (IntersectionVertex vertex1 : ringData.visibilityVertices()) {
        sumI += skipRatio;
        if (Math.floor(sumI) < i + 1) {
          continue;
        }
        i = (int) Math.floor(sumI);
        int j = 0;
        float sumJ = 0;
        for (IntersectionVertex vertex2 : ringData.visibilityVertices()) {
          sumJ += skipRatio;
          if (Math.floor(sumJ) < j + 1) {
            continue;
          }
          j = (int) Math.floor(sumJ);
          if (shouldSkipEdge(vertex1, vertex2, ringData.alreadyAddedEdges())) {
            continue;
          }
          Coordinate[] coordinates = new Coordinate[] {
            vertex1.getCoordinate(),
            vertex2.getCoordinate(),
          };
          LineString line = geometryFactory.createLineString(coordinates);
          if (ringData.polygon().contains(line)) {
            boolean platformLinked =
              ringData.platformLinkingVertices().contains(vertex1) ||
              ringData.platformLinkingVertices().contains(vertex2);
            pairs.add(new VisibilityPair(vertex1, vertex2, ringData.areaGroup(), platformLinked));
          }
        }
      }
    }
    return pairs;
  }

  // ---- Phase 3a: add visibility edges (cache miss) -----------------------------------

  /**
   * Create graph edges for each visibility pair and return them split into all edges and the
   * subset that must survive pruning because they connect platform-linking vertices.
   */
  private VisibilityEdgesResult addVisibilityEdges(List<VisibilityPair> pairs, OsmAreaGroup group) {
    Set<AreaEdge> allEdges = new HashSet<>();
    Set<AreaEdge> platformLinkedEdges = new HashSet<>();
    for (VisibilityPair pair : pairs) {
      Set<AreaEdge> segments = createSegments(
        pair.from(),
        pair.to(),
        group.areas,
        pair.areaGroup(),
        true
      );
      allEdges.addAll(segments);
      if (pair.platformLinked()) {
        platformLinkedEdges.addAll(segments);
      }
    }
    return new VisibilityEdgesResult(allEdges, platformLinkedEdges);
  }

  // ---- Phase 3b: replay visibility edges (cache hit) ---------------------------------

  /**
   * Reconstruct visibility edges from previously cached coordinate pairs. No pruning step is
   * needed because the cached pairs are already the pruned survivors of a previous run.
   */
  private void replayVisibilityEdges(
    double[][] cachedPairs,
    RingSetData ringSetData,
    OsmAreaGroup group
  ) {
    Map<CoordKey, IntersectionVertex> vertexByCoord = new HashMap<>();
    for (IntersectionVertex v : ringSetData.vertexToAreaGroup().keySet()) {
      vertexByCoord.put(new CoordKey(v.getX(), v.getY()), v);
    }
    for (double[] pair : cachedPairs) {
      IntersectionVertex v1 = vertexByCoord.get(new CoordKey(pair[0], pair[1]));
      IntersectionVertex v2 = vertexByCoord.get(new CoordKey(pair[2], pair[3]));
      if (v1 == null || v2 == null) {
        continue;
      }
      AreaGroup ag = ringSetData
        .vertexToAreaGroup()
        .getOrDefault(v1, ringSetData.vertexToAreaGroup().get(v2));
      if (ag != null) {
        createSegments(v1, v2, group.areas, ag, true);
      }
    }
  }

  // ---- Pruning -----------------------------------------------------------------------

  /**
   * Do an all-pairs shortest path search from a list of vertices over a specified set of edges,
   * and retain only those edges which are actually used in some shortest path.
   *
   * @return the visibility edges (not in {@code edgesToKeep}) that survived pruning
   */
  private Set<Edge> pruneAreaEdges(
    Collection<Vertex> startingVertices,
    Set<Edge> edges,
    Set<Edge> edgesToKeep
  ) {
    if (edges.isEmpty()) {
      return Set.of();
    }
    StreetMode mode;
    StreetEdge firstEdge = (StreetEdge) edges.iterator().next();

    if (firstEdge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
      mode = StreetMode.WALK;
    } else if (firstEdge.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
      mode = StreetMode.BIKE;
    } else {
      mode = StreetMode.CAR;
    }
    // TODO: This is incorrect, the configured defaults are not used.
    var request = StreetSearchRequest.of().withMode(mode).build();
    Set<Edge> usedEdges = new HashSet<>();
    for (Vertex vertex : startingVertices) {
      ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
        .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
        .withSkipEdgeStrategy(new ListedEdgesOnly(edges))
        .withDominanceFunction(new DominanceFunctions.EarliestArrival())
        .withRequest(request)
        .withFrom(vertex)
        .getShortestPathTree();

      for (Vertex endVertex : startingVertices) {
        GraphPath<State, Edge, Vertex> path = spt.getPath(endVertex);
        if (path != null) {
          usedEdges.addAll(path.edges);
        }
      }
    }
    Set<Edge> survivingVisibilityEdges = new HashSet<>();
    for (Edge edge : edges) {
      if (!usedEdges.contains(edge) && !edgesToKeep.contains(edge)) {
        graph.removeEdge(edge);
      } else if (!edgesToKeep.contains(edge)) {
        survivingVisibilityEdges.add(edge);
      }
    }
    return survivingVisibilityEdges;
  }

  // ---- Helpers -----------------------------------------------------------------------

  private Set<Long> collectOsmWayIds(OsmAreaGroup group) {
    return group.areas
      .stream()
      .map(area -> area.parent)
      .flatMap(osmEntity ->
        osmEntity instanceof OsmRelation relation
          ? relation.getMembers().stream().map(OsmRelationMember::getRef)
          : Stream.of(osmEntity.getId())
      )
      .collect(Collectors.toSet());
  }

  private boolean isStartingNode(OsmNode node, Set<Long> osmWayIds) {
    return (
      osmdb.isNodeBelongsToWay(node.getId()) ||
      // Do not add if part of same areaGroup
      !osmdb
        .getAreasForNode(node.getId())
        .stream()
        .allMatch(osmWay -> osmWayIds.contains(osmWay.getId())) ||
      node.isBoardingLocation()
    );
  }

  private WayProperties findAreaProperties(OsmEntity entity) {
    return wayPropertiesCache.computeIfAbsent(entity, e ->
      e.getOsmProvider().getWayPropertySet().getDataForEntity(e)
    );
  }

  private Set<AreaEdge> createEdgesForRingSegment(
    AreaGroup areaGroup,
    OsmArea area,
    Ring ring,
    int i,
    HashSet<NodeEdge> alreadyAddedEdges
  ) {
    OsmNode node = ring.nodes.get(i);
    OsmNode nextNode = ring.nodes.get((i + 1) % ring.nodes.size());
    IntersectionVertex v1 = vertexBuilder.getVertexForOsmNode(node, area.parent, SPLIT);
    IntersectionVertex v2 = vertexBuilder.getVertexForOsmNode(nextNode, area.parent, SPLIT);

    if (shouldSkipEdge(v1, v2, alreadyAddedEdges)) {
      return Set.of();
    }

    return createSegments(v1, v2, List.of(area), areaGroup, false);
  }

  private Set<AreaEdge> createSegments(
    IntersectionVertex vertex1,
    IntersectionVertex vertex2,
    Collection<OsmArea> areas,
    AreaGroup areaGroup,
    boolean testIntersection
  ) {
    Coordinate[] coordinates = new Coordinate[] {
      vertex1.getCoordinate(),
      vertex2.getCoordinate(),
    };
    double length = SphericalDistanceLibrary.distance(
      vertex1.getCoordinate(),
      vertex2.getCoordinate()
    );
    if (length < 0.01) {
      // vertex1 and vertex2 are in the same position
      return Set.of();
    }

    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    LineString line = geometryFactory.createLineString(coordinates);

    OsmEntity parent = null;
    WayProperties wayData = null;
    StreetTraversalPermission areaPermissions = StreetTraversalPermission.ALL;
    boolean wheelchairAccessible = true;

    // combine properties of intersected areas
    for (OsmArea area : areas) {
      MultiPolygon polygon = area.jtsMultiPolygon;
      // intersects() is a cheap spatial predicate; only compute the full intersection when needed
      boolean crosses =
        !testIntersection ||
        (polygon.intersects(line) && polygon.intersection(line).getLength() > 0.000001);
      if (crosses) {
        parent = area.parent;
        wayData = findAreaProperties(parent);
        areaPermissions = areaPermissions.intersection(wayData.getPermission());
        wheelchairAccessible = wheelchairAccessible && parent.isWheelchairAccessible();
      }
    }
    if (parent == null) {
      // No intersections - not really possible
      return Set.of();
    }
    String label = String.format(
      LABEL_TEMPLATE,
      parent.getId(),
      vertex1.getLabel(),
      vertex2.getLabel()
    );

    float carSpeed = parent
      .getOsmProvider()
      .getOsmTagMapper()
      .getCarSpeedForWay(parent, TraverseDirection.DIRECTIONLESS, issueStore);

    I18NString name = namer.getName(parent, label);
    AreaEdgeBuilder streetEdgeBuilder = new AreaEdgeBuilder()
      .withFromVertex(vertex1)
      .withToVertex(vertex2)
      .withGeometry(line)
      .withName(name)
      .withMeterLength(length)
      .withPermission(areaPermissions)
      .withBack(false)
      .withArea(areaGroup)
      .withCarSpeed(carSpeed)
      .withBogusName(parent.hasNoName())
      .withWheelchairAccessible(wheelchairAccessible)
      .withLink(parent.isLink());

    label = String.format(LABEL_TEMPLATE, parent.getId(), vertex2.getLabel(), vertex1.getLabel());
    name = namer.getName(parent, label);
    AreaEdgeBuilder backStreetEdgeBuilder = new AreaEdgeBuilder()
      .withFromVertex(vertex2)
      .withToVertex(vertex1)
      .withGeometry(line.reverse())
      .withName(name)
      .withMeterLength(length)
      .withPermission(areaPermissions)
      .withBack(true)
      .withArea(areaGroup)
      .withCarSpeed(carSpeed)
      .withBogusName(parent.hasNoName())
      .withWheelchairAccessible(wheelchairAccessible)
      .withLink(parent.isLink());

    AreaEdge street = streetEdgeBuilder.buildAndConnect();
    AreaEdge backStreet = backStreetEdgeBuilder.buildAndConnect();
    safetyValueApplier.applyWayProperties(street, backStreet, wayData, wayData, parent);
    return Set.of(street, backStreet);
  }

  private void createAreas(AreaGroup areaGroup, Ring ring, Collection<OsmArea> areas) {
    Polygon containingArea = ring.jtsPolygon;
    for (OsmArea area : areas) {
      Geometry intersection = containingArea.intersection(area.jtsMultiPolygon);
      if (intersection.getArea() == 0) {
        continue;
      }
      Area namedArea = new Area();
      OsmEntity areaEntity = area.parent;

      String id = "way (area) " + areaEntity.getId();
      I18NString name = namer.getName(areaEntity, id);
      namedArea.setName(name);

      WayProperties wayData = findAreaProperties(areaEntity);
      namedArea.setBicycleSafety((float) wayData.bicycleSafety());
      namedArea.setWalkSafety((float) wayData.walkSafety());
      namedArea.setGeometry(intersection);
      namedArea.setPermission(wayData.getPermission());
      namedArea.setWheelchairAccessible(areaEntity.isWheelchairAccessible());
      areaGroup.addArea(namedArea);

      if (areaEntity.isBoardingLocation()) {
        var references = areaEntity.getMultiTagValues(boardingLocationRefTags);
        if (!references.isEmpty()) {
          var platform = new Platform(name, area.findInteriorPoint(), references);
          osmInfoGraphBuildRepository.addPlatform(namedArea, platform);
        }
      }
    }
  }

  private boolean isPlatformLinkingPoint(OsmVertex osmVertex) {
    boolean isCandidate = false;
    Vertex start = null;
    for (Edge e : osmVertex.getIncoming()) {
      if (e instanceof StreetEdge se && !(e instanceof AreaEdge)) {
        if (Arrays.asList(1, 2, 3).contains(se.getPermission().code)) {
          isCandidate = true;
          start = se.getFromVertex();
          break;
        }
      }
    }

    if (isCandidate && start != null) {
      boolean isLinkingPoint = true;
      for (Edge se : osmVertex.getOutgoing()) {
        if (
          !se.getToVertex().getCoordinate().equals(start.getCoordinate()) &&
          !(se instanceof AreaEdge)
        ) {
          isLinkingPoint = false;
        }
      }
      return isLinkingPoint;
    }
    return false;
  }

  private boolean shouldSkipEdge(
    IntersectionVertex v1,
    IntersectionVertex v2,
    HashSet<NodeEdge> alreadyAddedEdges
  ) {
    if (v1 == v2) {
      return true;
    }
    NodeEdge edge = new NodeEdge(v1, v2);
    if (alreadyAddedEdges.contains(edge) || alreadyAddedEdges.contains(new NodeEdge(v2, v1))) {
      return true;
    }
    alreadyAddedEdges.add(edge);
    return false;
  }

  // ---- Inner types -------------------------------------------------------------------

  record ListedEdgesOnly(Set<Edge> edges) implements SkipEdgeStrategy<State, Edge> {
    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
      return !edges.contains(edge);
    }
  }

  private record NodeEdge(IntersectionVertex from, IntersectionVertex to) {}

  /**
   * Per-ring data collected during Phase 1, passed into Phase 2 for visibility computation.
   * The {@code alreadyAddedEdges} set is mutable and extended during Phase 2 to prevent
   * duplicate visibility edges across both phases.
   */
  private record PerRingData(
    AreaGroup areaGroup,
    Polygon polygon,
    HashSet<IntersectionVertex> visibilityVertices,
    Set<IntersectionVertex> platformLinkingVertices,
    HashSet<NodeEdge> alreadyAddedEdges
  ) {}

  /** Combined output of Phase 1 covering all rings in an area group. */
  private record RingSetData(
    Set<Edge> allEdges,
    Set<Edge> ringEdges,
    Set<Vertex> startingVertices,
    Map<AreaGroup, HashSet<IntersectionVertex>> visibilityVertexCandidates,
    Map<IntersectionVertex, AreaGroup> vertexToAreaGroup,
    List<PerRingData> perRingData
  ) {}

  /** A vertex pair confirmed to have line-of-sight within a ring polygon. */
  private record VisibilityPair(
    IntersectionVertex from,
    IntersectionVertex to,
    AreaGroup areaGroup,
    boolean platformLinked
  ) {}

  /** Output of Phase 3a: visibility edges split by whether they must survive pruning. */
  private record VisibilityEdgesResult(Set<AreaEdge> allEdges, Set<AreaEdge> platformLinkedEdges) {}

  /** Allocation-free coordinate key for vertex lookup maps. */
  private record CoordKey(long xBits, long yBits) {
    CoordKey(double x, double y) {
      this(Double.doubleToLongBits(x), Double.doubleToLongBits(y));
    }
  }
}

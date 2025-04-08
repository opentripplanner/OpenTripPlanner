package org.opentripplanner.graph_builder.module.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
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
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WalkableAreaBuilder {

  private final DataImportIssueStore issueStore;
  private final int maxAreaNodes;
  private final Graph graph;
  private final OsmDatabase osmdb;
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private final Map<OsmEntity, WayProperties> wayPropertiesCache = new HashMap<>();

  private final VertexGenerator vertexBuilder;

  private final HashMap<Coordinate, IntersectionVertex> areaBoundaryVertexForCoordinate =
    new HashMap<>();

  private final boolean platformEntriesLinking;

  private final List<OsmVertex> platformLinkingPoints;
  private final Set<String> boardingLocationRefTags;
  private final EdgeNamer namer;
  private final SafetyValueNormalizer normalizer;

  // template for AreaEdge names
  private static final String labelTemplate = "way (area) %s from %s to %s";

  private static final Logger LOG = LoggerFactory.getLogger(WalkableAreaBuilder.class);

  public WalkableAreaBuilder(
    Graph graph,
    OsmDatabase osmdb,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VertexGenerator vertexBuilder,
    EdgeNamer namer,
    SafetyValueNormalizer normalizer,
    DataImportIssueStore issueStore,
    int maxAreaNodes,
    boolean platformEntriesLinking,
    Set<String> boardingLocationRefTags
  ) {
    this.graph = graph;
    this.osmdb = osmdb;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.vertexBuilder = vertexBuilder;
    this.namer = namer;
    this.normalizer = normalizer;
    this.issueStore = issueStore;
    this.maxAreaNodes = maxAreaNodes;
    this.platformEntriesLinking = platformEntriesLinking;
    this.boardingLocationRefTags = boardingLocationRefTags;
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

  public void buildWithVisibility(OsmAreaGroup group) {
    // These sets contain the nodes/vertices which can be used to traverse from the rest of the
    // street network onto the walkable area
    Set<Vertex> startingVertices = new HashSet<>();

    // List of edges belonging to the walkable area
    Set<Edge> edges = new HashSet<>();

    // Edges which are part of the rings
    Set<Edge> ringEdges = new HashSet<>();

    HashMap<AreaGroup, HashSet<IntersectionVertex>> visibilityVertexCandidates = new HashMap<>();

    // OSM ways that this area group consists of
    Set<Long> osmWayIds = group.areas
      .stream()
      .map(area -> area.parent)
      .flatMap(osmEntity ->
        osmEntity instanceof OsmRelation relation
          ? relation.getMembers().stream().map(OsmRelationMember::getRef)
          : Stream.of(osmEntity.getId())
      )
      .collect(Collectors.toSet());

    // create polygon and accumulate nodes for area
    for (Ring ring : group.outermostRings) {
      Polygon polygon = ring.jtsPolygon;

      AreaGroup areaGroup = new AreaGroup(polygon);

      // the points corresponding to concave or hole vertices or those linked to ways
      HashSet<NodeEdge> alreadyAddedEdges = new HashSet<>();
      HashSet<IntersectionVertex> platformLinkingVertices = new HashSet<>();
      HashSet<IntersectionVertex> visibilityVertices = new HashSet<>();
      GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
      OsmEntity areaEntity = group.getSomeOsmObject();

      for (OsmArea area : group.areas) {
        // test if area is inside the current ring
        if (!group.isSimpleAreaGroup()) {
          if (!polygon.contains(area.jtsMultiPolygon)) {
            continue;
          }
        }
        // Add stops/entrances from public transit relations into the area
        // they may provide the only entrance to a platform
        // which otherwise would be pruned as unconnected island
        Collection<OsmNode> entrances = osmdb.getStopsInArea(area.parent);
        for (OsmNode node : entrances) {
          var vertex = vertexBuilder.getVertexForOsmNode(node, areaEntity);
          platformLinkingVertices.add(vertex);
          visibilityVertices.add(vertex);
          startingVertices.add(vertex);
        }

        for (Ring outerRing : area.outermostRings) {
          // variable to indicate if some additional entrance points have been added to area
          boolean linkPointsAdded = !entrances.isEmpty();
          // Add unconnected entries to area if platformEntriesLinking parameter is true
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
            edges.addAll(newEdges);
            ringEdges.addAll(newEdges);

            // A node can only be a visibility node only if it is an entrance to the
            // area or a convex point, i.e. the angle is over 180 degrees.
            // Also, if additional linking points have been defined, add some points from outer
            // edge to ensure that platform geometry gets connected
            if (
              outerRing.isNodeConvex(i) ||
              (linkPointsAdded && (i == 0 || i == outerRing.nodes.size() / 2))
            ) {
              visibilityVertices.add(vertexBuilder.getVertexForOsmNode(node, areaEntity));
            }
            if (isStartingNode(node, osmWayIds)) {
              var v = vertexBuilder.getVertexForOsmNode(node, areaEntity);
              startingVertices.add(v);
              visibilityVertices.add(v);
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
              edges.addAll(newEdges);
              ringEdges.addAll(newEdges);
              // A node can only be a visibility node only if it is an entrance to the
              // area or a convex point, i.e. the angle is over 180 degrees.
              // For holes, we must swap the convexity condition
              if (!innerRing.isNodeConvex(j)) {
                visibilityVertices.add(vertexBuilder.getVertexForOsmNode(node, areaEntity));
              }
              if (isStartingNode(node, osmWayIds)) {
                var v = vertexBuilder.getVertexForOsmNode(node, areaEntity);
                startingVertices.add(v);
                visibilityVertices.add(v);
              }
            }
          }
        }
      }

      if (visibilityVertices.isEmpty()) {
        issueStore.add(new UnconnectedArea(group));
        // Area is not connected to graph. Remove it immediately before it causes any trouble.
        for (Edge edge : edges) {
          graph.removeEdge(edge);
        }
        continue;
      }
      if (visibilityVertices.size() > maxAreaNodes) {
        issueStore.add(new AreaTooComplicated(group, visibilityVertices.size(), maxAreaNodes));
      }
      visibilityVertexCandidates.put(areaGroup, visibilityVertices);
      createAreas(areaGroup, ring, group.areas);

      // if area is too complex, consider only part of visibility nodes
      // so that at least some edges passing through the area are added
      // otherwise routing can use only area boundary edges
      float skip_ratio = (float) maxAreaNodes / (float) visibilityVertices.size();
      int i = 0;
      float sum_i = 0;
      for (IntersectionVertex vertex1 : visibilityVertices) {
        sum_i += skip_ratio;
        if (Math.floor(sum_i) < i + 1) {
          continue;
        }
        i = (int) Math.floor(sum_i);
        int j = 0;
        float sum_j = 0;
        for (IntersectionVertex vertex2 : visibilityVertices) {
          sum_j += skip_ratio;
          if (Math.floor(sum_j) < j + 1) {
            continue;
          }
          j = (int) Math.floor(sum_j);
          if (shouldSkipEdge(vertex1, vertex2, alreadyAddedEdges)) {
            continue;
          }
          Coordinate[] coordinates = new Coordinate[] {
            vertex1.getCoordinate(),
            vertex2.getCoordinate(),
          };
          LineString line = geometryFactory.createLineString(coordinates);
          if (polygon.contains(line)) {
            Set<AreaEdge> segments = createSegments(vertex1, vertex2, group.areas, areaGroup, true);
            edges.addAll(segments);
            if (platformLinkingVertices.contains(vertex1)) {
              ringEdges.addAll(segments);
            }
            if (platformLinkingVertices.contains(vertex2)) {
              ringEdges.addAll(segments);
            }
          }
        }
      }
    }
    pruneAreaEdges(startingVertices, edges, ringEdges);

    visibilityVertexCandidates.forEach((areaGroup, vertices) -> {
      if (vertices.size() > maxAreaNodes) {
        // keep nodes which have most connections
        areaGroup.addVisibilityVertices(
          vertices
            .stream()
            .sorted((v1, v2) -> Long.compare((v2.getDegreeOut()), v1.getDegreeOut()))
            .limit(maxAreaNodes)
            .collect(Collectors.toSet())
        );
      } else {
        areaGroup.addVisibilityVertices(vertices);
      }
    });
  }

  /**
   * Do an all-pairs shortest path search from a list of vertices over a specified set of edges,
   *  and retain only those edges which are actually used in some shortest path.
   */
  private void pruneAreaEdges(
    Collection<Vertex> startingVertices,
    Set<Edge> edges,
    Set<Edge> edgesToKeep
  ) {
    if (edges.isEmpty()) return;
    StreetMode mode;
    StreetEdge firstEdge = (StreetEdge) edges.iterator().next();

    if (firstEdge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
      mode = StreetMode.WALK;
    } else if (firstEdge.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
      mode = StreetMode.BIKE;
    } else {
      mode = StreetMode.CAR;
    }
    RouteRequest options = new RouteRequest();
    Set<Edge> usedEdges = new HashSet<>();
    for (Vertex vertex : startingVertices) {
      ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
        .setSkipEdgeStrategy(new ListedEdgesOnly(edges))
        .setDominanceFunction(new DominanceFunctions.EarliestArrival())
        .setRequest(options)
        .setStreetRequest(new StreetRequest(mode))
        .setFrom(vertex)
        .getShortestPathTree();

      for (Vertex endVertex : startingVertices) {
        GraphPath<State, Edge, Vertex> path = spt.getPath(endVertex);
        if (path != null) {
          usedEdges.addAll(path.edges);
        }
      }
    }
    for (Edge edge : edges) {
      if (!usedEdges.contains(edge) && !edgesToKeep.contains(edge)) {
        graph.removeEdge(edge);
      }
    }
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
    if (!wayPropertiesCache.containsKey(entity)) {
      var wayData = entity.getOsmProvider().getWayPropertySet().getDataForWay(entity);
      wayPropertiesCache.put(entity, wayData);
      return wayData;
    } else {
      return wayPropertiesCache.get(entity);
    }
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
    IntersectionVertex v1 = vertexBuilder.getVertexForOsmNode(node, area.parent);
    IntersectionVertex v2 = vertexBuilder.getVertexForOsmNode(nextNode, area.parent);

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
      boolean crosses = testIntersection ? polygon.intersection(line).getLength() > 0.000001 : true;
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
      labelTemplate,
      parent.getId(),
      vertex1.getLabel(),
      vertex2.getLabel()
    );

    float carSpeed = parent.getOsmProvider().getOsmTagMapper().getCarSpeedForWay(parent, false);

    I18NString name = namer.getNameForWay(parent, label);
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

    label = String.format(labelTemplate, parent.getId(), vertex2.getLabel(), vertex1.getLabel());
    name = namer.getNameForWay(parent, label);
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
    normalizer.applyWayProperties(street, backStreet, wayData, parent);
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
      I18NString name = namer.getNameForWay(areaEntity, id);
      namedArea.setName(name);

      WayProperties wayData = findAreaProperties(areaEntity);
      double bicycleSafety = wayData.bicycleSafety().forward();
      namedArea.setBicycleSafetyMultiplier(bicycleSafety);

      double walkSafety = wayData.walkSafety().forward();
      namedArea.setWalkSafetyMultiplier(walkSafety);
      namedArea.setOriginalEdges(intersection);
      namedArea.setPermission(wayData.getPermission());
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

  record ListedEdgesOnly(Set<Edge> edges) implements SkipEdgeStrategy<State, Edge> {
    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
      return !edges.contains(edge);
    }
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

  private record NodeEdge(IntersectionVertex from, IntersectionVertex to) {}
}

package org.opentripplanner.graph_builder.module.osm;

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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaEdgeList;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * Theoretically, it is not correct to build the visibility graph on the joined polygon of areas
 * with different levels of bike safety. That's because in the optimal path, you might end up
 * changing direction at area boundaries. The problem is known as "weighted planar subdivisions",
 * and the best known algorithm is O(N^3). That's not much worse than general visibility graph
 * construction, but it would have to be done at runtime to account for the differences in bike
 * safety preferences. Ted Chiang's "Story Of Your Life" describes how a very similar problem in
 * optics gives rise to Snell's Law. It is the second-best story about a law of physics that I know
 * of (Chiang's "Exhalation" is the first).
 * <p>
 * Anyway, since we're not going to run an O(N^3) algorithm at runtime just to give people who don't
 * understand Snell's Law weird paths that they can complain about, this should be just fine.
 * </p>
 * <p>
 * TODO this approach could be replaced by building a walkable grid of edges for an area, so the
 * number of edges for an area wouldn't be determined by the nodes. The current approach can lead
 * to an excessive number of edges, or to no edges at all if maxAreaNodes is surpassed.
 */
public class WalkableAreaBuilder {

  private final DataImportIssueStore issueStore;

  private final int maxAreaNodes;

  private final Graph graph;

  private final OsmDatabase osmdb;

  private final Map<OSMWithTags, WayProperties> wayPropertiesCache = new HashMap<>();

  private final VertexGenerator vertexBuilder;

  private final HashMap<Coordinate, IntersectionVertex> areaBoundaryVertexForCoordinate = new HashMap<>();

  private final boolean platformEntriesLinking;

  private final List<OsmVertex> platformLinkingEndpoints;
  private final Set<String> boardingLocationRefTags;
  private final EdgeNamer namer;
  private final SafetyValueNormalizer normalizer;
  private final VertexFactory vertexFactory;

  public WalkableAreaBuilder(
    Graph graph,
    OsmDatabase osmdb,
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
    this.vertexBuilder = vertexBuilder;
    this.namer = namer;
    this.normalizer = normalizer;
    this.issueStore = issueStore;
    this.maxAreaNodes = maxAreaNodes;
    this.platformEntriesLinking = platformEntriesLinking;
    this.boardingLocationRefTags = boardingLocationRefTags;
    this.platformLinkingEndpoints =
      platformEntriesLinking
        ? graph
          .getVertices()
          .stream()
          .filter(OsmVertex.class::isInstance)
          .map(OsmVertex.class::cast)
          .filter(this::isPlatformLinkingEndpoint)
          .collect(Collectors.toList())
        : List.of();
    this.vertexFactory = new VertexFactory(graph);
  }

  /**
   * For all areas just use outermost rings as edges so that areas can be routable without
   * visibility calculations
   */
  public void buildWithoutVisibility(AreaGroup group) {
    var references = getStopReferences(group);

    // create polygon and accumulate nodes for area
    for (Ring ring : group.outermostRings) {
      Set<AreaEdge> edges = new HashSet<>();
      AreaEdgeList edgeList = new AreaEdgeList(ring.jtsPolygon, references);
      // the points corresponding to concave or hole vertices
      // or those linked to ways
      HashSet<NodeEdge> alreadyAddedEdges = new HashSet<>();
      // we also want to fill in the edges of this area anyway, because we can,
      // and to avoid the numerical problems that they tend to cause
      for (Area area : group.areas) {
        if (!ring.jtsPolygon.contains(area.jtsMultiPolygon)) {
          continue;
        }

        for (Ring outerRing : area.outermostRings) {
          for (int i = 0; i < outerRing.nodes.size(); ++i) {
            edges.addAll(
              createEdgesForRingSegment(edgeList, area, outerRing, i, alreadyAddedEdges)
            );
          }
          //TODO: is this actually needed?
          for (Ring innerRing : outerRing.getHoles()) {
            for (int j = 0; j < innerRing.nodes.size(); ++j) {
              edges.addAll(
                createEdgesForRingSegment(edgeList, area, innerRing, j, alreadyAddedEdges)
              );
            }
          }
        }
      }
      edges
        .stream()
        .flatMap(v ->
          Stream
            .of(v.getFromVertex(), v.getToVertex())
            .filter(IntersectionVertex.class::isInstance)
            .map(IntersectionVertex.class::cast)
        )
        .forEach(edgeList::addVisibilityVertex);

      createNamedAreas(edgeList, ring, group.areas);
    }
  }

  public void buildWithVisibility(AreaGroup group) {
    // These sets contain the nodes/vertices which can be used to traverse from the rest of the
    // street network onto the walkable area
    Set<OSMNode> startingNodes = new HashSet<>();
    Set<Vertex> startingVertices = new HashSet<>();

    // List of edges belonging to the walkable area
    Set<Edge> edges = new HashSet<>();

    // Edges which are part of the rings. We want to keep there for linking even tough they
    // might not be part of the visibility edges.
    Set<Edge> ringEdges = new HashSet<>();

    // OSM ways that this area group consists of
    Set<Long> osmWayIds = group.areas
      .stream()
      .map(area -> area.parent)
      .flatMap(osmWithTags ->
        osmWithTags instanceof OSMRelation
          ? ((OSMRelation) osmWithTags).getMembers().stream().map(OSMRelationMember::getRef)
          : Stream.of(osmWithTags.getId())
      )
      .collect(Collectors.toSet());

    var references = getStopReferences(group);

    // create polygon and accumulate nodes for area
    for (Ring ring : group.outermostRings) {
      Polygon polygon = ring.jtsPolygon;

      AreaEdgeList edgeList = new AreaEdgeList(polygon, references);

      // the points corresponding to concave or hole vertices
      // or those linked to ways
      HashSet<OSMNode> visibilityNodes = new HashSet<>();
      HashSet<NodeEdge> alreadyAddedEdges = new HashSet<>();
      HashSet<IntersectionVertex> platformLinkingVertices = new HashSet<>();
      // we need to accumulate visibility points from all contained areas
      // inside this ring, but only for shared nodes; we don't care about
      // convexity, which we'll handle for the grouped area only.

      GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

      OSMWithTags areaEntity = group.getSomeOSMObject();

      // we also want to fill in the edges of this area anyway, because we can,
      // and to avoid the numerical problems that they tend to cause
      for (Area area : group.areas) {
        if (!polygon.contains(area.jtsMultiPolygon)) {
          continue;
        }

        // Add stops/entrances from public transit relations into the area
        // they may provide the only entrance to a platform
        // which otherwise would be pruned as unconnected island
        Collection<OSMNode> entrances = osmdb.getStopsInArea(area.parent);
        for (OSMNode node : entrances) {
          var vertex = vertexBuilder.getVertexForOsmNode(node, areaEntity);
          platformLinkingVertices.add(vertex);
          visibilityNodes.add(node);
          startingNodes.add(node);
          edgeList.addVisibilityVertex(vertex);
        }

        for (Ring outerRing : area.outermostRings) {
          // variable to indicate if some additional entrance points have been added to area
          boolean linkPointsAdded = !entrances.isEmpty();
          // Add unconnected entries to area if platformEntriesLinking parameter is true
          if (platformEntriesLinking && area.parent.isPlatform()) {
            List<OsmVertex> endpointsWithin = platformLinkingEndpoints
              .stream()
              .filter(t ->
                outerRing.jtsPolygon.contains(geometryFactory.createPoint(t.getCoordinate()))
              )
              .toList();
            platformLinkingVertices.addAll(endpointsWithin);
            for (OsmVertex v : endpointsWithin) {
              OSMNode node = osmdb.getNode(v.nodeId);
              visibilityNodes.add(node);
              startingNodes.add(node);
              edgeList.addVisibilityVertex(v);
              linkPointsAdded = true;
            }
          }

          for (int i = 0; i < outerRing.nodes.size(); ++i) {
            OSMNode node = outerRing.nodes.get(i);
            Set<AreaEdge> newEdges = createEdgesForRingSegment(
              edgeList,
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
              visibilityNodes.add(node);
              edgeList.addVisibilityVertex(vertexBuilder.getVertexForOsmNode(node, areaEntity));
            }
            if (isStartingNode(node, osmWayIds)) {
              visibilityNodes.add(node);
              startingNodes.add(node);
              edgeList.addVisibilityVertex(vertexBuilder.getVertexForOsmNode(node, areaEntity));
            }
          }
          for (Ring innerRing : outerRing.getHoles()) {
            for (int j = 0; j < innerRing.nodes.size(); ++j) {
              OSMNode node = innerRing.nodes.get(j);
              edges.addAll(
                createEdgesForRingSegment(edgeList, area, innerRing, j, alreadyAddedEdges)
              );
              // A node can only be a visibility node only if it is an entrance to the
              // area or a convex point, i.e. the angle is over 180 degrees.
              // For holes, the internal angle is calculated, so we must swap the sign
              if (!innerRing.isNodeConvex(j)) {
                visibilityNodes.add(node);
                edgeList.addVisibilityVertex(vertexBuilder.getVertexForOsmNode(node, areaEntity));
              }
              if (isStartingNode(node, osmWayIds)) {
                visibilityNodes.add(node);
                startingNodes.add(node);
                edgeList.addVisibilityVertex(vertexBuilder.getVertexForOsmNode(node, areaEntity));
              }
            }
          }
        }
      }

      if (edgeList.visibilityVertices().isEmpty()) {
        issueStore.add(new UnconnectedArea(group));
        // Area is not connected to graph. Remove it immediately before it causes any trouble.
        for (Edge edge : edges) {
          graph.removeEdge(edge);
        }
        continue;
      }

      createNamedAreas(edgeList, ring, group.areas);

      if (visibilityNodes.size() > maxAreaNodes) {
        issueStore.add(new AreaTooComplicated(group, visibilityNodes.size(), maxAreaNodes));
      }

      // if area is too complex, consider only part of visibility nodes
      // so that at least some edges passing through the area is added
      // otherwise routing can use only area boundary edges
      float skip_ratio = (float) maxAreaNodes / (float) visibilityNodes.size();
      int i = 0;
      float sum_i = 0;
      for (OSMNode nodeI : visibilityNodes) {
        sum_i += skip_ratio;
        if (Math.floor(sum_i) < i + 1) {
          continue;
        }
        i = (int) Math.floor(sum_i);
        IntersectionVertex startEndpoint = vertexBuilder.getVertexForOsmNode(nodeI, areaEntity);
        if (startingNodes.contains(nodeI)) {
          startingVertices.add(startEndpoint);
        }
        int j = 0;
        float sum_j = 0;
        for (OSMNode nodeJ : visibilityNodes) {
          sum_j += skip_ratio;
          if (Math.floor(sum_j) < j + 1) {
            continue;
          }
          j = (int) Math.floor(sum_j);
          NodeEdge edge = new NodeEdge(nodeI, nodeJ);
          if (alreadyAddedEdges.contains(edge)) continue;

          IntersectionVertex endEndpoint = vertexBuilder.getVertexForOsmNode(nodeJ, areaEntity);

          Coordinate[] coordinates = new Coordinate[] {
            startEndpoint.getCoordinate(),
            endEndpoint.getCoordinate(),
          };
          LineString line = geometryFactory.createLineString(coordinates);
          if (polygon.contains(line)) {
            Set<AreaEdge> segments = createSegments(
              startEndpoint,
              endEndpoint,
              group.areas,
              edgeList
            );
            edges.addAll(segments);
            if (platformLinkingVertices.contains(startEndpoint)) {
              ringEdges.addAll(segments);
            }
            if (platformLinkingVertices.contains(endEndpoint)) {
              ringEdges.addAll(segments);
            }
          }
        }
      }
    }
    pruneAreaEdges(startingVertices, edges, ringEdges);
  }

  private Set<String> getStopReferences(AreaGroup group) {
    return group.areas
      .stream()
      .filter(g -> g.parent.isBoardingLocation())
      .flatMap(g -> g.parent.getMultiTagValues(boardingLocationRefTags).stream())
      .collect(Collectors.toSet());
  }

  /**
   * Do an all-pairs shortest path search from a list of vertices over a specified set of edges, and
   * retain only those edges which are actually used in some shortest path.
   */
  private void pruneAreaEdges(
    Collection<Vertex> startingVertices,
    Set<Edge> edges,
    Set<Edge> edgesToKeep
  ) {
    if (edges.size() == 0) return;
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
      ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
        .of()
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

  private boolean isStartingNode(OSMNode node, Set<Long> osmWayIds) {
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

  private Set<AreaEdge> createEdgesForRingSegment(
    AreaEdgeList edgeList,
    Area area,
    Ring ring,
    int i,
    HashSet<NodeEdge> alreadyAddedEdges
  ) {
    OSMNode node = ring.nodes.get(i);
    OSMNode nextNode = ring.nodes.get((i + 1) % ring.nodes.size());
    NodeEdge nodeEdge = new NodeEdge(node, nextNode);
    if (alreadyAddedEdges.contains(nodeEdge)) {
      return Set.of();
    }
    alreadyAddedEdges.add(nodeEdge);
    IntersectionVertex startEndpoint = vertexBuilder.getVertexForOsmNode(node, area.parent);
    IntersectionVertex endEndpoint = vertexBuilder.getVertexForOsmNode(nextNode, area.parent);

    return createSegments(startEndpoint, endEndpoint, List.of(area), edgeList);
  }

  private Set<AreaEdge> createSegments(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    Collection<Area> areas,
    AreaEdgeList edgeList
  ) {
    List<Area> intersects = new ArrayList<>();

    Coordinate[] coordinates = new Coordinate[] {
      startEndpoint.getCoordinate(),
      endEndpoint.getCoordinate(),
    };
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    LineString line = geometryFactory.createLineString(coordinates);
    for (Area area : areas) {
      MultiPolygon polygon = area.jtsMultiPolygon;
      Geometry intersection = polygon.intersection(line);
      if (intersection.getLength() > 0.000001) {
        intersects.add(area);
      }
    }
    if (intersects.isEmpty()) {
      // apparently our intersection here was bogus
      return Set.of();
    }
    // do we need to recurse?
    if (intersects.size() == 1) {
      Area area = intersects.get(0);
      OSMWithTags areaEntity = area.parent;

      StreetTraversalPermission areaPermissions = areaEntity.overridePermissions(
        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
      );

      float carSpeed = areaEntity
        .getOsmProvider()
        .getOsmTagMapper()
        .getCarSpeedForWay(areaEntity, false);

      double length = SphericalDistanceLibrary.distance(
        startEndpoint.getCoordinate(),
        endEndpoint.getCoordinate()
      );

      String label =
        "way (area) " +
        areaEntity.getId() +
        " from " +
        startEndpoint.getLabel() +
        " to " +
        endEndpoint.getLabel();
      I18NString name = namer.getNameForWay(areaEntity, label);

      AreaEdgeBuilder streetEdgeBuilder = new AreaEdgeBuilder()
        .withFromVertex(startEndpoint)
        .withToVertex(endEndpoint)
        .withGeometry(line)
        .withName(name)
        .withMeterLength(length)
        .withPermission(areaPermissions)
        .withBack(false)
        .withArea(edgeList)
        .withCarSpeed(carSpeed);

      if (!areaEntity.hasTag("name") && !areaEntity.hasTag("ref")) {
        streetEdgeBuilder.withBogusName(true);
      }

      streetEdgeBuilder.withWheelchairAccessible(areaEntity.isWheelchairAccessible());

      streetEdgeBuilder.withLink(areaEntity.isLink());

      label =
        "way (area) " +
        areaEntity.getId() +
        " from " +
        endEndpoint.getLabel() +
        " to " +
        startEndpoint.getLabel();
      name = namer.getNameForWay(areaEntity, label);

      AreaEdgeBuilder backStreetEdgeBuilder = new AreaEdgeBuilder()
        .withFromVertex(endEndpoint)
        .withToVertex(startEndpoint)
        .withGeometry(line.reverse())
        .withName(name)
        .withMeterLength(length)
        .withPermission(areaPermissions)
        .withBack(true)
        .withArea(edgeList)
        .withCarSpeed(carSpeed);

      if (!areaEntity.hasTag("name") && !areaEntity.hasTag("ref")) {
        backStreetEdgeBuilder.withBogusName(true);
      }

      backStreetEdgeBuilder.withWheelchairAccessible(areaEntity.isWheelchairAccessible());

      backStreetEdgeBuilder.withLink(areaEntity.isLink());

      if (!wayPropertiesCache.containsKey(areaEntity)) {
        WayProperties wayData = areaEntity
          .getOsmProvider()
          .getWayPropertySet()
          .getDataForWay(areaEntity);
        wayPropertiesCache.put(areaEntity, wayData);
      }

      AreaEdge street = streetEdgeBuilder.buildAndConnect();
      AreaEdge backStreet = backStreetEdgeBuilder.buildAndConnect();
      normalizer.applyWayProperties(
        street,
        backStreet,
        wayPropertiesCache.get(areaEntity),
        areaEntity
      );
      return Set.of(street, backStreet);
    } else {
      // take the part that intersects with the start vertex
      Coordinate startCoordinate = startEndpoint.getCoordinate();
      Point startPoint = geometryFactory.createPoint(startCoordinate);
      Set<AreaEdge> edges = new HashSet<>();
      for (Area area : intersects) {
        MultiPolygon polygon = area.jtsMultiPolygon;
        if (
          !(polygon.intersects(startPoint) || polygon.getBoundary().intersects(startPoint))
        ) continue;
        Geometry lineParts = line.intersection(polygon);
        if (lineParts.getLength() > 0.000001) {
          Coordinate edgeCoordinate = null;
          // this is either a LineString or a MultiLineString (we hope)
          if (lineParts instanceof MultiLineString mls) {
            boolean found = false;
            for (int i = 0; i < mls.getNumGeometries(); ++i) {
              LineString segment = (LineString) mls.getGeometryN(i);
              if (found) {
                edgeCoordinate = segment.getEndPoint().getCoordinate();
                break;
              }
              if (segment.contains(startPoint) || segment.getBoundary().contains(startPoint)) {
                found = true;
                if (segment.getLength() > 0.000001) {
                  edgeCoordinate = segment.getEndPoint().getCoordinate();
                  break;
                }
              }
            }
          } else if (lineParts instanceof LineString lineString) {
            edgeCoordinate = lineString.getEndPoint().getCoordinate();
          } else {
            continue;
          }

          IntersectionVertex newEndpoint = areaBoundaryVertexForCoordinate.get(edgeCoordinate);
          if (newEndpoint == null) {
            newEndpoint = vertexFactory.intersection(edgeCoordinate);
            areaBoundaryVertexForCoordinate.put(edgeCoordinate, newEndpoint);
          }
          edges.addAll(createSegments(startEndpoint, newEndpoint, List.of(area), edgeList));
          edges.addAll(createSegments(newEndpoint, endEndpoint, intersects, edgeList));
          return edges;
        }
      }
    }
    return Set.of();
  }

  private void createNamedAreas(AreaEdgeList edgeList, Ring ring, Collection<Area> areas) {
    Polygon containingArea = ring.jtsPolygon;
    for (Area area : areas) {
      Geometry intersection = containingArea.intersection(area.jtsMultiPolygon);
      if (intersection.getArea() == 0) {
        continue;
      }
      NamedArea namedArea = new NamedArea();
      OSMWithTags areaEntity = area.parent;

      String id = "way (area) " + areaEntity.getId() + " (splitter linking)";
      I18NString name = namer.getNameForWay(areaEntity, id);
      namedArea.setName(name);

      if (!wayPropertiesCache.containsKey(areaEntity)) {
        WayProperties wayData = areaEntity
          .getOsmProvider()
          .getWayPropertySet()
          .getDataForWay(areaEntity);
        wayPropertiesCache.put(areaEntity, wayData);
      }

      double bicycleSafety = wayPropertiesCache.get(areaEntity).bicycleSafety().forward();
      namedArea.setBicycleSafetyMultiplier(bicycleSafety);

      double walkSafety = wayPropertiesCache.get(areaEntity).walkSafety().forward();
      namedArea.setWalkSafetyMultiplier(walkSafety);

      namedArea.setOriginalEdges(intersection);

      StreetTraversalPermission permission = areaEntity.overridePermissions(
        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
      );
      namedArea.setPermission(permission);

      edgeList.addArea(namedArea);
    }
  }

  private boolean isPlatformLinkingEndpoint(OsmVertex osmVertex) {
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
      boolean isEndpoint = true;
      for (Edge se : osmVertex.getOutgoing()) {
        if (
          !se.getToVertex().getCoordinate().equals(start.getCoordinate()) &&
          !(se instanceof AreaEdge)
        ) {
          isEndpoint = false;
        }
      }
      return isEndpoint;
    }
    return false;
  }

  record ListedEdgesOnly(Set<Edge> edges) implements SkipEdgeStrategy<State, Edge> {
    @Override
    public boolean shouldSkipEdge(State current, Edge edge) {
      return !edges.contains(edge);
    }
  }

  private record NodeEdge(OSMNode from, OSMNode to) {}
}

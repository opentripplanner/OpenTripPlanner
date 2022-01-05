package org.opentripplanner.graph_builder.module.osm;

import java.util.Map;
import java.util.Objects;
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
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.AreaTooComplicated;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule.Handler;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.NamedArea;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.util.I18NString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 *
 * TODO this approach could be replaced by building a walkable grid of edges for an area, so the
 * number of edges for an area wouldn't be determined by the nodes. The current approach can lead
 * to an excessive number of edges, or to no edges at all if maxAreaNodes is surpassed.
 * 
 */
public class WalkableAreaBuilder {

    private static Logger LOG = LoggerFactory.getLogger(WalkableAreaBuilder.class);

    private DataImportIssueStore issueStore;

    private final int maxAreaNodes;

    private Graph graph;

    private OSMDatabase osmdb;

    private WayPropertySet wayPropertySet;

    private Map<OSMWithTags, WayProperties> wayPropertiesCache = new HashMap<>();

    private StreetEdgeFactory edgeFactory;

    // This is an awful hack, but this class (WalkableAreaBuilder) ought to be rewritten.
    private Handler handler;

    private HashMap<Coordinate, IntersectionVertex> areaBoundaryVertexForCoordinate = new HashMap<Coordinate, IntersectionVertex>();

    private boolean platformEntriesLinking;

    private List<OsmVertex> platformLinkingEndpoints;

    public WalkableAreaBuilder(Graph graph, OSMDatabase osmdb, WayPropertySet wayPropertySet,
            StreetEdgeFactory edgeFactory, Handler handler, DataImportIssueStore issueStore,
            int maxAreaNodes, boolean platformEntriesLinking
    ) {
        this.graph = graph;
        this.osmdb = osmdb;
        this.wayPropertySet = wayPropertySet;
        this.edgeFactory = edgeFactory;
        this.handler = handler;
        this.issueStore = issueStore;
        this.maxAreaNodes = maxAreaNodes;
        this.platformEntriesLinking = platformEntriesLinking;
        this.platformLinkingEndpoints = platformEntriesLinking
            ? graph.getVertices().stream().
                filter(OsmVertex.class::isInstance).
                map(OsmVertex.class::cast).
                filter(this::isPlatformLinkingEndpoint).
                collect(Collectors.toList())
            : List.of();
    }

    /**
     * For all areas just use outermost rings as edges so that areas can be routable without visibility calculations
     * @param group
     */
    public void buildWithoutVisibility(AreaGroup group) {
        Set<Edge> edges = new HashSet<Edge>();

        // create polygon and accumulate nodes for area
        for (Ring ring : group.outermostRings) {

            AreaEdgeList edgeList = new AreaEdgeList(ring.jtsPolygon);
            // the points corresponding to concave or hole vertices
            // or those linked to ways
            HashSet<P2<OSMNode>> alreadyAddedEdges = new HashSet<P2<OSMNode>>();

            // we also want to fill in the edges of this area anyway, because we can,
            // and to avoid the numerical problems that they tend to cause
            for (Area area : group.areas) {

                if (!ring.jtsPolygon.contains(area.jtsMultiPolygon)) {
                    continue;
                }

                for (Ring outerRing : area.outermostRings) {
                    for (int i = 0; i < outerRing.nodes.size(); ++i) {
                        edges.addAll(createEdgesForRingSegment(edgeList, area, outerRing, i, alreadyAddedEdges));
                    }
                    //TODO: is this actually needed?
                    for (Ring innerRing : outerRing.getHoles()) {
                        for (int j = 0; j < innerRing.nodes.size(); ++j) {
                            edges.addAll(createEdgesForRingSegment(edgeList, area, innerRing, j,
                                alreadyAddedEdges));
                        }
                    }
                }
            }
        }
    }

    public void buildWithVisibility(AreaGroup group) {
        // These sets contain the nodes/vertices which can be used to traverse from the rest of the
        // street network onto the walkable area
        Set<OSMNode> startingNodes = new HashSet<OSMNode>();
        Set<Vertex> startingVertices = new HashSet<Vertex>();

        // List of edges belonging to the walkable area
        Set<Edge> edges = new HashSet<Edge>();

        // Edges which are part of the rings. We want to keep there for linking even tough they
        // might not be part of the visibility edges.
        Set<Edge> ringEdges = new HashSet<>();

        // OSM ways that this area group consists of
        Set<Long> osmWayIds = group.areas.stream()
                .map(area -> area.parent)
                .flatMap(osmWithTags -> osmWithTags instanceof OSMRelation
                        ? ((OSMRelation) osmWithTags).getMembers().stream().map(OSMRelationMember::getRef)
                        : Stream.of(osmWithTags.getId())
                )
                .collect(Collectors.toSet());


        // create polygon and accumulate nodes for area
        for (Ring ring : group.outermostRings) {
            Polygon polygon = ring.jtsPolygon;
            AreaEdgeList edgeList = new AreaEdgeList(polygon);

            // the points corresponding to concave or hole vertices
            // or those linked to ways
            HashSet<OSMNode> visibilityNodes = new HashSet<OSMNode>();
            HashSet<P2<OSMNode>> alreadyAddedEdges = new HashSet<P2<OSMNode>>();
            HashSet<OsmVertex> platformLinkingVertices = new HashSet<>();
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

                // Add stops from public transit relations into the area
                Collection<OSMNode> nodes = osmdb.getStopsInArea(area.parent);
                if (nodes != null) {
                    for (OSMNode node : nodes) {
                        OsmVertex vertex = handler.getVertexForOsmNode(node, areaEntity);
                        platformLinkingVertices.add(vertex);
                        visibilityNodes.add(node);
                        startingNodes.add(node);
                        edgeList.visibilityVertices.add(vertex);
                    }
                }

                for (Ring outerRing : area.outermostRings) {
                    // Add unconnected entries to area if platformEntriesLinking parameter is true
                    if (platformEntriesLinking
                            && "platform".equals(area.parent.getTag("public_transport"))) {
                        List<OsmVertex> endpointsWithin = platformLinkingEndpoints.stream()
                            .filter(t -> outerRing.jtsPolygon
                                    .contains(geometryFactory.createPoint(t.getCoordinate())))
                            .collect(Collectors.toList());
                        platformLinkingVertices.addAll(endpointsWithin);
                        for (OsmVertex v : endpointsWithin) {
                            OSMNode node = osmdb.getNode(v.nodeId);
                            visibilityNodes.add(node);
                            startingNodes.add(node);
                            edgeList.visibilityVertices.add(v);
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
                        if (outerRing.isNodeConvex(i)) {
                            visibilityNodes.add(node);
                        }
                        if (isStartingNode(node, osmWayIds)) {
                            visibilityNodes.add(node);
                            startingNodes.add(node);
                            edgeList.visibilityVertices.add(handler.getVertexForOsmNode(node, areaEntity));
                        }
                    }
                    for (Ring innerRing : outerRing.getHoles()) {
                        for (int j = 0; j < innerRing.nodes.size(); ++j) {
                            OSMNode node = innerRing.nodes.get(j);
                            edges.addAll(createEdgesForRingSegment(edgeList, area, innerRing, j,
                                    alreadyAddedEdges));
                            // A node can only be a visibility node only if it is an entrance to the
                            // area or a convex point, i.e. the angle is over 180 degrees.
                            // For holes, the internal angle is calculated, so we must swap the sign
                            if (!innerRing.isNodeConvex(j)) {
                                visibilityNodes.add(node);
                            }
                            if (isStartingNode(node, osmWayIds)) {
                                visibilityNodes.add(node);
                                startingNodes.add(node);
                                edgeList.visibilityVertices.add(handler.getVertexForOsmNode(node, areaEntity));
                            }
                        }
                    }
                }
            }

            // FIXME: temporary hard limit on size of
            // areas to prevent way explosion
            if (polygon.getNumPoints() > maxAreaNodes) {
                issueStore.add(
                        new AreaTooComplicated(
                                group.getSomeOSMObject().getId(), visibilityNodes.size(),
                                maxAreaNodes
                        ));
                continue;
            }

            if (edgeList.visibilityVertices.size() == 0) {
                issueStore.add(
                    "UnconnectedArea",
                    "Area %s has no connection to street network",
                    osmWayIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
            }

            createNamedAreas(edgeList, ring, group.areas);

            for (OSMNode nodeI : visibilityNodes) {
                IntersectionVertex startEndpoint = handler.getVertexForOsmNode(nodeI, areaEntity);
                if (startingNodes.contains(nodeI)) {
                    startingVertices.add(startEndpoint);
                }

                for (OSMNode nodeJ : visibilityNodes) {
                    P2<OSMNode> nodePair = new P2<OSMNode>(nodeI, nodeJ);
                    if (alreadyAddedEdges.contains(nodePair))
                        continue;

                    IntersectionVertex endEndpoint = handler.getVertexForOsmNode(nodeJ,
                            areaEntity);

                    Coordinate[] coordinates = new Coordinate[]{
                            startEndpoint.getCoordinate(),
                            endEndpoint.getCoordinate()
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

    class ListedEdgesOnly implements SkipEdgeStrategy {
        private Set<Edge> edges;

        public ListedEdgesOnly(Set<Edge> edges) {
            this.edges = edges;
        }

        @Override
        public boolean shouldSkipEdge(
            Set<Vertex> origins,
            Set<Vertex> targets,
            State current,
            Edge edge,
            ShortestPathTree spt,
            RoutingRequest traverseOptions
        ) {
            return !edges.contains(edge);
        }
    }

    /**
     * Do an all-pairs shortest path search from a list of vertices over a specified set of edges,
     * and retain only those edges which are actually used in some shortest path.
     * 
     * @param startingVertices
     * @param edges
     */
    private void pruneAreaEdges(Collection<Vertex> startingVertices, Set<Edge> edges, Set<Edge> edgesToKeep) {
        if (edges.size() == 0)
            return;
        TraverseMode mode;
        StreetEdge firstEdge = (StreetEdge) edges.iterator().next();

        if (firstEdge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
            mode = TraverseMode.WALK;
        } else if (firstEdge.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
            mode = TraverseMode.BICYCLE;
        } else {
            mode = TraverseMode.CAR;
        }
        RoutingRequest options = new RoutingRequest(mode);
        options.dominanceFunction = new DominanceFunction.EarliestArrival();
        options.setDummyRoutingContext(graph);
        AStar search = new AStar();
        search.setSkipEdgeStrategy(new ListedEdgesOnly(edges));
        Set<Edge> usedEdges = new HashSet<Edge>();
        for (Vertex vertex : startingVertices) {
            options.setRoutingContext(graph, vertex, null);
            options.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
            ShortestPathTree spt = search.getShortestPathTree(options);
            for (Vertex endVertex : startingVertices) {
                GraphPath path = spt.getPath(endVertex, false);
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
        return osmdb.isNodeBelongsToWay(node.getId())
            // Do not add if part of same areaGroup
            || !osmdb.getAreasForNode(node.getId()).stream()
                .allMatch(osmWay -> osmWayIds.contains(osmWay.getId()))
            || node.isStop();
    }

    private Set<AreaEdge> createEdgesForRingSegment(AreaEdgeList edgeList, Area area,
            Ring ring, int i, HashSet<P2<OSMNode>> alreadyAddedEdges) {
        OSMNode node = ring.nodes.get(i);
        OSMNode nextNode = ring.nodes.get((i + 1) % ring.nodes.size());
        P2<OSMNode> nodePair = new P2<OSMNode>(node, nextNode);
        if (alreadyAddedEdges.contains(nodePair)) {
            return Set.of();
        }
        alreadyAddedEdges.add(nodePair);
        IntersectionVertex startEndpoint = handler.getVertexForOsmNode(node, area.parent);
        IntersectionVertex endEndpoint = handler.getVertexForOsmNode(nextNode, area.parent);

        return createSegments(startEndpoint, endEndpoint, List.of(area), edgeList);
    }

    private Set<AreaEdge> createSegments(IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
            Collection<Area> areas, AreaEdgeList edgeList
    ) {
        List<Area> intersects = new ArrayList<Area>();

        Coordinate[] coordinates = new Coordinate[] { startEndpoint.getCoordinate(),
                endEndpoint.getCoordinate() };
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        LineString line = geometryFactory.createLineString(coordinates);
        for (Area area : areas) {
            MultiPolygon polygon = area.jtsMultiPolygon;
            Geometry intersection = polygon.intersection(line);
            if (intersection.getLength() > 0.000001) {
                intersects.add(area);
            }
        }
        if (intersects.size() == 0) {
            // apparently our intersection here was bogus
            return Set.of();
        }
        // do we need to recurse?
        if (intersects.size() == 1) {
            Area area = intersects.get(0);
            OSMWithTags areaEntity = area.parent;

            StreetTraversalPermission areaPermissions = OSMFilter.getPermissionsForEntity(
                    areaEntity, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

            float carSpeed = wayPropertySet.getCarSpeedForWay(areaEntity, false);

            double length = SphericalDistanceLibrary.distance(startEndpoint.getCoordinate(),
                    endEndpoint.getCoordinate());

            int cls = StreetEdge.CLASS_OTHERPATH;
            cls |= OSMFilter.getStreetClasses(areaEntity);

            String label = "way (area) " + areaEntity.getId() + " from " + startEndpoint.getLabel()
                    + " to " + endEndpoint.getLabel();
            I18NString name = handler.getNameForWay(areaEntity, label);

            AreaEdge street = edgeFactory.createAreaEdge(startEndpoint, endEndpoint, line, name,
                    length, areaPermissions, false, edgeList);
            street.setCarSpeed(carSpeed);

            if (!areaEntity.hasTag("name") && !areaEntity.hasTag("ref")) {
                street.setHasBogusName(true);
            }

            if (areaEntity.isTagFalse("wheelchair")) {
                street.setWheelchairAccessible(false);
            }

            street.setStreetClass(cls);

            label = "way (area) " + areaEntity.getId() + " from " + endEndpoint.getLabel() + " to "
                    + startEndpoint.getLabel();
            name = handler.getNameForWay(areaEntity, label);

            AreaEdge backStreet = edgeFactory.createAreaEdge(endEndpoint, startEndpoint,
                    (LineString) line.reverse(), name, length, areaPermissions, true, edgeList);
            backStreet.setCarSpeed(carSpeed);

            if (!areaEntity.hasTag("name") && !areaEntity.hasTag("ref")) {
                backStreet.setHasBogusName(true);
            }

            if (areaEntity.isTagFalse("wheelchair")) {
                backStreet.setWheelchairAccessible(false);
            }

            backStreet.setStreetClass(cls);

            if (!wayPropertiesCache.containsKey(areaEntity)) {
                WayProperties wayData = wayPropertySet.getDataForWay(areaEntity);
                wayPropertiesCache.put(areaEntity, wayData);
            }

            handler.applyWayProperties(
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
                if (!(polygon.intersects(startPoint) || polygon.getBoundary()
                        .intersects(startPoint)))
                    continue;
                Geometry lineParts = line.intersection(polygon);
                if (lineParts.getLength() > 0.000001) {
                    Coordinate edgeCoordinate = null;
                    // this is either a LineString or a MultiLineString (we hope)
                    if (lineParts instanceof MultiLineString) {
                        MultiLineString mls = (MultiLineString) lineParts;
                        boolean found = false;
                        for (int i = 0; i < mls.getNumGeometries(); ++i) {
                            LineString segment = (LineString) mls.getGeometryN(i);
                            if (found) {
                                edgeCoordinate = segment.getEndPoint().getCoordinate();
                                break;
                            }
                            if (segment.contains(startPoint)
                                    || segment.getBoundary().contains(startPoint)) {
                                found = true;
                                if (segment.getLength() > 0.000001) {
                                    edgeCoordinate = segment.getEndPoint().getCoordinate();
                                    break;
                                }
                            }
                        }
                    } else if (lineParts instanceof LineString) {
                        edgeCoordinate = ((LineString) lineParts).getEndPoint().getCoordinate();
                    } else {
                        continue;
                    }

                    IntersectionVertex newEndpoint = areaBoundaryVertexForCoordinate
                            .get(edgeCoordinate);
                    if (newEndpoint == null) {
                        newEndpoint = new IntersectionVertex(graph, "area splitter at "
                                + edgeCoordinate, edgeCoordinate.x, edgeCoordinate.y);
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
            int cls = StreetEdge.CLASS_OTHERPATH;
            cls |= OSMFilter.getStreetClasses(areaEntity);
            namedArea.setStreetClass(cls);

            String id = "way (area) " + areaEntity.getId() + " (splitter linking)";
            I18NString name = handler.getNameForWay(areaEntity, id);
            namedArea.setName(name);

            if (!wayPropertiesCache.containsKey(areaEntity)) {
                WayProperties wayData = wayPropertySet.getDataForWay(areaEntity);
                wayPropertiesCache.put(areaEntity, wayData);
            }

            Double safety = wayPropertiesCache.get(areaEntity).getSafetyFeatures().first;
            namedArea.setBicycleSafetyMultiplier(safety);

            namedArea.setOriginalEdges(intersection);

            StreetTraversalPermission permission = OSMFilter.getPermissionsForEntity(areaEntity,
                    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
            namedArea.setPermission(permission);

            edgeList.addArea(namedArea);
        }
    }

    private boolean isPlatformLinkingEndpoint(OsmVertex osmVertex) {
        boolean isCandidate = false;
        Vertex start = null;
        for (Edge e : osmVertex.getIncoming()) {
            if (e instanceof StreetEdge && ! (e instanceof AreaEdge)) {
                StreetEdge se = (StreetEdge) e;
                if (Arrays.asList(1,2,3).contains(se.getPermission().code)) {
                    isCandidate = true;
                    start = se.getFromVertex();
                    break;
                }
            }
        }

        if (isCandidate && start != null) {
            boolean isEndpoint = true;
            for (Edge se : osmVertex.getOutgoing()) {
                if (!se.getToVertex().getCoordinate().equals(start.getCoordinate()) && !(se instanceof AreaEdge)) {
                    isEndpoint = false;
                }
            }
            return isEndpoint;
        }
        return false;
    }
}

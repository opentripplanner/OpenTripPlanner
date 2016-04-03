/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule.Handler;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
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
import org.opentripplanner.routing.spt.DominanceFunction.EarliestArrival;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.visibility.Environment;
import org.opentripplanner.visibility.VLPoint;
import org.opentripplanner.visibility.VLPolygon;
import org.opentripplanner.visibility.VisibilityPolygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.opentripplanner.util.I18NString;

/**
 * Theoretically, it is not correct to build the visibility graph on the joined polygon of areas
 * with different levels of bike safety. That's because in the optimal path, you might end up
 * changing direction at area boundaries. The problem is known as "weighted planar subdivisions",
 * and the best known algorithm is O(N^3). That's not much worse than general visibility graph
 * construction, but it would have to be done at runtime to account for the differences in bike
 * safety preferences. Ted Chiang's "Story Of Your Life" describes how a very similar problem in
 * optics gives rise to Snell's Law. It is the second-best story about a law of physics that I know
 * of (Chiang's "Exhalation" is the first).
 * <p/>
 * Anyway, since we're not going to run an O(N^3) algorithm at runtime just to give people who don't
 * understand Snell's Law weird paths that they can complain about, this should be just fine.
 * 
 */
public class WalkableAreaBuilder {

    private static Logger LOG = LoggerFactory.getLogger(WalkableAreaBuilder.class);

    private final int MAX_AREA_NODES = 500;

    private static final double VISIBILITY_EPSILON = 0.000000001;

    private Graph graph;

    private OSMDatabase osmdb;

    private WayPropertySet wayPropertySet;

    private StreetEdgeFactory edgeFactory;

    // This is an awful hack, but this class (WalkableAreaBuilder) ought to be rewritten.
    private Handler __handler;

    private HashMap<Coordinate, IntersectionVertex> areaBoundaryVertexForCoordinate = new HashMap<Coordinate, IntersectionVertex>();

    public WalkableAreaBuilder(Graph graph, OSMDatabase osmdb, WayPropertySet wayPropertySet,
            StreetEdgeFactory edgeFactory, Handler __handler) {
        this.graph = graph;
        this.osmdb = osmdb;
        this.wayPropertySet = wayPropertySet;
        this.edgeFactory = edgeFactory;
        this.__handler = __handler;
    }

    /**
     * For all areas just use outermost rings as edges so that areas can be routable without visibility calculations
     * @param group
     */
    public void buildWithoutVisibility(AreaGroup group) {
        Set<Edge> edges = new HashSet<Edge>();

        // create polygon and accumulate nodes for area
        for (Ring ring : group.outermostRings) {

            AreaEdgeList edgeList = new AreaEdgeList();
            // the points corresponding to concave or hole vertices
            // or those linked to ways
            HashSet<P2<OSMNode>> alreadyAddedEdges = new HashSet<P2<OSMNode>>();

            // we also want to fill in the edges of this area anyway, because we can,
            // and to avoid the numerical problems that they tend to cause
            for (Area area : group.areas) {
                if (!ring.toJtsPolygon().contains(area.toJTSMultiPolygon())) {
                    continue;
                }

                for (Ring outerRing : area.outermostRings) {
                    for (int i = 0; i < outerRing.nodes.size(); ++i) {
                        createEdgesForRingSegment(edges, edgeList, area, outerRing, i,
                            alreadyAddedEdges);
                    }
                    //TODO: is this actually needed?
                    for (Ring innerRing : outerRing.holes) {
                        for (int j = 0; j < innerRing.nodes.size(); ++j) {
                            createEdgesForRingSegment(edges, edgeList, area, innerRing, j,
                                alreadyAddedEdges);
                        }
                    }
                }
            }
        }
    }

    public void buildWithVisibility(AreaGroup group) {
        Set<OSMNode> startingNodes = new HashSet<OSMNode>();
        Set<Vertex> startingVertices = new HashSet<Vertex>();
        Set<Edge> edges = new HashSet<Edge>();

        // create polygon and accumulate nodes for area
        for (Ring ring : group.outermostRings) {

            AreaEdgeList edgeList = new AreaEdgeList();
            // the points corresponding to concave or hole vertices
            // or those linked to ways
            ArrayList<VLPoint> visibilityPoints = new ArrayList<VLPoint>();
            ArrayList<OSMNode> visibilityNodes = new ArrayList<OSMNode>();
            HashSet<P2<OSMNode>> alreadyAddedEdges = new HashSet<P2<OSMNode>>();
            // we need to accumulate visibility points from all contained areas
            // inside this ring, but only for shared nodes; we don't care about
            // convexity, which we'll handle for the grouped area only.

            // we also want to fill in the edges of this area anyway, because we can,
            // and to avoid the numerical problems that they tend to cause
            for (Area area : group.areas) {
                if (!ring.toJtsPolygon().contains(area.toJTSMultiPolygon())) {
                    continue;
                }

                // Add stops from public transit relations into the area
                Collection<OSMNode> nodes = osmdb.getStopsInArea(area.parent);
                if (nodes != null) {
                    for (OSMNode node : nodes) {
                        addtoVisibilityAndStartSets(startingNodes, visibilityPoints,
                                visibilityNodes, node);
                    }
                }

                for (Ring outerRing : area.outermostRings) {
                    for (int i = 0; i < outerRing.nodes.size(); ++i) {
                        OSMNode node = outerRing.nodes.get(i);
                        createEdgesForRingSegment(edges, edgeList, area, outerRing, i,
                                alreadyAddedEdges);
                        addtoVisibilityAndStartSets(startingNodes, visibilityPoints,
                                visibilityNodes, node);
                    }
                    for (Ring innerRing : outerRing.holes) {
                        for (int j = 0; j < innerRing.nodes.size(); ++j) {
                            OSMNode node = innerRing.nodes.get(j);
                            createEdgesForRingSegment(edges, edgeList, area, innerRing, j,
                                    alreadyAddedEdges);
                            addtoVisibilityAndStartSets(startingNodes, visibilityPoints,
                                    visibilityNodes, node);
                        }
                    }
                }
            }
            List<OSMNode> nodes = new ArrayList<OSMNode>();
            List<VLPoint> vertices = new ArrayList<VLPoint>();
            accumulateRingNodes(ring, nodes, vertices);
            VLPolygon polygon = makeStandardizedVLPolygon(vertices, nodes, false);
            accumulateVisibilityPoints(ring.nodes, polygon, visibilityPoints, visibilityNodes,
                    false);

            ArrayList<VLPolygon> polygons = new ArrayList<VLPolygon>();
            polygons.add(polygon);
            // holes
            for (Ring innerRing : ring.holes) {
                ArrayList<OSMNode> holeNodes = new ArrayList<OSMNode>();
                vertices = new ArrayList<VLPoint>();
                accumulateRingNodes(innerRing, holeNodes, vertices);
                VLPolygon hole = makeStandardizedVLPolygon(vertices, holeNodes, true);
                accumulateVisibilityPoints(innerRing.nodes, hole, visibilityPoints,
                        visibilityNodes, true);
                nodes.addAll(holeNodes);
                polygons.add(hole);
            }

            Environment areaEnv = new Environment(polygons);
            // FIXME: temporary hard limit on size of
            // areas to prevent way explosion
            if (visibilityPoints.size() > MAX_AREA_NODES) {
                LOG.warn("Area " + group.getSomeOSMObject() + " is too complicated ("
                        + visibilityPoints.size() + " > " + MAX_AREA_NODES);
                continue;
            }

            if (!areaEnv.is_valid(VISIBILITY_EPSILON)) {
                LOG.warn("Area " + group.getSomeOSMObject() + " is not epsilon-valid (epsilon = "
                        + VISIBILITY_EPSILON + ")");
                continue;
            }

            edgeList.setOriginalEdges(ring.toJtsPolygon());

            createNamedAreas(edgeList, ring, group.areas);

            OSMWithTags areaEntity = group.getSomeOSMObject();

            for (int i = 0; i < visibilityNodes.size(); ++i) {
                OSMNode nodeI = visibilityNodes.get(i);
                VisibilityPolygon visibilityPolygon = new VisibilityPolygon(
                        visibilityPoints.get(i), areaEnv, VISIBILITY_EPSILON);
                Polygon poly = toJTSPolygon(visibilityPolygon);
                for (int j = 0; j < visibilityNodes.size(); ++j) {
                    OSMNode nodeJ = visibilityNodes.get(j);
                    P2<OSMNode> nodePair = new P2<OSMNode>(nodeI, nodeJ);
                    if (alreadyAddedEdges.contains(nodePair))
                        continue;

                    IntersectionVertex startEndpoint = __handler.getVertexForOsmNode(nodeI,
                            areaEntity);
                    IntersectionVertex endEndpoint = __handler.getVertexForOsmNode(nodeJ,
                            areaEntity);

                    Coordinate[] coordinates = new Coordinate[] { startEndpoint.getCoordinate(),
                            endEndpoint.getCoordinate() };
                    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
                    LineString line = geometryFactory.createLineString(coordinates);
                    if (poly != null && poly.contains(line)) {

                        createSegments(nodeI, nodeJ, startEndpoint, endEndpoint, group.areas,
                                edgeList, edges);
                        if (startingNodes.contains(nodeI)) {
                            startingVertices.add(startEndpoint);
                        }
                        if (startingNodes.contains(nodeJ)) {
                            startingVertices.add(endEndpoint);
                        }
                    }
                }
            }
        }
        pruneAreaEdges(startingVertices, edges);
    }

    class ListedEdgesOnly implements SkipEdgeStrategy {
        private Set<Edge> edges;

        public ListedEdgesOnly(Set<Edge> edges) {
            this.edges = edges;
        }

        @Override
        public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge,
                ShortestPathTree spt, RoutingRequest traverseOptions) {
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
    private void pruneAreaEdges(Collection<Vertex> startingVertices, Set<Edge> edges) {
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
        options.setDummyRoutingContext(graph);
        options.dominanceFunction = new DominanceFunction.EarliestArrival();
        GenericDijkstra search = new GenericDijkstra(options);
        search.setSkipEdgeStrategy(new ListedEdgesOnly(edges));
        Set<Edge> usedEdges = new HashSet<Edge>();
        for (Vertex vertex : startingVertices) {
            State state = new State(vertex, options);
            ShortestPathTree spt = search.getShortestPathTree(state);
            for (Vertex endVertex : startingVertices) {
                GraphPath path = spt.getPath(endVertex, false);
                if (path != null) {
                    for (Edge edge : path.edges) {
                        usedEdges.add(edge);
                    }
                }
            }
        }
        for (Edge edge : edges) {
            if (!usedEdges.contains(edge)) {
                graph.removeEdge(edge);
            }
        }
    }

    private void addtoVisibilityAndStartSets(Set<OSMNode> startingNodes,
            ArrayList<VLPoint> visibilityPoints, ArrayList<OSMNode> visibilityNodes, OSMNode node) {
        if (osmdb.isNodeBelongsToWay(node.getId())
                || osmdb.isNodeSharedByMultipleAreas(node.getId()) || node.isStop()) {
            startingNodes.add(node);
            VLPoint point = new VLPoint(node.lon, node.lat);
            if (!visibilityPoints.contains(point)) {
                visibilityPoints.add(point);
                visibilityNodes.add(node);
            }
        }
    }

    private Polygon toJTSPolygon(VLPolygon visibilityPolygon) {
        if (visibilityPolygon.vertices.isEmpty()) {
            return null;
        }
        // incomprehensibly, visilibity's routines for figuring out point-polygon containment are
        // too broken
        // to use here, so we have to fall back to JTS.
        Coordinate[] coordinates = new Coordinate[visibilityPolygon.n() + 1];

        for (int p = 0; p < coordinates.length; ++p) {
            VLPoint vlPoint = visibilityPolygon.get(p);
            coordinates[p] = new Coordinate(vlPoint.x, vlPoint.y);
        }
        LinearRing shell = GeometryUtils.getGeometryFactory().createLinearRing(coordinates);
        Polygon poly = GeometryUtils.getGeometryFactory().createPolygon(shell, new LinearRing[0]);
        return poly;
    }

    private void createEdgesForRingSegment(Set<Edge> edges, AreaEdgeList edgeList, Area area,
            Ring ring, int i, HashSet<P2<OSMNode>> alreadyAddedEdges) {
        OSMNode node = ring.nodes.get(i);
        OSMNode nextNode = ring.nodes.get((i + 1) % ring.nodes.size());
        P2<OSMNode> nodePair = new P2<OSMNode>(node, nextNode);
        if (alreadyAddedEdges.contains(nodePair)) {
            return;
        }
        alreadyAddedEdges.add(nodePair);
        IntersectionVertex startEndpoint = __handler.getVertexForOsmNode(node, area.parent);
        IntersectionVertex endEndpoint = __handler.getVertexForOsmNode(nextNode, area.parent);

        createSegments(node, nextNode, startEndpoint, endEndpoint, Arrays.asList(area), edgeList,
                edges);
    }

    private void createSegments(OSMNode fromNode, OSMNode toNode, IntersectionVertex startEndpoint,
            IntersectionVertex endEndpoint, Collection<Area> areas, AreaEdgeList edgeList,
            Set<Edge> edges) {

        List<Area> intersects = new ArrayList<Area>();

        Coordinate[] coordinates = new Coordinate[] { startEndpoint.getCoordinate(),
                endEndpoint.getCoordinate() };
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        LineString line = geometryFactory.createLineString(coordinates);
        for (Area area : areas) {
            MultiPolygon polygon = area.toJTSMultiPolygon();
            Geometry intersection = polygon.intersection(line);
            if (intersection.getLength() > 0.000001) {
                intersects.add(area);
            }
        }
        if (intersects.size() == 0) {
            // apparently our intersection here was bogus
            return;
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
            I18NString name = __handler.getNameForWay(areaEntity, label);

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
            edges.add(street);

            label = "way (area) " + areaEntity.getId() + " from " + endEndpoint.getLabel() + " to "
                    + startEndpoint.getLabel();
            name = __handler.getNameForWay(areaEntity, label);

            AreaEdge backStreet = edgeFactory.createAreaEdge(endEndpoint, startEndpoint,
                    (LineString) line.reverse(), name, length, areaPermissions, true, edgeList);
            backStreet.setCarSpeed(carSpeed);

            if (!areaEntity.hasTag("name") && !areaEntity.hasTag("ref")) {
                backStreet.setHasBogusName(true);
            }

            if (areaEntity.isTagFalse("wheelchair")) {
                street.setWheelchairAccessible(false);
            }

            backStreet.setStreetClass(cls);
            edges.add(backStreet);

            WayProperties wayData = wayPropertySet.getDataForWay(areaEntity);
            __handler.applyWayProperties(street, backStreet, wayData, areaEntity);

        } else {
            // take the part that intersects with the start vertex
            Coordinate startCoordinate = startEndpoint.getCoordinate();
            Point startPoint = geometryFactory.createPoint(startCoordinate);
            for (Area area : intersects) {
                MultiPolygon polygon = area.toJTSMultiPolygon();
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
                    createSegments(fromNode, toNode, startEndpoint, newEndpoint,
                            Arrays.asList(area), edgeList, edges);
                    createSegments(fromNode, toNode, newEndpoint, endEndpoint, intersects,
                            edgeList, edges);
                    break;
                }
            }
        }
    }

    private void createNamedAreas(AreaEdgeList edgeList, Ring ring, Collection<Area> areas) {
        Polygon containingArea = ring.toJtsPolygon();
        for (Area area : areas) {
            Geometry intersection = containingArea.intersection(area.toJTSMultiPolygon());
            if (intersection.getArea() == 0) {
                continue;
            }
            NamedArea namedArea = new NamedArea();
            OSMWithTags areaEntity = area.parent;
            int cls = StreetEdge.CLASS_OTHERPATH;
            cls |= OSMFilter.getStreetClasses(areaEntity);
            namedArea.setStreetClass(cls);

            String id = "way (area) " + areaEntity.getId() + " (splitter linking)";
            I18NString name = __handler.getNameForWay(areaEntity, id);
            namedArea.setName(name);

            WayProperties wayData = wayPropertySet.getDataForWay(areaEntity);
            Double safety = wayData.getSafetyFeatures().first;
            namedArea.setBicycleSafetyMultiplier(safety);

            namedArea.setOriginalEdges(intersection);

            StreetTraversalPermission permission = OSMFilter.getPermissionsForEntity(areaEntity,
                    StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
            namedArea.setPermission(permission);

            edgeList.addArea(namedArea);
        }
    }

    private void accumulateRingNodes(Ring ring, List<OSMNode> nodes, List<VLPoint> vertices) {
        for (OSMNode node : ring.nodes) {
            if (nodes.contains(node)) {
                // hopefully, this only happens in order to
                // close polygons
                continue;
            }
            VLPoint point = new VLPoint(node.lon, node.lat);
            nodes.add(node);
            vertices.add(point);
        }
    }

    private void accumulateVisibilityPoints(List<OSMNode> nodes, VLPolygon polygon,
            List<VLPoint> visibilityPoints, List<OSMNode> visibilityNodes, boolean hole) {
        int n = polygon.vertices.size();
        for (int i = 0; i < n; ++i) {
            OSMNode curNode = nodes.get(i);
            VLPoint cur = polygon.vertices.get(i);
            VLPoint prev = polygon.vertices.get((i + n - 1) % n);
            VLPoint next = polygon.vertices.get((i + 1) % n);
            if (hole
                    || (cur.x - prev.x) * (next.y - cur.y) - (cur.y - prev.y) * (next.x - cur.x) > 0) {
                // that math up there is a cross product to check
                // if the point is concave. Note that the sign is reversed because
                // visilibity is either ccw or latitude-major

                if (!visibilityNodes.contains(curNode)) {
                    visibilityPoints.add(cur);
                    visibilityNodes.add(curNode);
                }
            }
        }
    }

    private VLPolygon makeStandardizedVLPolygon(List<VLPoint> vertices, List<OSMNode> nodes,
            boolean reversed) {
        VLPolygon polygon = new VLPolygon(vertices);

        if ((reversed && polygon.area() > 0) || (!reversed && polygon.area() < 0)) {
            polygon.reverse();
            // need to reverse nodes as well
            reversePolygonOfOSMNodes(nodes);
        }

        if (!polygon.is_in_standard_form()) {
            standardize(polygon.vertices, nodes);
        }
        return polygon;
    }

    private void standardize(ArrayList<VLPoint> vertices, List<OSMNode> nodes) {
        // based on code from VisiLibity
        int point_count = vertices.size();
        if (point_count > 1) { // if more than one point in the polygon.
            ArrayList<VLPoint> vertices_temp = new ArrayList<VLPoint>(point_count);
            ArrayList<OSMNode> nodes_temp = new ArrayList<OSMNode>(point_count);
            // Find index of lexicographically smallest point.
            int index_of_smallest = 0;
            for (int i = 1; i < point_count; i++)
                if (vertices.get(i).compareTo(vertices.get(index_of_smallest)) < 0)
                    index_of_smallest = i;
            // minor optimization for already-standardized polygons
            if (index_of_smallest == 0)
                return;
            // Fill vertices_temp starting with lex. smallest.
            for (int i = index_of_smallest; i < point_count; i++) {
                vertices_temp.add(vertices.get(i));
                nodes_temp.add(nodes.get(i));
            }
            for (int i = 0; i < index_of_smallest; i++) {
                vertices_temp.add(vertices.get(i));
                nodes_temp.add(nodes.get(i));
            }
            for (int i = 0; i < point_count; ++i) {
                vertices.set(i, vertices_temp.get(i));
                nodes.set(i, nodes_temp.get(i));
            }
        }
    }

    private void reversePolygonOfOSMNodes(List<OSMNode> nodes) {
        for (int i = 1; i < (nodes.size() + 1) / 2; ++i) {
            OSMNode tmp = nodes.get(i);
            int opposite = nodes.size() - i;
            nodes.set(i, nodes.get(opposite));
            nodes.set(opposite, tmp);
        }
    }
}

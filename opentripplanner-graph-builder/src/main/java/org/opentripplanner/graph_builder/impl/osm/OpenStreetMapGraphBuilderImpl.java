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

package org.opentripplanner.graph_builder.impl.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.gbannotation.ConflictingBikeTags;
import org.opentripplanner.gbannotation.ElevationFlattened;
import org.opentripplanner.gbannotation.Graphwide;
import org.opentripplanner.gbannotation.LevelAmbiguous;
import org.opentripplanner.gbannotation.TurnRestrictionBad;
import org.opentripplanner.gbannotation.TurnRestrictionException;
import org.opentripplanner.gbannotation.TurnRestrictionUnknown;
import org.opentripplanner.graph_builder.impl.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.impl.extra_elevation_data.ExtraElevationData;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMLevel.Source;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.EdgeWithElevation;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorHopEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.TranslatedString;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.util.MapUtils;
import org.opentripplanner.visibility.Environment;
import org.opentripplanner.visibility.Point;
import org.opentripplanner.visibility.Polygon;
import org.opentripplanner.visibility.VisibilityGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Builds a street graph from OpenStreetMap data.
 * 
 */
enum Direction {
    LEFT, RIGHT, U, STRAIGHT;
}
/**
 * A temporary holder for turn restrictions while we have only way/node ids but not yet edge
 * objects
 */
class TurnRestrictionTag {
    long via;

    TurnRestrictionType type;

    Direction direction;

    public List<PlainStreetEdge> possibleFrom = new ArrayList<PlainStreetEdge>();
    public List<PlainStreetEdge> possibleTo = new ArrayList<PlainStreetEdge>();

    public TraverseModeSet modes;

    TurnRestrictionTag(long via, TurnRestrictionType type, Direction direction) {
        this.via = via;
        this.type = type;
        this.direction = direction;
    }
}

public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static Logger _log = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    private Map<Object, Object> _uniques = new HashMap<Object, Object>();

    private WayPropertySet wayPropertySet = new WayPropertySet();

    private CustomNamer customNamer;
    
    private ExtraElevationData extraElevationData = new ExtraElevationData();

    private boolean noZeroLevels = true;

    private boolean staticBikeRental;

    private OSMPlainStreetEdgeFactory edgeFactory = new DefaultOSMPlainStreetEdgeFactory();

    public List<String> provides() {
        return Arrays.asList("streets", "turns");
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }
    
    /**
     * The source for OSM map data
     */
    public void setProvider(OpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    /**
     * Multiple sources for OSM map data
     */
    public void setProviders(List<OpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    /**
     * Allows for alternate PlainStreetEdge implementations; this is intended
     * for users who want to provide more info in PSE than OTP normally keeps
     * around.
     */
    public void setEdgeFactory(OSMPlainStreetEdgeFactory edgeFactory) {
        this.edgeFactory = edgeFactory;
    }

    /**
     * Set the way properties from a {@link WayPropertySetSource} source.
     * 
     * @param source the way properties source
     */
    public void setDefaultWayPropertySetSource(WayPropertySetSource source) {
        wayPropertySet = source.getWayPropertySet();
    }

    /**
     * If true, disallow zero floors and add 1 to non-negative numeric floors, as is generally done
     * in the United States. This does not affect floor names from level maps. Default: true.
     */
    public void setNoZeroLevels(boolean nz) {
        noZeroLevels = nz;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        Handler handler = new Handler(graph);
        for (OpenStreetMapProvider provider : _providers) {
            _log.debug("gathering osm from provider: " + provider);
            provider.readOSM(handler);
        }
        _log.debug("building osm street graph");
        handler.buildGraph(extra);
    }

    @SuppressWarnings("unchecked")
    private <T> T unique(T value) {
        Object v = _uniques.get(value);
        if (v == null) {
            _uniques.put(value, value);
            v = value;
        }
        return (T) v;
    }

    public void setWayPropertySet(WayPropertySet wayDataSet) {
        this.wayPropertySet = wayDataSet;
    }

    public WayPropertySet getWayPropertySet() {
        return wayPropertySet;
    }

    /**
     * Whether bike rental stations should be loaded from OSM, rather
     * than periodically dynamically pulled from APIs.
     */
    public void setStaticBikeRental(boolean b) {
        this.staticBikeRental = b;
    }
    
    public boolean getStaticBikeRental() {
        return staticBikeRental;
    }

    private class Handler implements OpenStreetMapContentHandler {

        private static final double VISIBILITY_EPSILON = 0.000000001;
        private Map<Long, OSMNode> _nodes = new HashMap<Long, OSMNode>();
        private Map<Long, OSMWay> _ways = new HashMap<Long, OSMWay>();
        private List<Area> _areas = new ArrayList<Area>();
        private Set<Long> _areaWayIds = new HashSet<Long>();
        private Map<Long, OSMWay> _areaWaysById = new HashMap<Long, OSMWay>();
        private Map<Long, Set<OSMWay>> _areasForNode = new HashMap<Long, Set<OSMWay>>();
        private List<OSMWay> _singleWayAreas = new ArrayList<OSMWay>();

        private Map<Long, OSMRelation> _relations = new HashMap<Long, OSMRelation>();
        private Set<OSMWithTags> _processedAreas = new HashSet<OSMWithTags>();
        private Set<Long> _nodesWithNeighbors = new HashSet<Long>();
        private Set<Long> _areaNodes = new HashSet<Long>();

        private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByFromWay =
                new HashMap<Long, List<TurnRestrictionTag>>();

        private Map<Long, List<TurnRestrictionTag>> turnRestrictionsByToWay =
                new HashMap<Long, List<TurnRestrictionTag>>();

        class Ring {
            public List<OSMNode> nodes;

            public Polygon geometry;

            public List<Ring> holes = new ArrayList<Ring>();

            public Ring(List<Long> osmNodes) {
                ArrayList<Point> vertices = new ArrayList<Point>();
                nodes = new ArrayList<OSMNode>(osmNodes.size());
                for (long nodeId : osmNodes) {
                    OSMNode node = _nodes.get(nodeId);
                    if (nodes.contains(node)) {
                        // hopefully, this only happens in order to
                        // close polygons
                        continue;
                    }
                    Point point = new Point(node.getLon(), node.getLat());
                    nodes.add(node);
                    vertices.add(point);
                }
                geometry = new Polygon(vertices);
            }
        }

        /**
         * Stores information about an OSM area needed for visibility graph construction. Algorithm
         * based on http://wiki.openstreetmap.org/wiki/Relation:multipolygon/Algorithm but generally
         * done in a quick/dirty way.
         */
        class Area {

            public class AreaConstructionException extends RuntimeException {
                private static final long serialVersionUID = 1L;
            }
            OSMWithTags parent; // this is the way or relation that has the relevant tags for the
                                // area

            List<Ring> outermostRings = new ArrayList<Ring>();

            Area(OSMWithTags parent, List<OSMWay> outerRingWays, List<OSMWay> innerRingWays) {
                this.parent = parent;
                // ring assignment
                List<List<Long>> innerRingNodes = constructRings(innerRingWays);
                List<List<Long>> outerRingNodes = constructRings(outerRingWays);
                if (innerRingNodes == null || outerRingNodes == null) {
                    throw new AreaConstructionException();
                }
                ArrayList<List<Long>> allRings = new ArrayList<List<Long>>(innerRingNodes);
                allRings.addAll(outerRingNodes);

                List<Ring> innerRings = new ArrayList<Ring>();
                List<Ring> outerRings = new ArrayList<Ring>();
                for (List<Long> ring : innerRingNodes) {
                    innerRings.add(new Ring(ring));
                }
                for (List<Long> ring : outerRingNodes) {
                    outerRings.add(new Ring(ring));
                }

                // now, ring grouping
                // first, find outermost rings
                OUTER: for (Ring outer : outerRings) {
                    for (Ring possibleContainer : outerRings) {
                        if (outer != possibleContainer && 
                                outer.geometry.hasPointInside(possibleContainer.geometry)) {
                            continue OUTER;
                        }
                    }
                    outermostRings.add(outer);

                    // find holes in this ring
                    for (Ring possibleHole : innerRings) {
                        if (possibleHole.geometry.hasPointInside(outer.geometry)) {
                            outer.holes.add(possibleHole);
                        }
                    }
                }
            }

            public List<List<Long>> constructRings(List<OSMWay> ways) {
                if (ways.size() == 0) {
                    // no rings is no rings
                    return Collections.emptyList();
                }

                HashMap<Long, List<OSMWay>> waysByEndpoint = new HashMap<Long, List<OSMWay>>();
                for (OSMWay way : ways) {
                    List<Long> refs = way.getNodeRefs();
                    MapUtils.addToMapList(waysByEndpoint, refs.get(0), way);
                    MapUtils.addToMapList(waysByEndpoint, refs.get(refs.size() - 1), way);
                }

                List<List<Long>> closedRings = new ArrayList<List<Long>>();
                // precheck for impossible situations and precompute one-way rings

                List<Long> toRemove = new ArrayList<Long>();
                for (Map.Entry<Long, List<OSMWay>> entry : waysByEndpoint.entrySet()) {
                    Long key = entry.getKey();
                    List<OSMWay> list = entry.getValue();
                    if (list.size() % 2 == 1) {
                        return null;
                    }
                    OSMWay way1 = list.get(0);
                    OSMWay way2 = list.get(1);
                    if (list.size() == 2 && way1 == way2) {
                        ArrayList<Long> ring = new ArrayList<Long>(way1.getNodeRefs());
                        closedRings.add(ring);
                        toRemove.add(key);
                    }
                }
                for (Long key : toRemove) {
                    waysByEndpoint.remove(key);
                }

                List<Long> partialRing = new ArrayList<Long>();
                if (waysByEndpoint.size() == 0) {
                    return closedRings;
                }

                long firstEndpoint = 0, otherEndpoint = 0;
                OSMWay firstWay = null;
                for (Map.Entry<Long, List<OSMWay>> entry : waysByEndpoint.entrySet()) {
                    firstEndpoint = entry.getKey();
                    List<OSMWay> list = entry.getValue();
                    firstWay = list.get(0);
                    List<Long> nodeRefs = firstWay.getNodeRefs();
                    partialRing.addAll(nodeRefs);
                    firstEndpoint = nodeRefs.get(0);
                    otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
                    break;
                }
                waysByEndpoint.get(firstEndpoint).remove(firstWay);
                waysByEndpoint.get(otherEndpoint).remove(firstWay);
                if (constructRingsRecursive(waysByEndpoint, partialRing, closedRings, firstEndpoint)) {
                    return closedRings;
                } else {
                    return null;
                }
            }

            private boolean constructRingsRecursive(HashMap<Long, List<OSMWay>> waysByEndpoint,
                    List<Long> ring, List<List<Long>> closedRings, long endpoint) {

                List<OSMWay> ways = new ArrayList<OSMWay>(waysByEndpoint.get(endpoint));

                for (OSMWay way : ways) {
                    // remove this way from the map
                    List<Long> nodeRefs = way.getNodeRefs();
                    long firstEndpoint = nodeRefs.get(0);
                    long otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
                    MapUtils.removeFromMapList(waysByEndpoint, firstEndpoint, way);
                    MapUtils.removeFromMapList(waysByEndpoint, otherEndpoint, way);

                    ArrayList<Long> newRing = new ArrayList<Long>(ring.size() + nodeRefs.size());
                    long newFirstEndpoint;
                    if (firstEndpoint == endpoint) {
                        for (int j = nodeRefs.size() - 1; j >= 1; --j) {
                            newRing.add(nodeRefs.get(j));
                        }
                        newRing.addAll(ring);
                        newFirstEndpoint = otherEndpoint;
                    } else {
                        newRing.addAll(nodeRefs.subList(0, nodeRefs.size() - 1));
                        newRing.addAll(ring);
                        newFirstEndpoint = firstEndpoint;
                    }
                    if (newRing.get(newRing.size() - 1).equals(newRing.get(0))) {
                        // ring closure
                        closedRings.add(newRing);
                        // if we're out of endpoints, then we have succeeded
                        if (waysByEndpoint.size() == 0) {
                            return true; // success
                        }

                        // otherwise, we need to start a new partial ring
                        newRing = new ArrayList<Long>();
                        OSMWay firstWay = null;
                        for (Map.Entry<Long, List<OSMWay>> entry : waysByEndpoint.entrySet()) {
                            firstEndpoint = entry.getKey();
                            List<OSMWay> list = entry.getValue();
                            firstWay = list.get(0);
                            nodeRefs = firstWay.getNodeRefs();
                            newRing.addAll(nodeRefs);
                            firstEndpoint = nodeRefs.get(0);
                            otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
                            break;
                        }
                        MapUtils.removeFromMapList(waysByEndpoint, firstEndpoint, firstWay);
                        MapUtils.removeFromMapList(waysByEndpoint, otherEndpoint, firstWay);
                        if (constructRingsRecursive(waysByEndpoint, newRing, closedRings,
                                firstEndpoint)) {
                            return true;
                        }
                        MapUtils.addToMapList(waysByEndpoint, firstEndpoint, firstWay);
                        MapUtils.addToMapList(waysByEndpoint, otherEndpoint, firstWay);
                    } else {
                        // continue with this ring
                        if (waysByEndpoint.get(newFirstEndpoint) != null) {
                            if (constructRingsRecursive(waysByEndpoint, newRing, closedRings,
                                    newFirstEndpoint)) {
                                return true;
                            }
                        }
                    }
                    if (firstEndpoint == endpoint) {
                        MapUtils.addToMapList(waysByEndpoint, otherEndpoint, way);
                    } else {
                        MapUtils.addToMapList(waysByEndpoint, firstEndpoint, way);
                    }
                }
                return false;
            }
        }

        private Graph graph;

        /** The bike safety factor of the safest street */
        private double bestBikeSafety = 1;

        // track OSM nodes which are decomposed into multiple graph vertices because they are
        // elevators. later they will be iterated over to build ElevatorEdges between them.
        private HashMap<Long, HashMap<OSMLevel, IntersectionVertex>> multiLevelNodes =
                new HashMap<Long, HashMap<OSMLevel, IntersectionVertex>>();

        // track OSM nodes that will become graph vertices because they appear in multiple OSM ways
        private Map<Long, IntersectionVertex> intersectionNodes =
                new HashMap<Long, IntersectionVertex>();

        // track vertices to be removed in the turn-graph conversion.
        // this is a superset of intersectionNodes.values, which contains
        // a null vertex reference for multilevel nodes. the individual vertices
        // for each level of a multilevel node are includeed in endpoints.
        private ArrayList<IntersectionVertex> endpoints = new ArrayList<IntersectionVertex>();

        // track which vertical level each OSM way belongs to, for building elevators etc.
        private Map<OSMWithTags, OSMLevel> wayLevels = new HashMap<OSMWithTags, OSMLevel>();
        private HashSet<OSMNode> _bikeRentalNodes = new HashSet<OSMNode>();
        private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

        public Handler(Graph graph) {
            this.graph = graph;
        }

        public void buildGraph(HashMap<Class<?>, Object> extra) {

            // handle turn restrictions, road names, and level maps in relations
            processRelations();

            if (staticBikeRental) {
                processBikeRentalNodes();
            }

            // Remove all simple islands
            HashSet<Long> _keep = new HashSet<Long>(_nodesWithNeighbors);
            _keep.addAll(_areaNodes);
            _nodes.keySet().retainAll(_keep);

            // figure out which nodes that are actually intersections
            initIntersectionNodes();

            buildBasicGraph();
            buildAreas();

            buildElevatorEdges(graph);

            /* unify turn restrictions */
            for (List<TurnRestrictionTag> restrictions: turnRestrictionsByFromWay.values()) {
                for (TurnRestrictionTag restrictionTag : restrictions) {
                    if (restrictionTag.possibleFrom.isEmpty()) {
                        _log.warn("No from edge found for restriction " + restrictionTag);
                        continue;
                    }
                    if (restrictionTag.possibleTo.isEmpty()) {
                        _log.warn("No to edge found for restriction " + restrictionTag);
                        continue;
                    }
                    for (PlainStreetEdge from : restrictionTag.possibleFrom) {
                        if (from == null) {
                            _log.warn("from-edge is null in turn restriction " + restrictionTag);
                            continue;
                        }
                        for (PlainStreetEdge to : restrictionTag.possibleTo) {
                            if (from == null || to == null) {
                                continue;
                            }
                            int angleDiff = from.getOutAngle() - to.getInAngle();
                            if (angleDiff < 0) {
                                angleDiff += 360;
                            }
                            switch (restrictionTag.direction) {
                            case LEFT:
                                if (angleDiff >= 160) {
                                    continue; // not a left turn
                                }
                                break;
                            case RIGHT:
                                if (angleDiff <= 200)
                                    continue; // not a right turn
                                break;
                            case U:
                                if ((angleDiff <= 150 || angleDiff > 210))
                                    continue; // not straight
                                break;
                            case STRAIGHT:
                                if (Math.abs(angleDiff) >= 30)
                                    continue; // not a U turn
                                break;
                            }
                            TurnRestriction restriction = new TurnRestriction();
                            restriction.from = from;
                            restriction.to = to;
                            restriction.type = restrictionTag.type;
                            restriction.modes = restrictionTag.modes;
                            from.addTurnRestriction(restriction);
                        }
                    }
                }
            }

            if (customNamer != null) {
                customNamer.postprocess(graph);
            }
            
            //generate elevation profiles
            generateElevationProfiles(graph);

            applyBikeSafetyFactor(graph);
        } // END buildGraph()

        private void processBikeRentalNodes() {
            _log.debug("Processing bike rental nodes...");
            int n = 0;
            for (OSMNode node : _bikeRentalNodes) {
                n++;
                int capacity = Integer.MAX_VALUE;
                if (node.hasTag("capacity")) {
                    try {
                        capacity = Integer.parseInt(node.getTag("capacity"));
                    } catch (NumberFormatException e) {
                        _log.warn("Capacity is not a number: " + node.getTag("capacity"));
                    }
                }
                String network = node.getTag("network");
                if (network == null) {
                     _log.warn("Bike rental station at osm node " + node.getId() + " with no network; not including");
                     continue;
                }
                String creativeName = wayPropertySet.getCreativeNameForWay(node);
                BikeRentalStationVertex station = new BikeRentalStationVertex(graph, "" + node.getId(), "bike rental "
                        + node.getId(), node.getLon(), node.getLat(),
                        creativeName, capacity);

                new RentABikeOnEdge(station, station, network);
                new RentABikeOffEdge(station, station, network);
            }
            _log.debug("Created " + n + " bike rental stations.");
        }

        private void generateElevationProfiles(Graph graph) {
            Map<EdgeWithElevation, List<ElevationPoint>> data = extraElevationData.data;
            for (Map.Entry<EdgeWithElevation, List<ElevationPoint>> entry : data.entrySet()) {
                EdgeWithElevation edge = entry.getKey();
                List<ElevationPoint> points = entry.getValue();
                Collections.sort(points);

                if (points.size() == 1) {
                    ElevationPoint firstPoint = points.get(0);
                    ElevationPoint endPoint = new ElevationPoint(edge.getDistance(), firstPoint.ele);
                    points.add(endPoint);
                }
                Coordinate[] coords = new Coordinate[points.size()];
                int i = 0;
                for (ElevationPoint p : points) {
                    double d = p.distanceAlongShape;
                    if (i == 0) {
                        d = 0;
                    } else if (i == points.size() - 1) {
                        d = edge.getDistance();
                    }
                    coords[i++] = new Coordinate(d, p.ele);
                }
                // set elevation profile and warn if profile was flattened because it was too steep
                if(edge.setElevationProfile(new PackedCoordinateSequence.Double(coords), true)) {
                    _log.warn(graph.addBuilderAnnotation(new ElevationFlattened(edge)));
                }
            }
        }

        private void buildAreas() {
            final int MAX_AREA_NODES = 500;
            _log.debug("building visibility graphs for areas");
            for (Area area : _areas) {
                Set<OSMNode> startingNodes = new HashSet<OSMNode>();
                List<Vertex> startingVertices = new ArrayList<Vertex>();
                Set<Edge> edges = new HashSet<Edge>();

                OSMWithTags areaEntity = area.parent;

                StreetTraversalPermission areaPermissions = getPermissionsForEntity(areaEntity,
                        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
                if (areaPermissions == StreetTraversalPermission.NONE) 
                        continue;
                setWayName(areaEntity);

                List<Point> vertices = new ArrayList<Point>();

                // the points corresponding to concave or hole vertices
                // or those linked to ways
                Set<Point> visibilityPoints = new HashSet<Point>();

                // create polygon and accumulate nodes for area

                for (Ring ring : area.outermostRings) {
                    List<OSMNode> nodes = new ArrayList<OSMNode>();
                    vertices.clear();
                    for (OSMNode node : ring.nodes) {
                        if (nodes.contains(node)) {
                            // hopefully, this only happens in order to
                            // close polygons
                            continue;
                        }
                        if (node == null) {
                            throw new RuntimeException("node for area "
                                    + areaEntity.getId() + " does not exist");
                        }
                        Point point = new Point(node.getLon(), node.getLat());
                        nodes.add(node);
                        vertices.add(point);
                    }
                    Polygon polygon = new Polygon(vertices);

                    if (polygon.area() < 0) {
                        polygon.reverse();
                        // need to reverse nodes as well
                        reversePolygonOfOSMNodes(nodes);
                    }

                    if (!polygon.is_in_standard_form()) {
                        standardize(polygon.vertices, nodes);
                    }

                    int n = polygon.vertices.size();
                    for (int i = 0; i < n; ++i) {
                        Point cur = polygon.vertices.get(i);
                        Point prev = polygon.vertices.get((i + n - 1) % n);
                        Point next = polygon.vertices.get((i + 1) % n);
                        OSMNode curNode = nodes.get(i);
                        if (_nodesWithNeighbors.contains(curNode.getId()) || multipleAreasContain(curNode.getId())) {
                            visibilityPoints.add(cur);
                            startingNodes.add(curNode);
                        } else if ((cur.x - prev.x) * (next.y - cur.y) - (cur.y - prev.y)
                                * (next.x - cur.x) < 0) {
                            //that math up there is a couple of cross products to check
                            //if the point is concave.
                            visibilityPoints.add(cur);
                        }

                    }

                    ArrayList<Polygon> polygons = new ArrayList<Polygon>();
                    polygons.add(polygon);
                    // holes
                    for (Ring innerRing : ring.holes) {
                        ArrayList<OSMNode> holeNodes = new ArrayList<OSMNode>();
                        vertices = new ArrayList<Point>();
                        for (OSMNode node : innerRing.nodes) {
                            if (holeNodes.contains(node)) {
                                // hopefully, this only happens in order to
                                // close polygons
                                continue;
                            }
                            if (node == null) {
                                throw new RuntimeException("node for area does not exist");
                            }
                            Point point = new Point(node.getLon(), node.getLat());
                            holeNodes.add(node);
                            vertices.add(point);
                            visibilityPoints.add(point);
                            if (_nodesWithNeighbors.contains(node.getId()) || multipleAreasContain(node.getId())) {                    
                                startingNodes.add(node);
                            }
                        }
                        Polygon hole = new Polygon(vertices);

                        if (hole.area() > 0) {
                            reversePolygonOfOSMNodes(holeNodes);
                            hole.reverse();
                        }
                        if (!hole.is_in_standard_form()) {
                            standardize(hole.vertices, holeNodes);
                        }
                        nodes.addAll(holeNodes);
                        polygons.add(hole);
                    }

                    Environment areaEnv = new Environment(polygons);

                    //FIXME: temporary hard limit on size of 
                    //areas to prevent way explosion
                    if (visibilityPoints.size() > MAX_AREA_NODES) {
                        _log.warn("Area " + area.parent + " is too complicated (" + visibilityPoints.size() + " > " + MAX_AREA_NODES);
                        continue;
                    }

                    if (!areaEnv.is_valid(VISIBILITY_EPSILON)) {
                        _log.warn("Area " + area.parent + " is not epsilon-valid (epsilon = " + VISIBILITY_EPSILON + ")");
                        continue;
                    }
                    VisibilityGraph vg = new VisibilityGraph(areaEnv, VISIBILITY_EPSILON, visibilityPoints);
                    for (int i = 0; i < nodes.size(); ++i) {
                        OSMNode nodeI = nodes.get(i);
                        for (int j = 0; j < nodes.size(); ++j) {
                            if (i == j)
                                continue;

                            if (vg.get(0, i, 0, j)) {
                                // vertex i is connected to vertex j
                                IntersectionVertex startEndpoint = getVertexForOsmNode(nodeI,
                                        areaEntity);
                                OSMNode nodeJ = nodes.get(j);
                                IntersectionVertex endEndpoint = getVertexForOsmNode(nodeJ, areaEntity);

                                Coordinate[] coordinates = new Coordinate[] {
                                        startEndpoint.getCoordinate(), endEndpoint.getCoordinate() };
                                LineString geometry = GeometryUtils.getGeometryFactory().createLineString(coordinates);

                                String id = "way (area) " + areaEntity.getId() + " from "
                                        + nodeI.getId() + " to " + nodeJ.getId();
                                id = unique(id);
                                String name = getNameForWay(areaEntity, id);

                                double length = distanceLibrary.distance(
                                        startEndpoint.getCoordinate(), endEndpoint.getCoordinate());
                                PlainStreetEdge street = edgeFactory.createEdge(nodeI, nodeJ, areaEntity, startEndpoint,
                                        endEndpoint, geometry, name, length,
                                        areaPermissions,
                                        i > j);
                                street.setId(id);

                                edges.add(street);
                                if (startingNodes.contains(nodeI)) {
                                    startingVertices.add(startEndpoint);
                                }
                            }
                        }
                    }
                }
                pruneAreaEdges(startingVertices, edges);
            }
        }

        private void standardize(ArrayList<Point> vertices, List<OSMNode> nodes) {
            //based on code from VisiLibity
            int point_count = vertices.size();
            if (point_count > 1) { // if more than one point in the polygon.
                ArrayList<Point> vertices_temp = new ArrayList<Point>(point_count);
                ArrayList<OSMNode> nodes_temp = new ArrayList<OSMNode>(point_count);
                // Find index of lexicographically smallest point.
                int index_of_smallest = 0;
                for (int i = 1; i < point_count; i++)
                    if (vertices.get(i).compareTo(vertices.get(index_of_smallest)) < 0)
                        index_of_smallest = i;
                //minor optimization for already-standardized polygons
                if (index_of_smallest == 0) return;
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

        private boolean multipleAreasContain(long id) {
            Set<OSMWay> areas = _areasForNode.get(id);
            if (areas == null) {
                return false;
            }
            return areas.size() > 1;
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
         * Do an all-pairs shortest path search from a list of vertices over a specified
         * set of edges, and retain only those edges which are actually used in some
         * shortest path.
         * @param startingVertices
         * @param edges
         */
        private void pruneAreaEdges(List<Vertex> startingVertices, Set<Edge> edges) {
            if (edges.size() == 0)
                    return;
            TraverseMode mode;
            PlainStreetEdge firstEdge = (PlainStreetEdge) edges.iterator().next();

            if (firstEdge.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
                mode = TraverseMode.WALK;
            } else if (firstEdge.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
                mode = TraverseMode.BICYCLE;
            } else {
                mode = TraverseMode.CAR;
            }
            RoutingRequest options = new RoutingRequest(mode);
            GenericDijkstra search = new GenericDijkstra(options);
            search.setSkipEdgeStrategy(new ListedEdgesOnly(edges));
            Set<Edge> usedEdges = new HashSet<Edge>();
            for (Vertex vertex : startingVertices) {
                State state = new State(vertex, options);
                ShortestPathTree spt = search.getShortestPathTree(state);
                for (Vertex endVertex : startingVertices) {
                    GraphPath path = spt.getPath(endVertex, false);
                    if (path != null){
                        for (Edge edge : path.edges) {
                            usedEdges.add(edge);
                        }
                    }
                }
            }
            for (Edge edge : edges) {
                if (!usedEdges.contains(edge)) {
                    edge.detach();
                }
            }
        }

        private void reversePolygonOfOSMNodes(List<OSMNode> nodes) {
            for (int i = 1; i < (nodes.size()+1) / 2; ++i) {
                OSMNode tmp = nodes.get(i);
                int opposite = nodes.size() - i;
                nodes.set(i, nodes.get(opposite));
                nodes.set(opposite, tmp);
            }
        }

        private void buildBasicGraph() {

            /* build the street segment graph from OSM ways */
            long wayIndex = 0;

           WAY: for (OSMWay way : _ways.values()) {

                if (wayIndex % 10000 == 0)
                    _log.debug("ways=" + wayIndex + "/" + _ways.size());
                wayIndex++;

                WayProperties wayData = wayPropertySet.getDataForWay(way);

                setWayName(way);
                Set<Alert> note = wayPropertySet.getNoteForWay(way);
                Set<Alert> wheelchairNote = getWheelchairNotes(way);

                StreetTraversalPermission permissions = getPermissionsForEntity(way,
                        wayData.getPermission());
                if (permissions == StreetTraversalPermission.NONE)
                    continue;

                //handle duplicate nodes in OSM ways
                //this is a workaround for crappy OSM data quality
                ArrayList<Long> nodes = new ArrayList<Long>(way.getNodeRefs().size());
                long last = -1;
                double lastLat = -1, lastLon = -1;
                String lastLevel = null;
                for (long nodeId : way.getNodeRefs()) {
                    OSMNode node = _nodes.get(nodeId);
                    if (node == null)
                        continue WAY;
                    boolean levelsDiffer = false;
                    String level = node.getTag("level");
                    if (lastLevel == null) {
                        if (level != null) {
                            levelsDiffer = true;
                        }
                    } else {
                        if (!lastLevel.equals(level)) {
                            levelsDiffer = true;
                        }
                    }
                    if (nodeId != last && (node.getLat() != lastLat || node.getLon() != lastLon || levelsDiffer))
                        nodes.add(nodeId);
                    last = nodeId;
                    lastLon = node.getLon();
                    lastLat = node.getLat();
                    lastLevel = level;
                }

                IntersectionVertex startEndpoint = null, endEndpoint = null;

                ArrayList<Coordinate> segmentCoordinates = new ArrayList<Coordinate>();

                getLevelsForWay(way);

                /*
                 * Traverse through all the nodes of this edge. For nodes which are not shared with
                 * any other edge, do not create endpoints -- just accumulate them for geometry and
                 * ele tags. For nodes which are shared, create endpoints and StreetVertex
                 * instances.
                 * One exception: if the next vertex also appears earlier in the way, we need
                 * to split the way, because otherwise we have a way that loops from a vertex to
                 * itself, which could cause issues with splitting.
                 */
                Long startNode = null;
                //where the current edge should start
                OSMNode osmStartNode = null;
                List<ElevationPoint> elevationPoints = new ArrayList<ElevationPoint>();
                double distance = 0;
                for (int i = 0; i < nodes.size() - 1; i++) {
                    OSMNode segmentStartOSMNode = _nodes.get(nodes.get(i));
                    if (segmentStartOSMNode == null) {
                        continue;
                    }
                    Long endNode = nodes.get(i + 1);
                    if (osmStartNode == null) {
                        startNode = nodes.get(i);
                        osmStartNode = segmentStartOSMNode;
                        elevationPoints.clear();
                    }
                    //where the current edge might end
                    OSMNode osmEndNode = _nodes.get(endNode);

                    if (osmStartNode == null || osmEndNode == null)
                        continue;

                    LineString geometry;

                    /*
                     * skip vertices that are not intersections, except that we use them for
                     * geometry
                     */
                    if (segmentCoordinates.size() == 0) {
                        segmentCoordinates.add(getCoordinate(osmStartNode));
                    }
                    String ele = segmentStartOSMNode.getTag("ele");
                    if (ele != null) {
                        Double elevation = ElevationUtils.parseEleTag(ele);
                        if (elevation != null) {
                            elevationPoints.add(
                                    new ElevationPoint(distance, elevation));
                        }
                    }

                    distance += distanceLibrary.distance(
                            segmentStartOSMNode.getLat(), segmentStartOSMNode.getLon(),
                            osmEndNode.getLat(), osmEndNode.getLon());

            if (intersectionNodes.containsKey(endNode)
                    || i == nodes.size() - 2
                    || nodes.subList(0, i).contains(nodes.get(i))) {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        ele = osmEndNode.getTag("ele");
                        if (ele != null) {
                            Double elevation = ElevationUtils.parseEleTag(ele);
                            if (elevation != null) {
                                elevationPoints.add(
                                        new ElevationPoint(distance, elevation));
                            }
                        }

                        geometry = GeometryUtils.getGeometryFactory().createLineString(segmentCoordinates
                                .toArray(new Coordinate[0]));
                        segmentCoordinates.clear();
                    } else {
                        segmentCoordinates.add(getCoordinate(osmEndNode));
                        continue;
                    }

                    /* generate endpoints */
                    if (startEndpoint == null) { // first iteration on this way
                        // make or get a shared vertex for flat intersections,
                        // one vertex per level for multilevel nodes like elevators
                        startEndpoint = getVertexForOsmNode(osmStartNode, way);
                    } else { // subsequent iterations
                        startEndpoint = endEndpoint;
                    }

                    endEndpoint = getVertexForOsmNode(osmEndNode, way);

                    P2<PlainStreetEdge> streets = getEdgesForStreet(startEndpoint, endEndpoint,
                            way, i, osmStartNode.getId(), osmEndNode.getId(), permissions, geometry);

                    PlainStreetEdge street = streets.getFirst();

                    if (street != null) {
                        double safety = wayData.getSafetyFeatures().getFirst();
                        street.setBicycleSafetyEffectiveLength(street.getLength() * safety);
                        if (safety < bestBikeSafety) {
                            bestBikeSafety = safety;
                        }
                        if (note != null) {
                            street.setNote(note);
                        }
                        if (wheelchairNote != null) {
                            street.setWheelchairNote(wheelchairNote);
                        }
                    }

                    PlainStreetEdge backStreet = streets.getSecond();
                    if (backStreet != null) {
                        double safety = wayData.getSafetyFeatures().getSecond();
                        if (safety < bestBikeSafety) {
                            bestBikeSafety = safety;
                        }
                        backStreet.setBicycleSafetyEffectiveLength(backStreet.getLength() * safety);
                        if (note != null) {
                            backStreet.setNote(note);
                        }
                        if (wheelchairNote != null) {
                            backStreet.setWheelchairNote(wheelchairNote);
                        }
                    }

                    storeExtraElevationData(elevationPoints, street, backStreet, distance);

                    applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
                    startNode = endNode;
                    osmStartNode = _nodes.get(startNode);
                }
            } // END loop over OSM ways
        }

        private void setWayName(OSMWithTags way) {
            if (!way.hasTag("name")) {
                String creativeName = wayPropertySet.getCreativeNameForWay(way);
                if (creativeName != null) {
                    way.addTag("otp:gen_name", creativeName);
                }
            }
        }

        private void storeExtraElevationData(List<ElevationPoint> elevationPoints, PlainStreetEdge street, PlainStreetEdge backStreet, double length) {
            if (elevationPoints.isEmpty()) {
                return;
            }

            for (ElevationPoint p : elevationPoints) {
                if (street != null) {
                    MapUtils.addToMapList(extraElevationData.data, street, p);
                }
                if (backStreet != null) {
                    MapUtils.addToMapList(extraElevationData.data, backStreet, p.fromBack(length));
                }
            }
        }

        private void buildElevatorEdges(Graph graph) {
            /* build elevator edges */
            for (Long nodeId : multiLevelNodes.keySet()) {
                OSMNode node = _nodes.get(nodeId);
                // this allows skipping levels, e.g., an elevator that stops
                // at floor 0, 2, 3, and 5.
                // Converting to an Array allows us to
                // subscript it so we can loop over it in twos. Assumedly, it will stay
                // sorted when we convert it to an Array.
                // The objects are Integers, but toArray returns Object[]
                HashMap<OSMLevel, IntersectionVertex> vertices = multiLevelNodes.get(nodeId);

                /*
                 * first, build FreeEdges to disconnect from the graph, GenericVertices to serve as
                 * attachment points, and ElevatorBoard and ElevatorAlight edges to connect future
                 * ElevatorHop edges to. After this iteration, graph will look like (side view):
                 * +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * + GenericVertex, X EndpointVertex, ~~ FreeEdge, ==
                 * ElevatorBoardEdge/ElevatorAlightEdge Another loop will fill in the
                 * ElevatorHopEdges.
                 */

                OSMLevel[] levels = vertices.keySet().toArray(new OSMLevel[0]);
                Arrays.sort(levels);
                ArrayList<Vertex> onboardVertices = new ArrayList<Vertex>();
                for (OSMLevel level : levels) {
                    // get the node to build the elevator out from
                    IntersectionVertex sourceVertex = vertices.get(level);
                    String sourceVertexLabel = sourceVertex.getLabel();
                    String levelName = level.longName;

                    ElevatorOffboardVertex offboardVertex = new ElevatorOffboardVertex(graph,
                            sourceVertexLabel + "_offboard", sourceVertex.getX(),
                            sourceVertex.getY(), levelName);

                    new FreeEdge(sourceVertex, offboardVertex);
                    new FreeEdge(offboardVertex, sourceVertex);

                    ElevatorOnboardVertex onboardVertex = new ElevatorOnboardVertex(graph,
                            sourceVertexLabel + "_onboard", sourceVertex.getX(),
                            sourceVertex.getY(), levelName);

                    new ElevatorBoardEdge(offboardVertex, onboardVertex);
                    new ElevatorAlightEdge(onboardVertex, offboardVertex, level.longName);

                    // accumulate onboard vertices to so they can be connected by hop edges later
                    onboardVertices.add(onboardVertex);
                }

                // -1 because we loop over onboardVertices two at a time
                for (Integer i = 0, vSize = onboardVertices.size() - 1; i < vSize; i++) {
                    Vertex from = onboardVertices.get(i);
                    Vertex to = onboardVertices.get(i + 1);

                    // default permissions: pedestrian, wheelchair, and bicycle
                    boolean wheelchairAccessible = true;
                    StreetTraversalPermission permission = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
                    // check for bicycle=no, otherwise assume it's OK to take a bike
                    if (node.isTagFalse("bicycle")) {
                        permission = StreetTraversalPermission.PEDESTRIAN;
                    }
                    // check for wheelchair=no
                    if (node.isTagFalse("wheelchair")) {
                        wheelchairAccessible = false;
                    }

                    // The narrative won't be strictly correct, as it will show the elevator as part
                    // of the cycling leg, but I think most cyclists will figure out that they
                    // should really dismount.
                    ElevatorHopEdge foreEdge = new ElevatorHopEdge(from, to, permission);
                    ElevatorHopEdge backEdge = new ElevatorHopEdge(to, from, permission);
                    foreEdge.wheelchairAccessible = wheelchairAccessible;
                    backEdge.wheelchairAccessible = wheelchairAccessible;
                }
            } // END elevator edge loop
        }

        private void applyEdgesToTurnRestrictions(OSMWay way, long startNode, long endNode,
                PlainStreetEdge street, PlainStreetEdge backStreet) {
            /* Check if there are turn restrictions starting on this segment */
            List<TurnRestrictionTag> restrictionTags = turnRestrictionsByFromWay.get(way.getId());

            if (restrictionTags != null) {
                for (TurnRestrictionTag tag : restrictionTags) {
                    if (tag.via == startNode) {
                        tag.possibleFrom.add(backStreet);
                    } else if (tag.via == endNode) {
                        tag.possibleFrom.add(street);
                    }
                }
            }

            restrictionTags = turnRestrictionsByToWay.get(way.getId());
            if (restrictionTags != null) {
                for (TurnRestrictionTag tag : restrictionTags) {
                    if (tag.via == startNode) {
                        tag.possibleTo.add(street);
                    } else if (tag.via == endNode) {
                        tag.possibleTo.add(backStreet);
                    }
                }
            }
        }

        private void getLevelsForWay(OSMWithTags way) {
            /* Determine OSM level for each way, if it was not already set */
            if (!wayLevels.containsKey(way)) {
                // if this way is not a key in the wayLevels map, a level map was not
                // already applied in processRelations

                /* try to find a level name in tags */
                String levelName = null;
                OSMLevel level = OSMLevel.DEFAULT;
                if (way.hasTag("level")) { // TODO: floating-point levels &c.
                    levelName = way.getTag("level");
                    level = OSMLevel.fromString(levelName, OSMLevel.Source.LEVEL_TAG, noZeroLevels);
                } else if (way.hasTag("layer")) {
                    levelName = way.getTag("layer");
                    level = OSMLevel.fromString(levelName, OSMLevel.Source.LAYER_TAG, noZeroLevels);
                } 
                if (level == null || ( ! level.reliable)) {
                    _log.warn(graph.addBuilderAnnotation(new LevelAmbiguous(levelName, way.getId())));
                    level = OSMLevel.DEFAULT;
                }
                wayLevels.put(way, level);
            }
        }

        private void initIntersectionNodes() {
            Set<Long> possibleIntersectionNodes = new HashSet<Long>();
            for (OSMWay way : _ways.values()) {
                List<Long> nodes = way.getNodeRefs();
                for (long node : nodes) {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.put(node, null);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                }
            }
        }

        private Set<Alert> getWheelchairNotes(OSMWithTags way) {
            Map<String, String> tags = way.getTagsByPrefix("wheelchair:description");
            if (tags == null) {
                return null;
            }
            Set<Alert> alerts = new HashSet<Alert>();
            Alert alert = new Alert();
            alerts.add(alert);
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (k.equals("wheelchair:description")) {
                    // no language, assume default from TranslatedString
                    alert.alertHeaderText = new TranslatedString(v);
                } else {
                    String lang = k.substring("wheelchair:description:".length());
                    alert.alertHeaderText = new TranslatedString(lang, v);
                }
            }
            return alerts;
        }

        /**
         * The safest bike lane should have a safety weight no lower than the time weight of a flat
         * street. This method divides the safety lengths by the length ratio of the safest street,
         * ensuring this property.
         * 
         * @param graph
         */
        private void applyBikeSafetyFactor(Graph graph) {
            _log.info(graph.addBuilderAnnotation(new Graphwide ("Multiplying all bike safety values by " + (1 / bestBikeSafety))));
            HashSet<Edge> seenEdges = new HashSet<Edge>();
            for (Vertex vertex : graph.getVertices()) {
                for (Edge e : vertex.getOutgoing()) {
                    if (!(e instanceof PlainStreetEdge)) {
                        continue;
                    }
                    PlainStreetEdge pse = (PlainStreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyEffectiveLength(pse.getBicycleSafetyEffectiveLength()
                                / bestBikeSafety);
                    }
                }
                for (Edge e : vertex.getIncoming()) {
                    if (!(e instanceof PlainStreetEdge)) {
                        continue;
                    }
                    PlainStreetEdge pse = (PlainStreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyEffectiveLength(pse.getBicycleSafetyEffectiveLength()
                                / bestBikeSafety);
                    }
                }
            }
        }

        private Coordinate getCoordinate(OSMNode osmNode) {
            return new Coordinate(osmNode.getLon(), osmNode.getLat());
        }

        public void addNode(OSMNode node) {
            if(node.isTag("amenity", "bicycle_rental")) {
                _bikeRentalNodes.add(node);
                return;
            }
            if (!(_nodesWithNeighbors.contains(node.getId()) || _areaNodes.contains(node.getId())))
                return;

            if (_nodes.containsKey(node.getId()))
                return;

            _nodes.put(node.getId(), node);

            if (_nodes.size() % 100000 == 0)
                _log.debug("nodes=" + _nodes.size());
        }

        public void addWay(OSMWay way) {
            /* only add ways once */
            long wayId = way.getId();
            if (_ways.containsKey(wayId) || _areaWaysById.containsKey(wayId))
                return;

            if (_areaWayIds.contains(wayId)) {
                _areaWaysById.put(wayId, way);
            }
            
            /* filter out ways that are not relevant for routing */
            if (!isWayRouteable(way)) {
                return;
            }
            if (way.isTag("area", "yes") && way.getNodeRefs().size() > 2) {
                //this is an area that's a simple polygon.  So we can just add it straight
                //to the areas, if it's not part of a relation.
                if (!_areaWayIds.contains(wayId)) {
                    _singleWayAreas.add(way);
                    _areaWaysById.put(wayId, way);
                    _areaWayIds.add(wayId);
                    for (Long node : way.getNodeRefs()) {
                        MapUtils.addToMapSet(_areasForNode, node, way);
                    }
                    getLevelsForWay(way);
                }
                return;
            }

            _ways.put(wayId, way);

            if (_ways.size() % 10000 == 0)
                _log.debug("ways=" + _ways.size());
        }

        private boolean isWayRouteable(OSMWithTags way) {
            if (!(way.hasTag("highway") || way.isTag("railway", "platform")))
                return false;
            String highway = way.getTag("highway");
            if (highway != null
                    && (highway.equals("conveyer") || highway.equals("proposed") || highway
                            .equals("raceway")))
                return false;

            String access = way.getTag("access");

            if (access != null) {
                if ("no".equals(access) || "license".equals(access)) {
                    if (way.doesTagAllowAccess("motorcar")) {
                        return true;
                    }
                    if (way.doesTagAllowAccess("bicycle")) {
                        return true;
                    }
                    if (way.doesTagAllowAccess("foot")) {
                        return true;
                    }
                    return false;
                }
            }
            return true;
        }

        public void addRelation(OSMRelation relation) {
            if (_relations.containsKey(relation.getId()))
                return;

            if (relation.isTag("type", "multipolygon") && relation.hasTag("highway")) {
                // OSM MultiPolygons are ferociously complicated, and in fact cannot be processed
                // without reference to the ways that compose them.  Accordingly, we will merely
                // mark the ways for preservation here, and deal with the details once we have
                // the ways loaded.
                if (!isWayRouteable(relation)) {
                    return;
                }
                for (OSMRelationMember member : relation.getMembers()) {
                    _areaWayIds.add(member.getRef());
                }
                getLevelsForWay(relation);
            } else if (!(relation.isTag("type", "restriction"))
                    && !(relation.isTag("type", "route") && relation.isTag("route", "road"))
                    && !(relation.isTag("type", "multipolygon") && relation.hasTag("highway"))
                    && !(relation.isTag("type", "level_map"))) {
                return;
            }

            _relations.put(relation.getId(), relation);

            if (_relations.size() % 100 == 0)
                _log.debug("relations=" + _relations.size());

        }

        public void secondPhase() {
            // This copies relevant tags to the ways (highway=*) where it doesn't exist, so that
            // the way purging keeps the needed way around.
            // Multipolygons may be processed more than once, which may be needed since
            // some member might be in different files for the same multipolygon.
            
            // NOTE (AMB): this purging phase may not be necessary if highway tags are not
            // copied over from multipolygon relations. Perhaps we can get by with 
            // only 2 steps -- ways+relations, followed by used nodes.
            // Ways can be tag-filtered in phase 1. 
            
            markNodesForKeeping(_ways.values(), _nodesWithNeighbors);
            markNodesForKeeping(_areaWaysById.values(), _areaNodes);
        }

        /**
         * After all relations, ways, and nodes are loaded, handle areas.
         */
        public void nodesLoaded() {
            processMultipolygons();
            AREA: for (OSMWay way : _singleWayAreas) {
                if (_processedAreas.contains(way)) {
                    continue;
                }
                for (Long nodeRef : way.getNodeRefs()) {
                    if (! _nodes.containsKey(nodeRef)) {
                        continue AREA;
                    }
                }
                try {
                    _areas.add(new Area(way, Arrays.asList(way), Collections.<OSMWay> emptyList()));
                } catch (Area.AreaConstructionException e) {
                    // this area cannot be constructed, but we already have all the
                    //necessary nodes to construct it. So, something must be wrong with
                    // the area; we'll mark it as processed so that we don't retry.
                }
                _processedAreas.add(way);
            }
            
        }

        private void markNodesForKeeping(Collection<OSMWay> osmWays, Set<Long> nodeSet) {
            for (Iterator<OSMWay> it = osmWays.iterator(); it.hasNext();) {
                OSMWay way = it.next();
                // Since the way is kept, update nodes-with-neighbors
                List<Long> nodes = way.getNodeRefs();
                if (nodes.size() > 1) {
                    nodeSet.addAll(nodes);
                }
            }
        }

        /**
         * Copies useful metadata from multipolygon relations to the relevant ways, or to the area
         * map
         * 
         * This is done at a different time than processRelations(), so that way purging doesn't
         * remove the used ways.
         */
        private void processMultipolygons() {
            RELATION: for (OSMRelation relation : _relations.values()) {
                if (_processedAreas.contains(relation)) {
                    continue;
                }
                if (!(relation.isTag("type", "multipolygon") && relation.hasTag("highway"))) {
                    continue;
                }
                // Area multipolygons -- pedestrian plazas
                ArrayList<OSMWay> innerWays = new ArrayList<OSMWay>();
                ArrayList<OSMWay> outerWays = new ArrayList<OSMWay>();
                for (OSMRelationMember member : relation.getMembers()) {
                    String role = member.getRole();
                    OSMWay way = _areaWaysById.get(member.getRef());
                    if (way == null) {
                        // relation includes way which does not exist in the data. Skip.
                        continue RELATION;
                    }
                    for (Long nodeId : way.getNodeRefs()) {
                        if (!_nodes.containsKey(nodeId)) {
                            // this area is missing some nodes, perhaps because it is on
                            // the edge of the region, so we will simply not route on it.
                            continue RELATION;
                        }
                        MapUtils.addToMapSet(_areasForNode, nodeId, way);
                    }
                    if (role.equals("inner")) {
                        innerWays.add(way);
                    } else if (role.equals("outer")) {
                        outerWays.add(way);
                    } else {
                        _log.warn("Unexpected role " + role + " in multipolygon");
                    }
                }
                _processedAreas.add(relation);
                Area area;
                try {
                    area = new Area(relation, outerWays, innerWays);
                } catch (Area.AreaConstructionException e) {
                    continue;
                }
                _areas.add(area);

                for (OSMRelationMember member : relation.getMembers()) {
                    //multipolygons for attribute mapping
                    if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                        continue;
                    }

                    OSMWithTags way = _ways.get(member.getRef());
                    if (way == null) {
                        continue;
                    }

                    if (relation.hasTag("highway") && !way.hasTag("highway")) {
                        way.addTag("highway", relation.getTag("highway"));
                    }
                    if (relation.hasTag("name") && !way.hasTag("name")) {
                        way.addTag("name", relation.getTag("name"));
                    }
                    if (relation.hasTag("ref") && !way.hasTag("ref")) {
                        way.addTag("ref", relation.getTag("ref"));
                    }
                }
            }
        }

        /**
         * Copies useful metadata from relations to the relevant ways/nodes.
         */
        private void processRelations() {
            _log.debug("Processing relations...");

            for (OSMRelation relation : _relations.values()) {
                if (relation.isTag("type", "restriction")) {
                    processRestriction(relation);
                } else if (relation.isTag("type", "level_map")) {
                    processLevelMap(relation);
                } else if (relation.isTag("type", "route")) {
                    processRoad(relation);
                }

                // multipolygons will be further processed in secondPhase()
            }
        }

        /**
         * Store turn restrictions for use in StreetUtils.makeEdgeBased.
         * 
         * @param relation
         */
        private void processRestriction(OSMRelation relation) {
            long from = -1, to = -1, via = -1;
            for (OSMRelationMember member : relation.getMembers()) {
                String role = member.getRole();
                if (role.equals("from")) {
                    from = member.getRef();
                } else if (role.equals("to")) {
                    to = member.getRef();
                } else if (role.equals("via")) {
                    via = member.getRef();
                }
            }
            if (from == -1 || to == -1 || via == -1) {
                _log.warn(graph.addBuilderAnnotation(new TurnRestrictionBad(relation.getId())));
                return;
            }

            Set<TraverseMode> modes = EnumSet.of(TraverseMode.BICYCLE, TraverseMode.CAR);
            String exceptModes = relation.getTag("except");
            if (exceptModes != null) {
                for (String m : exceptModes.split(";")) {
                    if (m.equals("motorcar")) {
                        modes.remove(TraverseMode.CAR);
                    } else if (m.equals("bicycle")) {
                        modes.remove(TraverseMode.BICYCLE);
                        _log.warn(graph.addBuilderAnnotation(new TurnRestrictionException(via, from)));
                    }
                }
            }

            TurnRestrictionTag tag;
            if (relation.isTag("restriction", "no_right_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.RIGHT);
            } else if (relation.isTag("restriction", "no_left_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.LEFT);
            } else if (relation.isTag("restriction", "no_straight_on")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.STRAIGHT);
            } else if (relation.isTag("restriction", "no_u_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.NO_TURN, Direction.U);
            } else if (relation.isTag("restriction", "only_straight_on")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.STRAIGHT);
            } else if (relation.isTag("restriction", "only_right_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.RIGHT);
            } else if (relation.isTag("restriction", "only_left_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.LEFT);
            } else if (relation.isTag("restriction", "only_u_turn")) {
                tag = new TurnRestrictionTag(via, TurnRestrictionType.ONLY_TURN, Direction.U);
            } else {
                _log.warn(graph.addBuilderAnnotation(new TurnRestrictionUnknown(relation.getTag("restriction"))));
                return;
            }
            tag.modes = new TraverseModeSet(modes);

            MapUtils.addToMapList(turnRestrictionsByFromWay, from, tag);
            MapUtils.addToMapList(turnRestrictionsByToWay, to, tag);

        }

        /**
         * Process an OSM level map.
         * 
         * @param relation
         */
        private void processLevelMap(OSMRelation relation) {
            Map<String, OSMLevel> levels = OSMLevel.mapFromSpecList(relation.getTag("levels"), Source.LEVEL_MAP, true);
            for (OSMRelationMember member : relation.getMembers()) {
                if ("way".equals(member.getType()) && _ways.containsKey(member.getRef())) {
                    OSMWay way = _ways.get(member.getRef());
                    if (way != null) {
                        String role = member.getRole();
                        // if the level map relation has a role:xyz tag, this way is something
                        // more complicated than a single level (e.g. ramp/stairway).
                        if (!relation.hasTag("role:" + role)) {
                            if (levels.containsKey(role)) {
                                wayLevels.put(way, levels.get(role));
                            } else {
                                _log.warn(member.getRef() + " has undefined level " + role);
                            }
                        }
                    }
                }
            }
        }

        /*
         * Handle route=road relations.
         * 
         * @param relation
         */
        private void processRoad(OSMRelation relation) {
            for (OSMRelationMember member : relation.getMembers()) {
                if (!("way".equals(member.getType()) && _ways.containsKey(member.getRef()))) {
                    continue;
                }

                OSMWithTags way = _ways.get(member.getRef());
                if (way == null) {
                    continue;
                }

                if (relation.hasTag("name")) {
                    if (way.hasTag("otp:route_name")) {
                        way.addTag(
                                "otp:route_name",
                                addUniqueName(way.getTag("otp:route_name"), relation.getTag("name")));
                    } else {
                        way.addTag(new OSMTag("otp:route_name", relation.getTag("name")));
                    }
                }
                if (relation.hasTag("ref")) {
                    if (way.hasTag("otp:route_ref")) {
                        way.addTag("otp:route_ref",
                                addUniqueName(way.getTag("otp:route_ref"), relation.getTag("ref")));
                    } else {
                        way.addTag(new OSMTag("otp:route_ref", relation.getTag("ref")));
                    }
                }
            }
        }

        private String addUniqueName(String routes, String name) {
            String[] names = routes.split(", ");
            for (String existing : names) {
                if (existing.equals(name)) {
                    return routes;
                }
            }
            return routes + ", " + name;
        }

        /**
         * Handle oneway streets, cycleways, and other per-mode and universal access controls. See
         * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
         * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         * 
         * @param end
         * @param start
         */
        private P2<PlainStreetEdge> getEdgesForStreet(IntersectionVertex start,
                IntersectionVertex end, OSMWithTags way, int index, long startNode, long endNode,
                StreetTraversalPermission permissions, LineString geometry) {
            // get geometry length in meters, irritatingly.
            Coordinate[] coordinates = geometry.getCoordinates();
            double d = 0;
            for (int i = 1; i < coordinates.length; ++i) {
                d += distanceLibrary .distance(coordinates[i - 1], coordinates[i]);
            }

            LineString backGeometry = (LineString) geometry.reverse();

            Map<String, String> tags = way.getTags();

            if (permissions == StreetTraversalPermission.NONE)
                return new P2<PlainStreetEdge>(null, null);

            PlainStreetEdge street = null, backStreet = null;

            /*
             * pedestrian rules: everything is two-way (assuming pedestrians are allowed at all)
             * bicycle rules: default: permissions;
             * 
             * cycleway=dismount means walk your bike -- the engine will automatically try walking
             * bikes any time it is forbidden to ride them, so the only thing to do here is to
             * remove bike permissions
             * 
             * oneway=... sets permissions for cars and bikes oneway:bicycle overwrites these
             * permissions for bikes only
             * 
             * now, cycleway=opposite_lane, opposite, opposite_track can allow once oneway has been
             * set by oneway:bicycle, but should give a warning if it conflicts with oneway:bicycle
             * 
             * bicycle:backward=yes works like oneway:bicycle=no bicycle:backwards=no works like
             * oneway:bicycle=yes
             */

            String foot = way.getTag("foot");
            if ("yes".equals(foot) || "designated".equals(foot)) {
                permissions = permissions.add(StreetTraversalPermission.PEDESTRIAN);
            }

            if (OSMWithTags.isFalse(foot)) {
                permissions = permissions.remove(StreetTraversalPermission.PEDESTRIAN);
            }

            boolean forceBikes = false;
            String bicycle = way.getTag("bicycle");
            if ("yes".equals(bicycle) || "designated".equals(bicycle)) {
                permissions = permissions.add(StreetTraversalPermission.BICYCLE);
                forceBikes = true;
            }

            if (way.isTag("cycleway", "dismount") || "dismount".equals(bicycle)) {
                permissions = permissions.remove(StreetTraversalPermission.BICYCLE);
                if (forceBikes) {
                    _log.warn(graph.addBuilderAnnotation(new ConflictingBikeTags(way.getId())));
                }
            }
            P2<StreetTraversalPermission> permissionPair = getPermissions(permissions, way);
            StreetTraversalPermission permissionsFront = permissionPair.getFirst();
            StreetTraversalPermission permissionsBack = permissionPair.getSecond();

            String access = way.getTag("access");
            boolean noThruTraffic = "destination".equals(access) || "private".equals(access)
                    || "customers".equals(access) || "delivery".equals(access)
                    || "forestry".equals(access) || "agricultural".equals(access);

            if (permissionsFront != StreetTraversalPermission.NONE) {
                street = getEdgeForStreet(start, end, way, index, startNode, endNode, d, permissionsFront,
                        geometry, false);
                street.setNoThruTraffic(noThruTraffic);
            }
            if (permissionsBack != StreetTraversalPermission.NONE) {
                backStreet = getEdgeForStreet(end, start, way, index, endNode, startNode, d, permissionsBack,
                        backGeometry, true);
                backStreet.setNoThruTraffic(noThruTraffic);
            }

            /* mark edges that are on roundabouts */
            if ("roundabout".equals(tags.get("junction"))) {
                if (street != null)
                    street.setRoundabout(true);
                if (backStreet != null)
                    backStreet.setRoundabout(true);
            }

            return new P2<PlainStreetEdge>(street, backStreet);
        }

        /**
         * Check OSM tags for various one-way and one-way-by-mode tags and return a pair
         * of permissions for travel along and against the way. 
         */
        private P2<StreetTraversalPermission> getPermissions(StreetTraversalPermission permissions, OSMWithTags way) {

            StreetTraversalPermission permissionsFront = permissions;
            StreetTraversalPermission permissionsBack = permissions;

            if (way.isTagTrue("oneway") || "roundabout".equals(way.getTag("junction"))) {
                permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE_AND_CAR);
            }
            if (way.isTag("oneway", "-1")) {
                permissionsFront = permissionsFront
                        .remove(StreetTraversalPermission.BICYCLE_AND_CAR);
            }
            String oneWayBicycle = way.getTag("oneway:bicycle");
            if (OSMWithTags.isTrue(oneWayBicycle) || way.isTagFalse("bicycle:backwards")) {
                permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
            }
            if ("-1".equals(oneWayBicycle)) {
                permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
            }
            if (OSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
                if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
                    permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
                    permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
                }
            }

            // any cycleway which is opposite* allows contraflow biking
            String cycleway = way.getTag("cycleway");
            String cyclewayLeft = way.getTag("cycleway:left");
            String cyclewayRight = way.getTag("cycleway:right");
            if ((cycleway != null && cycleway.startsWith("opposite"))
                    || (cyclewayLeft != null && cyclewayLeft.startsWith("opposite"))
                    || (cyclewayRight != null && cyclewayRight.startsWith("opposite"))) {

                permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
            }
            return new P2<StreetTraversalPermission>(permissionsFront, permissionsBack);
        }

        private PlainStreetEdge getEdgeForStreet(IntersectionVertex start, IntersectionVertex end,
                OSMWithTags way, int index, long startNode, long endNode, double length,
                StreetTraversalPermission permissions, LineString geometry, boolean back) {

            String id = "way " + way.getId() + " from " + index;
            id = unique(id);

            String name = getNameForWay(way, id);

            boolean steps = "steps".equals(way.getTag("highway"));
            if (steps) {
                // consider the elevation gain of stairs, roughly
                length *= 2;
            }

            PlainStreetEdge street = edgeFactory
                    .createEdge(_nodes.get(startNode), _nodes.get(endNode), way, start, end,
                            geometry, name, length, permissions, back);
            street.setId(id);

            String highway = way.getTag("highway");
            int cls;
            if ("crossing".equals(highway) && !way.isTag("bicycle", "designated")) {
                cls = StreetEdge.CLASS_CROSSING;
            } else if ("footway".equals(highway) && way.isTag("footway", "crossing") && !way.isTag("bicycle", "designated")) {
                cls = StreetEdge.CLASS_CROSSING;
            } else if ("residential".equals(highway) || "tertiary".equals(highway)
                    || "secondary".equals(highway) || "secondary_link".equals(highway)
                    || "primary".equals(highway) || "primary_link".equals(highway)
                    || "trunk".equals(highway) || "trunk_link".equals(highway)) {
                cls = StreetEdge.CLASS_STREET;
            } else {
                cls = StreetEdge.CLASS_OTHERPATH;
            }

            if ("platform".equals(highway) || "platform".equals(way.getTag("railway")) || "platform".equals(way.getTag("public_transport"))) {
                cls |= StreetEdge.CLASS_PLATFORM;
            }
            street.setStreetClass(cls);

            if (!way.hasTag("name")) {
                street.setBogusName(true);
            }
            street.setStairs(steps);

            /* TODO: This should probably generalized somehow? */
            if (way.isTagFalse("wheelchair") || (steps && !way.isTagTrue("wheelchair"))) {
                street.setWheelchairAccessible(false);
            }

            street.setSlopeOverride(wayPropertySet.getSlopeOverride(way));

            if (customNamer != null) {
                customNamer.nameWithEdge(way, street);
            }

            return street;
        }

        private String getNameForWay(OSMWithTags way, String id) {
            String name = way.getAssumedName();

            if (customNamer != null) {
                name = customNamer.name(way, name);
            }

            if (name == null) {
                name = id;
            }
            return name;
        }

        private StreetTraversalPermission getPermissionsForEntity(OSMWithTags entity,
                StreetTraversalPermission def) {
            Map<String, String> tags = entity.getTags();
            StreetTraversalPermission permission = null;

            String highway = tags.get("highway");
            String cycleway = tags.get("cycleway");
            String access = tags.get("access");
            String motorcar = tags.get("motorcar");
            String bicycle = tags.get("bicycle");
            String foot = tags.get("foot");

            /*
             * Only a few tags are examined here, because we only care about modes supported by OTP
             * (wheelchairs are not of concern here)
             * 
             * Only a few values are checked for, all other values are presumed to be permissive (=>
             * This may not be perfect, but is closer to reality, since most people don't follow the
             * rules perfectly ;-)
             */
            if (access != null) {
                if ("no".equals(access) || "license".equals(access)) {
                    // this can actually be overridden
                    permission = StreetTraversalPermission.NONE;
                    if (entity.doesTagAllowAccess("motorcar")) {
                        permission = permission.add(StreetTraversalPermission.CAR);
                    }
                    if (entity.doesTagAllowAccess("bicycle")) {
                        permission = permission.add(StreetTraversalPermission.BICYCLE);
                    }
                    if (entity.doesTagAllowAccess("foot")) {
                        permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                    }
                } else {
                    permission = def;
                }
            } else if (motorcar != null || bicycle != null || foot != null) {
                permission = def;
            }

            if (motorcar != null) {
                if ("no".equals(motorcar) || "license".equals(motorcar)) {
                    permission = permission.remove(StreetTraversalPermission.CAR);
                } else {
                    permission = permission.add(StreetTraversalPermission.CAR);
                }
            }

            if (bicycle != null) {
                if ("no".equals(bicycle) || "license".equals(bicycle)) {
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                } else {
                    permission = permission.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (foot != null) {
                if ("no".equals(foot) || "license".equals(foot)) {
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                } else {
                    permission = permission.add(StreetTraversalPermission.PEDESTRIAN);
                }
            }

            if (highway != null) {
                if ("construction".equals(highway)) {
                    permission = StreetTraversalPermission.NONE;
                }
            } else {
                if ("construction".equals(cycleway)) {
                    permission = StreetTraversalPermission.NONE;
                }
            }

            if (permission == null)
                return def;

            return permission;
        }

        /**
         * Is this a multi-level node that should be decomposed to multiple coincident nodes?
         * Currently returns true only for elevators.
         * 
         * @param node
         * @return whether the node is multi-level
         * @author mattwigway
         */
        private boolean isMultiLevelNode(OSMNode node) {
            return node.hasTag("highway") && "elevator".equals(node.getTag("highway"));
        }

        /**
         * Record the level of the way for this node, e.g. if the way is at level 5, mark that this
         * node is active at level 5.
         * 
         * @param the way that has the level
         * @param the node to record for
         * @author mattwigway
         */
        private IntersectionVertex recordLevel(OSMNode node, OSMWithTags way) {
            OSMLevel level = wayLevels.get(way);
            HashMap<OSMLevel, IntersectionVertex> vertices;
            long nodeId = node.getId();
            if (multiLevelNodes.containsKey(nodeId)) {
                vertices = multiLevelNodes.get(nodeId);
            } else {
                vertices = new HashMap<OSMLevel, IntersectionVertex>();
                multiLevelNodes.put(nodeId, vertices);
            }
            if (!vertices.containsKey(level)) {
                Coordinate coordinate = getCoordinate(node);
                String label = "osm node " + nodeId + " at level " + level.shortName;
                IntersectionVertex vertex = new IntersectionVertex(graph, label, coordinate.x,
                        coordinate.y, label);
                vertices.put(level, vertex);
                // multilevel nodes should also undergo turn-conversion
                endpoints.add(vertex);
                return vertex;
            }
            return vertices.get(level);
        }

        /**
         * Make or get a shared vertex for flat intersections, or one vertex per level for
         * multilevel nodes like elevators. When there is an elevator or other Z-dimension
         * discontinuity, a single node can appear in several ways at different levels.
         * 
         * @param node The node to fetch a label for.
         * @param way The way it is connected to (for fetching level information).
         * @return vertex The graph vertex.
         */
        private IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
            // If the node should be decomposed to multiple levels,
            // use the numeric level because it is unique, the human level may not be (although
            // it will likely lead to some head-scratching if it is not).
            IntersectionVertex iv = null;
            if (isMultiLevelNode(node)) {
                // make a separate node for every level
                return recordLevel(node, way);
            }
            // single-level case
            long nid = node.getId();
            iv = intersectionNodes.get(nid);
            if (iv == null) {
                Coordinate coordinate = getCoordinate(node);
                String label = "osm node " + nid;
                iv = new IntersectionVertex(graph, label, coordinate.x, coordinate.y, label);
                intersectionNodes.put(nid, iv);
                endpoints.add(iv);
            }
            return iv;
        }

        @Override
        public void doneRelations() {
            //nothing to do here
        }
    }

    public CustomNamer getCustomNamer() {
        return customNamer;
    }

    public void setCustomNamer(CustomNamer customNamer) {
        this.customNamer = customNamer;
    }

    @Override
    public void checkInputs() {
        for (OpenStreetMapProvider provider : _providers) {
            provider.checkInputs();
        }
    }
}

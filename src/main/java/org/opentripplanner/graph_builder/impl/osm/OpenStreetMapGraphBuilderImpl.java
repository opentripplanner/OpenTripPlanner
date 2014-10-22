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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.Envelope2D;
import org.opentripplanner.common.DisjointSet;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.graph_builder.annotation.Graphwide;
import org.opentripplanner.graph_builder.annotation.ParkAndRideUnlinked;
import org.opentripplanner.graph_builder.annotation.StreetCarSpeedZero;
import org.opentripplanner.graph_builder.impl.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorHopEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.NamedArea;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.edgetype.RentABikeOffEdge;
import org.opentripplanner.routing.edgetype.RentABikeOnEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.ExitVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;
import org.opentripplanner.visibility.Environment;
import org.opentripplanner.visibility.VLPoint;
import org.opentripplanner.visibility.VLPolygon;
import org.opentripplanner.visibility.VisibilityPolygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OpenStreetMapGraphBuilderImpl implements GraphBuilder {

    private static Logger LOG = LoggerFactory.getLogger(OpenStreetMapGraphBuilderImpl.class);

    // Private members that are only read or written internally.

    private Set<Object> _uniques = new HashSet<Object>();

    private HashMap<Vertex, Double> elevationData = new HashMap<Vertex, Double>();

    public boolean skipVisibility = false;

    // Members that can be set by clients.

    /**
     * WayPropertySet computes edge properties from OSM way data.
     */
    public WayPropertySet wayPropertySet = new WayPropertySet();

    /**
     * Providers of OSM data.
     */
    private List<OpenStreetMapProvider> _providers = new ArrayList<OpenStreetMapProvider>();

    /**
     * Allows for arbitrary custom naming of edges.
     */
    public CustomNamer customNamer;
    
    /**
     * Ignore wheelchair accessibility information.
     */
    public boolean ignoreWheelchairAccessibility = false;

    /**
     * Allows for alternate PlainStreetEdge implementations; this is intended for users who want to provide more info in PSE than OTP normally keeps
     * around.
     */
    public StreetEdgeFactory edgeFactory = new DefaultStreetEdgeFactory();

    /**
     * Whether bike rental stations should be loaded from OSM, rather than periodically dynamically pulled from APIs.
     */
    public boolean staticBikeRental = false;
    
    /**
     * Whether we should create P+R stations from OSM data. 
     */
    public boolean staticParkAndRide = true;

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
     * Set the way properties from a {@link WayPropertySetSource} source.
     *
     * @param source the way properties source
     */
    public void setDefaultWayPropertySetSource(WayPropertySetSource source) {
        wayPropertySet = source.getWayPropertySet();
    }

    /**
     * Construct and set providers all at once.
     */
    public OpenStreetMapGraphBuilderImpl(List<OpenStreetMapProvider> providers) {
        this.setProviders(providers);
    }

    public OpenStreetMapGraphBuilderImpl() {
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        OSMDatabase osmdb = new OSMDatabase();
        Handler handler = new Handler(graph, osmdb);
        for (OpenStreetMapProvider provider : _providers) {
            LOG.info("Gathering OSM from provider: " + provider);
            provider.readOSM(osmdb);
        }
        osmdb.postLoad();
        for (GraphBuilderAnnotation annotation : osmdb.getAnnotations()) {
            graph.addBuilderAnnotation(annotation);
        }
        LOG.info("Building street graph from OSM");
        handler.buildGraph(extra);
    }

    private <T> T unique(T value) {
        if (!_uniques.contains(value)) {
            _uniques.add(value);
        }
        return (T) value;
    }

    private class Handler {

        private static final double VISIBILITY_EPSILON = 0.000000001;

        private static final String nodeLabelFormat = "osm:node:%d";

        private static final String levelnodeLabelFormat = nodeLabelFormat + ":level:%s";

        private Graph graph;

        private OSMDatabase osmdb;

        /**
         * The bike safety factor of the safest street
         */
        private float bestBikeSafety = 1.0f;

        // track OSM nodes which are decomposed into multiple graph vertices because they are
        // elevators. later they will be iterated over to build ElevatorEdges between them.
        private HashMap<Long, HashMap<OSMLevel, IntersectionVertex>> multiLevelNodes = new HashMap<Long, HashMap<OSMLevel, IntersectionVertex>>();

        // track OSM nodes that will become graph vertices because they appear in multiple OSM ways
        private Map<Long, IntersectionVertex> intersectionNodes = new HashMap<Long, IntersectionVertex>();

        // track vertices to be removed in the turn-graph conversion.
        // this is a superset of intersectionNodes.values, which contains
        // a null vertex reference for multilevel nodes. the individual vertices
        // for each level of a multilevel node are includeed in endpoints.
        private ArrayList<IntersectionVertex> endpoints = new ArrayList<IntersectionVertex>();

        private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

        private HashMap<Coordinate, IntersectionVertex> areaBoundaryVertexForCoordinate = new HashMap<Coordinate, IntersectionVertex>();

        public Handler(Graph graph, OSMDatabase osmdb) {
            this.graph = graph;
            this.osmdb = osmdb;
        }

        public void buildGraph(HashMap<Class<?>, Object> extra) {

            if (staticBikeRental) {
                processBikeRentalNodes();
            }

            // figure out which nodes that are actually intersections
            initIntersectionNodes();

            buildBasicGraph();
            if (skipVisibility) {
                LOG.info("Skipping visibility graph construction for walkable areas.");
            } else {
                buildWalkableAreas();
            }

            if (staticParkAndRide) {
                buildParkAndRideAreas();
            }

            buildElevatorEdges(graph);

            unifyTurnRestrictions();

            if (customNamer != null) {
                customNamer.postprocess(graph);
            }

            // generate elevation profiles
            extra.put(ElevationPoint.class, elevationData);

            applyBikeSafetyFactor(graph);
        } // END buildGraph()

        private void processBikeRentalNodes() {
            LOG.info("Processing bike rental nodes...");
            int n = 0;
            BikeRentalStationService bikeRentalService = new BikeRentalStationService();
            graph.putService(BikeRentalStationService.class, bikeRentalService);
            for (OSMNode node : osmdb.getBikeRentalNodes()) {
                n++;
                String creativeName = wayPropertySet.getCreativeNameForWay(node);
                int capacity = Integer.MAX_VALUE;
                if (node.hasTag("capacity")) {
                    try {
                        capacity = node.getCapacity();
                    } catch (NumberFormatException e) {
                        LOG.warn("Capacity for osm node " + node.getId() + " (" + creativeName
                                + ") is not a number: " + node.getTag("capacity"));
                    }
                }
                String networks = node.getTag("network");
                String operators = node.getTag("operator");
                Set<String> networkSet = new HashSet<String>();
                if (networks != null)
                    networkSet.addAll(Arrays.asList(networks.split(";")));
                if (operators != null)
                    networkSet.addAll(Arrays.asList(operators.split(";")));
                if (networkSet.isEmpty()) {
                    LOG.warn("Bike rental station at osm node " + node.getId() + " ("
                            + creativeName + ") with no network; including as compatible-with-all.");
                    networkSet.add("*"); // Special "catch-all" value
                }
                BikeRentalStation station = new BikeRentalStation();
                station.id = "" + node.getId();
                station.name = creativeName;
                station.x = node.lon;
                station.y = node.lat;
                // The following make sure that spaces+bikes=capacity, always.
                // Also, for the degenerate case of capacity=1, we should have 1
                // bike available, not 0.
                station.spacesAvailable = capacity / 2;
                station.bikesAvailable = capacity - station.spacesAvailable;
                station.realTimeData = false;
                bikeRentalService.addStation(station);
                BikeRentalStationVertex stationVertex = new BikeRentalStationVertex(graph, station);
                new RentABikeOnEdge(stationVertex, stationVertex, networkSet);
                new RentABikeOffEdge(stationVertex, stationVertex, networkSet);
            }
            LOG.info("Created " + n + " bike rental stations.");
        }

        final int MAX_AREA_NODES = 500;

        private void buildWalkableAreas() {
            LOG.info("Building visibility graphs for walkable areas.");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
            for (AreaGroup group : areaGroups) {
                buildWalkableAreasForGroup(group);
            }
            LOG.info("Done building visibility graphs for walkable areas.");
        }

        /**
         * Theoretically, it is not correct to build the visibility graph on the joined polygon of areas with different levels of bike safety. That's
         * because in the optimal path, you might end up changing direction at area boundaries. The problem is known as "weighted planar
         * subdivisions", and the best known algorithm is O(N^3). That's not much worse than general visibility graph construction, but it would have
         * to be done at runtime to account for the differences in bike safety preferences. Ted Chiang's "Story Of Your Life" describes how a very
         * similar problem in optics gives rise to Snell's Law. It is the second-best story about a law of physics that I know of (Chiang's
         * "Exhalation" is the first).
         * <p/>
         * Anyway, since we're not going to run an O(N^3) algorithm at runtime just to give people who don't understand Snell's Law weird paths that
         * they can complain about, this should be just fine.
         *
         * @param group
         */
        private void buildWalkableAreasForGroup(AreaGroup group) {
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
                    if (nodes != null){
                        for (OSMNode node : nodes){
                            addtoVisibilityAndStartSets(startingNodes, visibilityPoints, visibilityNodes, node);
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
                    LOG.warn("Area " + group.getSomeOSMObject()
                            + " is not epsilon-valid (epsilon = " + VISIBILITY_EPSILON + ")");
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

                        IntersectionVertex startEndpoint = getVertexForOsmNode(nodeI, areaEntity);
                        IntersectionVertex endEndpoint = getVertexForOsmNode(nodeJ, areaEntity);

                        Coordinate[] coordinates = new Coordinate[]{
                                startEndpoint.getCoordinate(), endEndpoint.getCoordinate()};
                        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
                        LineString line = geometryFactory.createLineString(coordinates);
                        if (poly.contains(line)) {

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
        
        private void buildParkAndRideAreas() {
            LOG.info("Building P+R areas");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
            int n = 0;
            for (AreaGroup group : areaGroups) {
                if (buildParkAndRideAreasForGroup(group))
                    n++;
            }
            LOG.info("Created {} P+R.", n);
        }

        private boolean buildParkAndRideAreasForGroup(AreaGroup group) {
            Envelope2D envelope = null;
            // Process all nodes from outer rings
            List<IntersectionVertex> accessVertexes = new ArrayList<IntersectionVertex>();
            String creativeName = null;
            long osmId = 0L;
            for (Area area : group.areas) {
                osmId = area.parent.getId();
                if (creativeName == null || area.parent.getTag("name") != null)
                    creativeName = wayPropertySet.getCreativeNameForWay(area.parent);
                for (Ring ring : area.outermostRings) {
                    for (OSMNode node : ring.nodes) {
                        // We need to lazy create the envelope as the default
                        // constructor include (0,0) in the bounds...
                        if (envelope == null)
                            envelope = new Envelope2D(null, node.lon, node.lat, 0, 0);
                        else
                            envelope.add(node.lon, node.lat);
                        IntersectionVertex accessVertex = getVertexForOsmNode(node, area.parent);
                        if (accessVertex.getIncoming().isEmpty()
                                || accessVertex.getOutgoing().isEmpty())
                            continue;
                        accessVertexes.add(accessVertex);
                    }
                }
            }
            // Check P+R accessibility by walking and driving.
            TraversalRequirements walkReq = new TraversalRequirements(new RoutingRequest(
                    TraverseMode.WALK));
            TraversalRequirements driveReq = new TraversalRequirements(new RoutingRequest(
                    TraverseMode.CAR));
            boolean walkAccessibleIn = false;
            boolean carAccessibleIn = false;
            boolean walkAccessibleOut = false;
            boolean carAccessibleOut = false;
            for (IntersectionVertex accessVertex : accessVertexes) {
                for (Edge incoming : accessVertex.getIncoming()) {
                    if (incoming instanceof StreetEdge) {
                        if (walkReq.canBeTraversed((StreetEdge)incoming))
                            walkAccessibleIn = true;
                        if (driveReq.canBeTraversed((StreetEdge)incoming))
                            carAccessibleIn = true;
                    }
                }
                for (Edge outgoing : accessVertex.getOutgoing()) {
                    if (outgoing instanceof StreetEdge) {
                        if (walkReq.canBeTraversed((StreetEdge)outgoing))
                            walkAccessibleOut = true;
                        if (driveReq.canBeTraversed((StreetEdge)outgoing))
                            carAccessibleOut = true;
                    }
                }
            }
            if (walkAccessibleIn != walkAccessibleOut) {
                LOG.error("P+R walk IN/OUT accessibility mismatch! Please have a look as this should not happen.");
            }
            if (!walkAccessibleOut || !carAccessibleIn) {
                // This will prevent the P+R to be useful.
                LOG.warn(graph.addBuilderAnnotation(new ParkAndRideUnlinked(creativeName, osmId)));
                return false;
            }
            if (!walkAccessibleIn || !carAccessibleOut) {
                LOG.warn("P+R '{}' ({}) is not walk-accessible");
                // This does not prevent routing as we only use P+R for car dropoff,
                // but this is an issue with OSM data.
            }
            // Place the P+R at the center of the envelope
            ParkAndRideVertex parkAndRideVertex = new ParkAndRideVertex(graph, "P+R" + osmId, "P+R_"
                    + osmId, envelope.getCenterX(), envelope.getCenterY(), creativeName);
            new ParkAndRideEdge(parkAndRideVertex);
            for (IntersectionVertex accessVertex : accessVertexes) {
                new ParkAndRideLinkEdge(parkAndRideVertex, accessVertex);
                new ParkAndRideLinkEdge(accessVertex, parkAndRideVertex);
            }
            LOG.debug("Created P+R '{}' ({})", creativeName, osmId);
            return true;
        }

        private void addtoVisibilityAndStartSets(Set<OSMNode> startingNodes,
                                                 ArrayList<VLPoint> visibilityPoints, ArrayList<OSMNode> visibilityNodes,
                                                 OSMNode node) {
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
            // incomprehensibly, visilibity's routines for figuring out point-polygon containment are too broken
            // to use here, so we have to fall back to JTS.
            Coordinate[] coordinates = new Coordinate[visibilityPolygon.n() + 1];

            for (int p = 0; p < coordinates.length; ++p) {
                VLPoint vlPoint = visibilityPolygon.get(p);
                coordinates[p] = new Coordinate(vlPoint.x, vlPoint.y);
            }
            LinearRing shell = GeometryUtils.getGeometryFactory().createLinearRing(coordinates);
            Polygon poly = GeometryUtils.getGeometryFactory().createPolygon(shell,
                    new LinearRing[0]);
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
            IntersectionVertex startEndpoint = getVertexForOsmNode(node, area.parent);
            IntersectionVertex endEndpoint = getVertexForOsmNode(nextNode, area.parent);

            createSegments(node, nextNode, startEndpoint, endEndpoint, Arrays.asList(area),
                    edgeList, edges);
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
                cls |= getStreetClasses(areaEntity);
                namedArea.setStreetClass(cls);

                String id = "way (area) " + areaEntity.getId() + " (splitter linking)";
                id = unique(id);
                String name = getNameForWay(areaEntity, id);
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

        private void createSegments(OSMNode fromNode, OSMNode toNode,
                                    IntersectionVertex startEndpoint, IntersectionVertex endEndpoint,
                                    Collection<Area> areas, AreaEdgeList edgeList, Set<Edge> edges) {

            List<Area> intersects = new ArrayList<Area>();

            Coordinate[] coordinates = new Coordinate[]{startEndpoint.getCoordinate(),
                    endEndpoint.getCoordinate()};
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

                StreetTraversalPermission areaPermissions = OSMFilter.getPermissionsForEntity(areaEntity,
                        StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

                float carSpeed = wayPropertySet.getCarSpeedForWay(areaEntity, false);

                double length = distanceLibrary.distance(startEndpoint.getCoordinate(),
                        endEndpoint.getCoordinate());

                int cls = StreetEdge.CLASS_OTHERPATH;
                cls |= getStreetClasses(areaEntity);

                String label = "way (area) " + areaEntity.getId() + " from "
                        + startEndpoint.getLabel() + " to " + endEndpoint.getLabel();
                label = unique(label);
                String name = getNameForWay(areaEntity, label);

                AreaEdge street = edgeFactory.createAreaEdge(startEndpoint, endEndpoint, line,
                        name, length, areaPermissions, false, edgeList);
                street.setCarSpeed(carSpeed);

                street.setStreetClass(cls);
                edges.add(street);

                label = "way (area) " + areaEntity.getId() + " from " + endEndpoint.getLabel()
                        + " to " + startEndpoint.getLabel();
                label = unique(label);
                name = getNameForWay(areaEntity, label);

                AreaEdge backStreet = edgeFactory.createAreaEdge(endEndpoint, startEndpoint,
                        (LineString) line.reverse(), name, length, areaPermissions, true, edgeList);
                backStreet.setCarSpeed(carSpeed);

                backStreet.setStreetClass(cls);
                edges.add(backStreet);

                WayProperties wayData = wayPropertySet.getDataForWay(areaEntity);
                applyWayProperties(street, backStreet, wayData, areaEntity);

            } else {
                // take the part that intersects with the start vertex
                Coordinate startCoordinate = startEndpoint.getCoordinate();
                Point startPoint = geometryFactory.createPoint(startCoordinate);
                for (Area area : intersects) {
                    MultiPolygon polygon = area.toJTSMultiPolygon();
                    if (!(polygon.intersects(startPoint) || polygon.getBoundary().intersects(
                            startPoint)))
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
                        || (cur.x - prev.x) * (next.y - cur.y) - (cur.y - prev.y)
                        * (next.x - cur.x) > 0) {
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

        private List<AreaGroup> groupAreas(Collection<Area> areas) {
            DisjointSet<Area> groups = new DisjointSet<Area>();
            Multimap<OSMNode, Area> areasForNode = LinkedListMultimap.create();
            for (Area area : areas) {
                for (Ring ring : area.outermostRings) {
                    for (Ring inner : ring.holes) {
                        for (OSMNode node : inner.nodes) {
                            areasForNode.put(node, area);
                        }
                    }
                    for (OSMNode node : ring.nodes) {
                        areasForNode.put(node, area);
                    }
                }
            }

            // areas that can be joined must share nodes and levels
            for (OSMNode osmNode : areasForNode.keySet()) {
                for (Area area1 : areasForNode.get(osmNode)) {
                    OSMLevel level1 = osmdb.getLevelForWay(area1.parent);
                    for (Area area2 : areasForNode.get(osmNode)) {
                        OSMLevel level2 = osmdb.getLevelForWay(area2.parent);
                        if ((level1 == null && level2 == null)
                                || (level1 != null && level1.equals(level2))) {
                            groups.union(area1, area2);
                        }
                    }
                }
            }

            List<AreaGroup> out = new ArrayList<AreaGroup>();
            for (Set<Area> areaSet : groups.sets()) {
                try {
                    out.add(new AreaGroup(areaSet));
                } catch (AreaGroup.RingConstructionException e) {
                    for (Area area : areaSet) {
                        LOG.debug("Failed to create merged area for "
                                + area
                                + ".  This area might not be at fault; it might be one of the other areas in this list.");
                        out.add(new AreaGroup(Arrays.asList(area)));
                    }
                }
            }
            return out;
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
         * Do an all-pairs shortest path search from a list of vertices over a specified set of edges, and retain only those edges which are actually
         * used in some shortest path.
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
                    graph.streetNotesService.removeStaticNotes(edge);
                    edge.detach();
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

        private void buildBasicGraph() {

            /* build the street segment graph from OSM ways */
            long wayIndex = 0;
            long wayCount = osmdb.getWays().size();

            WAY:
            for (OSMWay way : osmdb.getWays()) {

                if (wayIndex % 10000 == 0)
                    LOG.debug("ways=" + wayIndex + "/" + wayCount);
                wayIndex++;

                WayProperties wayData = wayPropertySet.getDataForWay(way);

                setWayName(way);

                StreetTraversalPermission permissions = OSMFilter.getPermissionsForWay(way,
                        wayData.getPermission());
                if (!OSMFilter.isWayRoutable(way) || permissions.allowsNothing())
                    continue;

                // handle duplicate nodes in OSM ways
                // this is a workaround for crappy OSM data quality
                ArrayList<Long> nodes = new ArrayList<Long>(way.getNodeRefs().size());
                long last = -1;
                double lastLat = -1, lastLon = -1;
                String lastLevel = null;
                for (long nodeId : way.getNodeRefs()) {
                    OSMNode node = osmdb.getNode(nodeId);
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
                    if (nodeId != last
                            && (node.lat != lastLat || node.lon != lastLon || levelsDiffer))
                        nodes.add(nodeId);
                    last = nodeId;
                    lastLon = node.lon;
                    lastLat = node.lat;
                    lastLevel = level;
                }

                IntersectionVertex startEndpoint = null, endEndpoint = null;

                ArrayList<Coordinate> segmentCoordinates = new ArrayList<Coordinate>();

                /*
                 * Traverse through all the nodes of this edge. For nodes which are not shared with any other edge, do not create endpoints -- just
                 * accumulate them for geometry and ele tags. For nodes which are shared, create endpoints and StreetVertex instances. One exception:
                 * if the next vertex also appears earlier in the way, we need to split the way, because otherwise we have a way that loops from a
                 * vertex to itself, which could cause issues with splitting.
                 */
                Long startNode = null;
                // where the current edge should start
                OSMNode osmStartNode = null;

                for (int i = 0; i < nodes.size() - 1; i++) {
                    OSMNode segmentStartOSMNode = osmdb.getNode(nodes.get(i));
                    if (segmentStartOSMNode == null) {
                        continue;
                    }
                    Long endNode = nodes.get(i + 1);
                    if (osmStartNode == null) {
                        startNode = nodes.get(i);
                        osmStartNode = segmentStartOSMNode;
                    }
                    // where the current edge might end
                    OSMNode osmEndNode = osmdb.getNode(endNode);

                    if (osmStartNode == null || osmEndNode == null)
                        continue;

                    LineString geometry;

                    /*
                     * We split segments at intersections, self-intersections, nodes with ele tags, and transit stops;
                     * the only processing we do on other nodes is to accumulate their geometry
                     */
                    if (segmentCoordinates.size() == 0) {
                        segmentCoordinates.add(getCoordinate(osmStartNode));
                    }

                    if (intersectionNodes.containsKey(endNode) || i == nodes.size() - 2
                            || nodes.subList(0, i).contains(nodes.get(i))
                            || osmEndNode.hasTag("ele")
                            || osmEndNode.isStop()) {
                        segmentCoordinates.add(getCoordinate(osmEndNode));

                        geometry = GeometryUtils.getGeometryFactory().createLineString(
                                segmentCoordinates.toArray(new Coordinate[0]));
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
                        String ele = segmentStartOSMNode.getTag("ele");
                        if (ele != null) {
                            Double elevation = ElevationUtils.parseEleTag(ele);
                            if (elevation != null) {
                                elevationData.put(startEndpoint, elevation);
                            }
                        }
                    } else { // subsequent iterations
                        startEndpoint = endEndpoint;
                    }

                    endEndpoint = getVertexForOsmNode(osmEndNode, way);
                    String ele = osmEndNode.getTag("ele");
                    if (ele != null) {
                        Double elevation = ElevationUtils.parseEleTag(ele);
                        if (elevation != null) {
                            elevationData.put(endEndpoint, elevation);
                        }
                    }
                    P2<StreetEdge> streets = getEdgesForStreet(startEndpoint, endEndpoint,
                            way, i, osmStartNode.getId(), osmEndNode.getId(), permissions, geometry);

                    StreetEdge street = streets.first;
                    StreetEdge backStreet = streets.second;
                    applyWayProperties(street, backStreet, wayData, way);

                    applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
                    startNode = endNode;
                    osmStartNode = osmdb.getNode(startNode);
                }
            } // END loop over OSM ways
        }

        private void applyWayProperties(StreetEdge street, StreetEdge backStreet,
                                        WayProperties wayData, OSMWithTags way) {

            Set<T2<Alert, NoteMatcher>> notes = wayPropertySet.getNoteForWay(way);
            boolean noThruTraffic = way.isThroughTrafficExplicitlyDisallowed();

            if (street != null) {
                double safety = wayData.getSafetyFeatures().first;
                street.setBicycleSafetyFactor((float)safety);
                if (safety < bestBikeSafety) {
                    bestBikeSafety = (float)safety;
                }
                if (notes != null) {
                    for (T2<Alert, NoteMatcher> note : notes)
                        graph.streetNotesService.addStaticNote(street, note.first, note.second);
                }
                street.setNoThruTraffic(noThruTraffic);
            }

            if (backStreet != null) {
                double safety = wayData.getSafetyFeatures().second;
                if (safety < bestBikeSafety) {
                    bestBikeSafety = (float)safety;
                }
                backStreet.setBicycleSafetyFactor((float)safety);
                if (notes != null) {
                    for (T2<Alert, NoteMatcher> note : notes)
                        graph.streetNotesService.addStaticNote(backStreet, note.first, note.second);
                }
                backStreet.setNoThruTraffic(noThruTraffic);
            }
        }

        private void setWayName(OSMWithTags way) {
            if (!way.hasTag("name")) {
                String creativeName = wayPropertySet.getCreativeNameForWay(way);
                if (creativeName != null) {
                    way.addTag("otp:gen_name", creativeName);
                }
            }
        }

        private void buildElevatorEdges(Graph graph) {
            /* build elevator edges */
            for (Long nodeId : multiLevelNodes.keySet()) {
                OSMNode node = osmdb.getNode(nodeId);
                // this allows skipping levels, e.g., an elevator that stops
                // at floor 0, 2, 3, and 5.
                // Converting to an Array allows us to
                // subscript it so we can loop over it in twos. Assumedly, it will stay
                // sorted when we convert it to an Array.
                // The objects are Integers, but toArray returns Object[]
                HashMap<OSMLevel, IntersectionVertex> vertices = multiLevelNodes.get(nodeId);

                /*
                 * first, build FreeEdges to disconnect from the graph, GenericVertices to serve as attachment points, and ElevatorBoard and
                 * ElevatorAlight edges to connect future ElevatorHop edges to. After this iteration, graph will look like (side view): +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * +==+~~X
                 * 
                 * + GenericVertex, X EndpointVertex, ~~ FreeEdge, == ElevatorBoardEdge/ElevatorAlightEdge Another loop will fill in the
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

        private void unifyTurnRestrictions() {
            // Note that usually when the from or to way is not found, it's because OTP has already
            // filtered that way.
            // So many of these are not really problems worth issuing warnings on.
            for (Long fromWay : osmdb.getTurnRestrictionWayIds()) {
                for (TurnRestrictionTag restrictionTag : osmdb.getFromWayTurnRestrictions(fromWay)) {
                    if (restrictionTag.possibleFrom.isEmpty()) {
                        LOG.warn("No from edge found for " + restrictionTag);
                        continue;
                    }
                    if (restrictionTag.possibleTo.isEmpty()) {
                        LOG.warn("No to edge found for " + restrictionTag);
                        continue;
                    }
                    for (StreetEdge from : restrictionTag.possibleFrom) {
                        if (from == null) {
                            LOG.warn("from-edge is null in turn " + restrictionTag);
                            continue;
                        }
                        for (StreetEdge to : restrictionTag.possibleTo) {
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
                                    continue; // not a U turn
                                break;
                            case STRAIGHT:
                                if (angleDiff >= 30 && angleDiff < 330)
                                    continue; // not straight
                                break;
                            }
                            TurnRestriction restriction = new TurnRestriction();
                            restriction.from = from;
                            restriction.to = to;
                            restriction.type = restrictionTag.type;
                            restriction.modes = restrictionTag.modes;
                            restriction.time = restrictionTag.time;
                            from.addTurnRestriction(restriction);
                        }
                    }
                }
            }
        }

        private void applyEdgesToTurnRestrictions(OSMWay way, long startNode, long endNode,
                                                  StreetEdge street, StreetEdge backStreet) {
            /* Check if there are turn restrictions starting on this segment */
            Collection<TurnRestrictionTag> restrictionTags = osmdb.getFromWayTurnRestrictions(way.getId());

            if (restrictionTags != null) {
                for (TurnRestrictionTag tag : restrictionTags) {
                    if (tag.via == startNode) {
                        tag.possibleFrom.add(backStreet);
                    } else if (tag.via == endNode) {
                        tag.possibleFrom.add(street);
                    }
                }
            }

            restrictionTags = osmdb.getToWayTurnRestrictions(way.getId());
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

        private void initIntersectionNodes() {
            Set<Long> possibleIntersectionNodes = new HashSet<Long>();
            for (OSMWay way : osmdb.getWays()) {
                List<Long> nodes = way.getNodeRefs();
                for (long node : nodes) {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.put(node, null);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                }
            }
            // Intersect ways at area boundaries if needed.
            for (Area area : Iterables.concat(osmdb.getWalkableAreas(), osmdb.getParkAndRideAreas())) {
                for (Ring outerRing : area.outermostRings) {
                    for (OSMNode node : outerRing.nodes) {
                        long nodeId = node.getId();
                        if (possibleIntersectionNodes.contains(nodeId)) {
                            intersectionNodes.put(nodeId, null);
                        } else {
                            possibleIntersectionNodes.add(nodeId);
                        }
                    }
                }
            }
        }

        /**
         * The safest bike lane should have a safety weight no lower than the time weight of a flat street. This method divides the safety lengths by
         * the length ratio of the safest street, ensuring this property.
         * 
         * TODO Move this away, this is common to all street builders.
         *
         * @param graph
         */
        private void applyBikeSafetyFactor(Graph graph) {
            LOG.info(graph.addBuilderAnnotation(new Graphwide(
                    "Multiplying all bike safety values by " + (1 / bestBikeSafety))));
            HashSet<Edge> seenEdges = new HashSet<Edge>();
            HashSet<AreaEdgeList> seenAreas = new HashSet<AreaEdgeList>();
            for (Vertex vertex : graph.getVertices()) {
                for (Edge e : vertex.getOutgoing()) {
                    if (e instanceof AreaEdge) {
                        AreaEdgeList areaEdgeList = ((AreaEdge) e).getArea();
                        if (seenAreas.contains(areaEdgeList))
                            continue;
                        seenAreas.add(areaEdgeList);
                        for (NamedArea area : areaEdgeList.getAreas()) {
                            area.setBicycleSafetyMultiplier(area.getBicycleSafetyMultiplier()
                                    / bestBikeSafety);
                        }
                    }
                    if (!(e instanceof StreetEdge)) {
                        continue;
                    }
                    StreetEdge pse = (StreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyFactor(pse.getBicycleSafetyFactor() / bestBikeSafety);
                    }
                }
                for (Edge e : vertex.getIncoming()) {
                    if (!(e instanceof StreetEdge)) {
                        continue;
                    }
                    StreetEdge pse = (StreetEdge) e;

                    if (!seenEdges.contains(e)) {
                        seenEdges.add(e);
                        pse.setBicycleSafetyFactor(pse.getBicycleSafetyFactor() / bestBikeSafety);
                    }
                }
            }
        }

        private Coordinate getCoordinate(OSMNode osmNode) {
            return new Coordinate(osmNode.lon, osmNode.lat);
        }

        private String getNodeLabel(OSMNode node) {
            return String.format(nodeLabelFormat, node.getId());
        }

        private String getLevelNodeLabel(OSMNode node, OSMLevel level) {
            return String.format(levelnodeLabelFormat, node.getId(), level.shortName);
        }

        /**
         * Returns the length of the geometry in meters.
         *
         * @param geometry
         * @return
         */
        private double getGeometryLengthMeters(Geometry geometry) {
            Coordinate[] coordinates = geometry.getCoordinates();
            double d = 0;
            for (int i = 1; i < coordinates.length; ++i) {
                d += distanceLibrary.distance(coordinates[i - 1], coordinates[i]);
            }
            return d;
        }

        /**
         * Handle oneway streets, cycleways, and other per-mode and universal access controls. See http://wiki.openstreetmap.org/wiki/Bicycle for
         * various scenarios, along with http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         *
         * @param end
         * @param start
         */
        private P2<StreetEdge> getEdgesForStreet(IntersectionVertex start,
                                                      IntersectionVertex end, OSMWay way, int index, long startNode, long endNode,
                                                      StreetTraversalPermission permissions, LineString geometry) {
            // No point in returning edges that can't be traversed by anyone.
            if (permissions.allowsNothing()) {
                return new P2<StreetEdge>(null, null);
            }

            LineString backGeometry = (LineString) geometry.reverse();
            StreetEdge street = null, backStreet = null;
            double length = this.getGeometryLengthMeters(geometry);

            P2<StreetTraversalPermission> permissionPair = getPermissions(permissions, way);
            StreetTraversalPermission permissionsFront = permissionPair.first;
            StreetTraversalPermission permissionsBack = permissionPair.second;

            if (permissionsFront.allowsAnything()) {
                street = getEdgeForStreet(start, end, way, index, startNode, endNode, length,
                        permissionsFront, geometry, false);
            }
            if (permissionsBack.allowsAnything()) {
                backStreet = getEdgeForStreet(end, start, way, index, endNode, startNode, length,
                        permissionsBack, backGeometry, true);
            }
            if (street != null && backStreet != null) {
                backStreet.shareData(street);
            }

            /* mark edges that are on roundabouts */
            if (way.isRoundabout()) {
                if (street != null)
                    street.setRoundabout(true);
                if (backStreet != null)
                    backStreet.setRoundabout(true);
            }

            return new P2<StreetEdge>(street, backStreet);
        }

        /**
         * Check OSM tags for various one-way and one-way-by-mode tags and return a pair of permissions for travel along and against the way.
         */
        private P2<StreetTraversalPermission> getPermissions(StreetTraversalPermission permissions,
                                                             OSMWay way) {

            StreetTraversalPermission permissionsFront = permissions;
            StreetTraversalPermission permissionsBack = permissions;

            // Check driving direction restrictions.
            if (way.isOneWayForwardDriving() || way.isRoundabout()) {
                permissionsBack = permissionsBack
                        .remove(StreetTraversalPermission.BICYCLE_AND_DRIVING);
            }
            if (way.isOneWayReverseDriving()) {
                permissionsFront = permissionsFront
                        .remove(StreetTraversalPermission.BICYCLE_AND_DRIVING);
            }

            // Check bike direction restrictions.
            if (way.isOneWayForwardBicycle()) {
                permissionsBack = permissionsBack.remove(StreetTraversalPermission.BICYCLE);
            }
            if (way.isOneWayReverseBicycle()) {
                permissionsFront = permissionsFront.remove(StreetTraversalPermission.BICYCLE);
            }

            // TODO(flamholz): figure out what this is for.
            String oneWayBicycle = way.getTag("oneway:bicycle");
            if (OSMWithTags.isFalse(oneWayBicycle) || way.isTagTrue("bicycle:backwards")) {
                if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
                    permissionsFront = permissionsFront.add(StreetTraversalPermission.BICYCLE);
                    permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
                }
            }

            if (way.isOpposableCycleway()) {
                permissionsBack = permissionsBack.add(StreetTraversalPermission.BICYCLE);
            }
            return new P2<StreetTraversalPermission>(permissionsFront, permissionsBack);
        }

        private StreetEdge getEdgeForStreet(IntersectionVertex start, IntersectionVertex end,
                                                 OSMWay way, int index, long startNode, long endNode, double length,
                                                 StreetTraversalPermission permissions, LineString geometry, boolean back) {

            String label = "way " + way.getId() + " from " + index;
            label = unique(label);
            String name = getNameForWay(way, label);

            // consider the elevation gain of stairs, roughly
            boolean steps = way.isSteps();
            if (steps) {
                length *= 2;
            }

            float carSpeed = wayPropertySet.getCarSpeedForWay(way, back);

            StreetEdge street = edgeFactory.createEdge(start, end, geometry, name, length,
                    permissions, back);
            street.setCarSpeed(carSpeed);

            String highway = way.getTag("highway");
            int cls;
            if ("crossing".equals(highway) && !way.isTag("bicycle", "designated")) {
                cls = StreetEdge.CLASS_CROSSING;
            } else if ("footway".equals(highway) && way.isTag("footway", "crossing")
                    && !way.isTag("bicycle", "designated")) {
                cls = StreetEdge.CLASS_CROSSING;
            } else if ("residential".equals(highway) || "tertiary".equals(highway)
                    || "secondary".equals(highway) || "secondary_link".equals(highway)
                    || "primary".equals(highway) || "primary_link".equals(highway)
                    || "trunk".equals(highway) || "trunk_link".equals(highway)) {
                cls = StreetEdge.CLASS_STREET;
            } else {
                cls = StreetEdge.CLASS_OTHERPATH;
            }

            cls |= getStreetClasses(way);
            street.setStreetClass(cls);

            if (!way.hasTag("name") && !way.hasTag("ref")) {
                street.setHasBogusName(true);
            }
            street.setStairs(steps);

            /* TODO: This should probably generalized somehow? */
            if (!ignoreWheelchairAccessibility
                    && (way.isTagFalse("wheelchair") || (steps && !way.isTagTrue("wheelchair")))) {
                street.setWheelchairAccessible(false);
            }

            street.setSlopeOverride(wayPropertySet.getSlopeOverride(way));

            // < 0.04: account for
            if (carSpeed < 0.04) {
                LOG.warn(graph.addBuilderAnnotation(new StreetCarSpeedZero(way.getId())));
            }

            if (customNamer != null) {
                customNamer.nameWithEdge(way, street);
            }

            return street;
        }

        private int getStreetClasses(OSMWithTags way) {
            int link = 0;
            String highway = way.getTag("highway");
            if (highway != null && highway.endsWith(("_link"))) {
                link = StreetEdge.CLASS_LINK;
            }
            return getPlatformClass(way) | link;
        }

        private int getPlatformClass(OSMWithTags way) {
            String highway = way.getTag("highway");
            if ("platform".equals(way.getTag("railway"))) {
                return StreetEdge.CLASS_TRAIN_PLATFORM;
            }
            if ("platform".equals(highway) || "platform".equals(way.getTag("public_transport"))) {
                if (way.isTagTrue("train") || way.isTagTrue("subway") || way.isTagTrue("tram")
                        || way.isTagTrue("monorail")) {
                    return StreetEdge.CLASS_TRAIN_PLATFORM;
                }
                return StreetEdge.CLASS_OTHER_PLATFORM;
            }
            return 0;
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

        /**
         * Record the level of the way for this node, e.g. if the way is at level 5, mark that this node is active at level 5.
         *
         * @param way the way that has the level
         * @param node the node to record for
         * @author mattwigway
         */
        private IntersectionVertex recordLevel(OSMNode node, OSMWithTags way) {
            OSMLevel level = osmdb.getLevelForWay(way);
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
                String label = this.getLevelNodeLabel(node, level);
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
         * Make or get a shared vertex for flat intersections, or one vertex per level for multilevel nodes like elevators. When there is an elevator
         * or other Z-dimension discontinuity, a single node can appear in several ways at different levels.
         *
         * @param node The node to fetch a label for.
         * @param way  The way it is connected to (for fetching level information).
         * @return vertex The graph vertex.
         */
        private IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
            // If the node should be decomposed to multiple levels,
            // use the numeric level because it is unique, the human level may not be (although
            // it will likely lead to some head-scratching if it is not).
            IntersectionVertex iv = null;
            if (node.isMultiLevel()) {
                // make a separate node for every level
                return recordLevel(node, way);
            }
            // single-level case
            long nid = node.getId();
            iv = intersectionNodes.get(nid);
            if (iv == null) {
                Coordinate coordinate = getCoordinate(node);
                String label = getNodeLabel(node);
                String highway = node.getTag("highway");
                if ("motorway_junction".equals(highway)) {
                    String ref = node.getTag("ref");
                    if (ref != null) {
                        ExitVertex ev = new ExitVertex(graph, label, coordinate.x, coordinate.y);
                        ev.setExitName(ref);
                        iv = ev;
                    }
                }

                if (node.isStop()) {
                    String ref = node.getTag("ref");
                    String name = node.getTag("name");
                    if (ref != null) {
                        TransitStopStreetVertex tsv = new TransitStopStreetVertex(graph, label, coordinate.x, coordinate.y, name, ref);
                        iv = tsv;
                    }
                }

                if (iv == null) {
                    iv = new IntersectionVertex(graph, label, coordinate.x, coordinate.y, label);
                    if (node.hasTrafficLight()) {
                        iv.trafficLight = (true);
                    }
                }

                intersectionNodes.put(nid, iv);
                endpoints.add(iv);
            }
            return iv;
        }
    }

    @Override
    public void checkInputs() {
        for (OpenStreetMapProvider provider : _providers) {
            provider.checkInputs();
        }
    }
}

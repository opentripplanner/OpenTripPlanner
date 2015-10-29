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

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.annotation.*;
import org.opentripplanner.graph_builder.module.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.*;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OpenStreetMapModule implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(OpenStreetMapModule.class);

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
     * Whether bike rental stations should be loaded from OSM, rather than periodically dynamically pulled from APIs. (default false)
     */
    public boolean staticBikeRental;

    /**
     * Whether we should create car P+R stations from OSM data. The default value is true. In normal operation it is
     * set by the JSON graph builder configuration, but it is also initialized to "true" here to provide the default
     * behavior in tests.
     */
    public boolean staticParkAndRide = true;

    /**
     * Whether we should create bike P+R stations from OSM data. (default false)
     */
    public boolean staticBikeParkAndRide;

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
    public OpenStreetMapModule(List<OpenStreetMapProvider> providers) {
        this.setProviders(providers);
    }

    public OpenStreetMapModule() {
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
        graph.hasStreets = true;
        //Calculates envelope for OSM
        graph.calculateEnvelope();
    }

    /*
     * TODO: What this function is supposed to do? Please comment or remove.
     */
    private <T> T unique(T value) {
        if (!_uniques.contains(value)) {
            _uniques.add(value);
        }
        return (T) value;
    }

    protected class Handler {

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
        private HashMap<Long, HashMap<OSMLevel, OsmVertex>> multiLevelNodes = new HashMap<Long, HashMap<OSMLevel, OsmVertex>>();

        // track OSM nodes that will become graph vertices because they appear in multiple OSM ways
        private Map<Long, OsmVertex> intersectionNodes = new HashMap<Long, OsmVertex>();

        // track vertices to be removed in the turn-graph conversion.
        // this is a superset of intersectionNodes.values, which contains
        // a null vertex reference for multilevel nodes. the individual vertices
        // for each level of a multilevel node are includeed in endpoints.
        private ArrayList<OsmVertex> endpoints = new ArrayList<OsmVertex>();

        public Handler(Graph graph, OSMDatabase osmdb) {
            this.graph = graph;
            this.osmdb = osmdb;
        }

        public void buildGraph(HashMap<Class<?>, Object> extra) {

            if (staticBikeRental) {
                processBikeRentalNodes();
            }

            if (staticBikeParkAndRide) {
                processBikeParkAndRideNodes();
            }

            for (Area area : Iterables.concat(osmdb.getWalkableAreas(),
                    osmdb.getParkAndRideAreas(), osmdb.getBikeParkingAreas()))
                setWayName(area.parent);

            // figure out which nodes that are actually intersections
            initIntersectionNodes();

            buildBasicGraph();
            buildWalkableAreas(skipVisibility);

            if (staticParkAndRide) {
                buildParkAndRideAreas();
            }
            if (staticBikeParkAndRide) {
                buildBikeParkAndRideAreas();
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
            BikeRentalStationService bikeRentalService = graph.getService(
                    BikeRentalStationService.class, true);
            graph.putService(BikeRentalStationService.class, bikeRentalService);
            for (OSMNode node : osmdb.getBikeRentalNodes()) {
                n++;
                //Gets name tag and translations if they exists
                //TODO: use wayPropertySet.getCreativeNameForWay(node)
                //Currently this names them as platform n
                I18NString creativeName = node.getAssumedName();
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
                    networkSet = null; // Special "catch-all" value
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
                bikeRentalService.addBikeRentalStation(station);
                BikeRentalStationVertex stationVertex = new BikeRentalStationVertex(graph, station);
                new RentABikeOnEdge(stationVertex, stationVertex, networkSet);
                new RentABikeOffEdge(stationVertex, stationVertex, networkSet);
            }
            if (n > 1) {
                graph.hasBikeSharing = true;
            }
            LOG.info("Created " + n + " bike rental stations.");
        }

        private void processBikeParkAndRideNodes() {
            LOG.info("Processing bike P+R nodes...");
            int n = 0;
            BikeRentalStationService bikeRentalService = graph.getService(
                    BikeRentalStationService.class, true);
            for (OSMNode node : osmdb.getBikeParkingNodes()) {
                n++;
                I18NString creativeName = wayPropertySet.getCreativeNameForWay(node);
                //TODO: localize
                if (creativeName == null)
                    creativeName = new NonLocalizedString("P+R");
                BikePark bikePark = new BikePark();
                bikePark.id = "" + node.getId();
                //TODO: localize bikePark name
                bikePark.name = creativeName.toString();
                bikePark.x = node.lon;
                bikePark.y = node.lat;
                bikeRentalService.addBikePark(bikePark);
                BikeParkVertex parkVertex = new BikeParkVertex(graph, bikePark);
                new BikeParkEdge(parkVertex);
            }
            LOG.info("Created " + n + " bike P+R.");
        }

        private void buildBikeParkAndRideAreas() {
            LOG.info("Building bike P+R areas");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getBikeParkingAreas());
            int n = 0;
            for (AreaGroup group : areaGroups) {
                for (Area area : group.areas) {
                    buildBikeParkAndRideForArea(area);
                    n++;
                }
            }
            if (n > 0) {
                graph.hasBikeRide = true;
            }
            LOG.info("Created {} bike P+R areas.", n);
        }

        /**
         * Build a bike P+R for the given area. Please note that, unlike car P+R, we do not use OSM
         * connectivity between the area and ways for linking the bike P+R to the road street
         * network. There aren't much bike park area in OSM data, but none of them are (properly)
         * linked to the street network (they are most of the time buildings). We just create a bike
         * P+R in the middle of the area envelope and rely on the same linking mechanism as for
         * nodes to connect them to the nearest streets.
         * 
         * @param area
         */
        private void buildBikeParkAndRideForArea(Area area) {
            BikeRentalStationService bikeRentalService = graph.getService(
                    BikeRentalStationService.class, true);
            Envelope envelope = new Envelope();
            long osmId = area.parent.getId();
            I18NString creativeName = wayPropertySet.getCreativeNameForWay(area.parent);
            for (Ring ring : area.outermostRings) {
                for (OSMNode node : ring.nodes) {
                    envelope.expandToInclude(new Coordinate(node.lon, node.lat));
                }
            }
            BikePark bikePark = new BikePark();
            bikePark.id = "" + osmId;
            //TODO: localize 
            bikePark.name = creativeName.toString();
            bikePark.x = (envelope.getMinX() + envelope.getMaxX()) / 2;
            bikePark.y = (envelope.getMinY() + envelope.getMaxY()) / 2;
            bikeRentalService.addBikePark(bikePark);
            BikeParkVertex bikeParkVertex = new BikeParkVertex(graph, bikePark);
            new BikeParkEdge(bikeParkVertex);
            LOG.debug("Created area bike P+R '{}' ({})", creativeName, osmId);
        }

        private void buildWalkableAreas(boolean skipVisibility) {
            if (skipVisibility) {
                LOG.info("Skipping visibility graph construction for walkable areas and using just area rings for edges.");
            } else {
                LOG.info("Building visibility graphs for walkable areas.");
            }
            List<AreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
            WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(graph, osmdb,
                    wayPropertySet, edgeFactory, this);
            if (skipVisibility) {
                for (AreaGroup group : areaGroups) {
                    walkableAreaBuilder.buildWithoutVisibility(group);
                }
            } else {
                for (AreaGroup group : areaGroups) {
                    walkableAreaBuilder.buildWithVisibility(group);
                }
            }
            // running a request caches the timezone; we need to clear it now so that when agencies are loaded
            // the graph time zone is set to the agency time zone.
            graph.clearTimeZone();
            if (skipVisibility) {
                LOG.info("Done building rings for walkable areas.");
            } else {
                LOG.info("Done building visibility graphs for walkable areas.");
            }
        }

        private void buildParkAndRideAreas() {
            LOG.info("Building P+R areas");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
            int n = 0;
            for (AreaGroup group : areaGroups) {
                if (buildParkAndRideAreasForGroup(group))
                    n++;
            }
            if (n > 0) {
                graph.hasParkRide = true;
            }
            LOG.info("Created {} P+R.", n);
        }

        private boolean buildParkAndRideAreasForGroup(AreaGroup group) {
            Envelope envelope = new Envelope();
            // Process all nodes from outer rings
            // These are IntersectionVertices not OsmVertices because there can be both OsmVertices and TransitStopStreetVertices.
            List<OsmVertex> accessVertexes = new ArrayList<OsmVertex>();
            I18NString creativeName = null;
            long osmId = 0L;
            for (Area area : group.areas) {
                osmId = area.parent.getId();
                if (creativeName == null || area.parent.getTag("name") != null)
                    creativeName = wayPropertySet.getCreativeNameForWay(area.parent);
                for (Ring ring : area.outermostRings) {
                    for (OSMNode node : ring.nodes) {
                        envelope.expandToInclude(new Coordinate(node.lon, node.lat));
                        OsmVertex accessVertex = getVertexForOsmNode(node, area.parent);
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
            for (OsmVertex accessVertex : accessVertexes) {
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
                LOG.warn(graph.addBuilderAnnotation(new ParkAndRideUnlinked((creativeName != null ? creativeName.toString() : "null"), osmId)));
                return false;
            }
            if (!walkAccessibleIn || !carAccessibleOut) {
                LOG.warn("P+R '{}' ({}) is not walk-accessible");
                // This does not prevent routing as we only use P+R for car dropoff,
                // but this is an issue with OSM data.
            }
            // Place the P+R at the center of the envelope
            ParkAndRideVertex parkAndRideVertex = new ParkAndRideVertex(graph, "P+R" + osmId,
                    "P+R_" + osmId, (envelope.getMinX() + envelope.getMaxX()) / 2,
                    (envelope.getMinY() + envelope.getMaxY()) / 2, creativeName);
            new ParkAndRideEdge(parkAndRideVertex);
            for (OsmVertex accessVertex : accessVertexes) {
                new ParkAndRideLinkEdge(parkAndRideVertex, accessVertex);
                new ParkAndRideLinkEdge(accessVertex, parkAndRideVertex);
            }
            LOG.debug("Created P+R '{}' ({})", creativeName, osmId);
            return true;
        }

        private List<AreaGroup> groupAreas(Collection<Area> areas) {
            Map<Area, OSMLevel> areasLevels = new HashMap<>(areas.size());
            for (Area area : areas) {
                areasLevels.put(area, osmdb.getLevelForWay(area.parent));
            }
            return AreaGroup.groupAreas(areasLevels);
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
                        wayData.getPermission(), graph);
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

                OsmVertex startEndpoint = null;
                OsmVertex endEndpoint = null;

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
                            || osmEndNode.isStop()
                            || osmEndNode.isBollard()) {
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

        // TODO Set this to private once WalkableAreaBuilder is gone
        protected void applyWayProperties(StreetEdge street, StreetEdge backStreet,
                                        WayProperties wayData, OSMWithTags way) {

            Set<T2<Alert, NoteMatcher>> notes = wayPropertySet.getNoteForWay(way);
            boolean noThruTraffic = way.isThroughTrafficExplicitlyDisallowed();
            // if (noThruTraffic) LOG.info("Way {} does not allow through traffic.", way.getId());
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
                I18NString creativeName = wayPropertySet.getCreativeNameForWay(way);
                if (creativeName != null) {
                    //way.addTag("otp:gen_name", creativeName);
                    way.setCreativeName(creativeName);
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
                HashMap<OSMLevel, OsmVertex> vertices = multiLevelNodes.get(nodeId);

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
                    OsmVertex sourceVertex = vertices.get(level);
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
            // filtered that way. So many missing edges are not really problems worth issuing warnings on.
            for (Long fromWay : osmdb.getTurnRestrictionWayIds()) {
                for (TurnRestrictionTag restrictionTag : osmdb.getFromWayTurnRestrictions(fromWay)) {
                    if (restrictionTag.possibleFrom.isEmpty()) {
                        graph.addBuilderAnnotation(new TurnRestrictionBad(restrictionTag.relationOSMID,
                            "No from edge found"));
                        continue;
                    }
                    if (restrictionTag.possibleTo.isEmpty()) {
                        graph.addBuilderAnnotation(
                            new TurnRestrictionBad(restrictionTag.relationOSMID,
                                "No to edge found"));
                        continue;
                    }
                    for (StreetEdge from : restrictionTag.possibleFrom) {
                        if (from == null) {
                            graph.addBuilderAnnotation(
                                new TurnRestrictionBad(restrictionTag.relationOSMID,
                                    "from-edge is null"));
                            continue;
                        }
                        for (StreetEdge to : restrictionTag.possibleTo) {
                            if (from == null || to == null) {
                                graph.addBuilderAnnotation(
                                    new TurnRestrictionBad(restrictionTag.relationOSMID,
                                        "to-edge is null"));
                                continue;
                            }
                            int angleDiff = from.getOutAngle() - to.getInAngle();
                            if (angleDiff < 0) {
                                angleDiff += 360;
                            }
                            switch (restrictionTag.direction) {
                            case LEFT:
                                if (angleDiff >= 160) {
                                    graph.addBuilderAnnotation(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "Left turn restriction is not on edges which turn left"));
                                    continue; // not a left turn
                                }
                                break;
                            case RIGHT:
                                if (angleDiff <= 200) {
                                    graph.addBuilderAnnotation(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "Right turn restriction is not on edges which turn right"));
                                    continue; // not a right turn
                                }
                                break;
                            case U:
                                if ((angleDiff <= 150 || angleDiff > 210)) {
                                    graph.addBuilderAnnotation(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "U-turn restriction is not on U-turn"));
                                    continue; // not a U turn
                                }
                                break;
                            case STRAIGHT:
                                if (angleDiff >= 30 && angleDiff < 330) {
                                    graph.addBuilderAnnotation(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "Straight turn restriction is not on edges which go straight"));
                                    continue; // not straight
                                }
                                break;
                            }
                            TurnRestriction restriction = new TurnRestriction();
                            restriction.from = from;
                            restriction.to = to;
                            restriction.type = restrictionTag.type;
                            restriction.modes = restrictionTag.modes;
                            restriction.time = restrictionTag.time;
                            graph.addTurnRestriction(from, restriction);
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
                d += SphericalDistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
            }
            return d;
        }

        /**
         * Handle oneway streets, cycleways, and other per-mode and universal access controls. See http://wiki.openstreetmap.org/wiki/Bicycle for
         * various scenarios, along with http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
         *
         * @param endEndpoint
         * @param startEndpoint
         */
        private P2<StreetEdge> getEdgesForStreet(OsmVertex startEndpoint,
                                                      OsmVertex endEndpoint, OSMWay way, int index, long startNode, long endNode,
                                                      StreetTraversalPermission permissions, LineString geometry) {
            // No point in returning edges that can't be traversed by anyone.
            if (permissions.allowsNothing()) {
                return new P2<StreetEdge>(null, null);
            }

            LineString backGeometry = (LineString) geometry.reverse();
            StreetEdge street = null, backStreet = null;
            double length = this.getGeometryLengthMeters(geometry);

            P2<StreetTraversalPermission> permissionPair = OSMFilter.getPermissions(permissions,
                    way);
            StreetTraversalPermission permissionsFront = permissionPair.first;
            StreetTraversalPermission permissionsBack = permissionPair.second;

            if (permissionsFront.allowsAnything()) {
                street = getEdgeForStreet(startEndpoint, endEndpoint, way, index, startNode, endNode, length,
                        permissionsFront, geometry, false);
            }
            if (permissionsBack.allowsAnything()) {
                backStreet = getEdgeForStreet(endEndpoint, startEndpoint, way, index, endNode, startNode, length,
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

        private StreetEdge getEdgeForStreet(OsmVertex startEndpoint, OsmVertex endEndpoint,
                                                 OSMWay way, int index, long startNode, long endNode, double length,
                                                 StreetTraversalPermission permissions, LineString geometry, boolean back) {

            String label = "way " + way.getId() + " from " + index;
            label = unique(label);
            I18NString name = getNameForWay(way, label);

            // consider the elevation gain of stairs, roughly
            boolean steps = way.isSteps();
            if (steps) {
                length *= 2;
            }

            float carSpeed = wayPropertySet.getCarSpeedForWay(way, back);

            StreetEdge street = edgeFactory.createEdge(startEndpoint, endEndpoint, geometry, name, length,
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

            cls |= OSMFilter.getStreetClasses(way);
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

            // save the way ID so we can match with OpenTraffic
            street.wayId = way.getId();

            return street;
        }

        // TODO Set this to private once WalkableAreaBuilder is gone
        protected I18NString getNameForWay(OSMWithTags way, String id) {
            I18NString name = way.getAssumedName();

            if (customNamer != null && name != null) {
                name = new NonLocalizedString(customNamer.name(way, name.toString()));
            }

            if (name == null) {
                name = new NonLocalizedString(id);
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
        private OsmVertex recordLevel(OSMNode node, OSMWithTags way) {
            OSMLevel level = osmdb.getLevelForWay(way);
            HashMap<OSMLevel, OsmVertex> vertices;
            long nodeId = node.getId();
            if (multiLevelNodes.containsKey(nodeId)) {
                vertices = multiLevelNodes.get(nodeId);
            } else {
                vertices = new HashMap<OSMLevel, OsmVertex>();
                multiLevelNodes.put(nodeId, vertices);
            }
            if (!vertices.containsKey(level)) {
                Coordinate coordinate = getCoordinate(node);
                String label = this.getLevelNodeLabel(node, level);
                OsmVertex vertex = new OsmVertex(graph, label, coordinate.x,
                         coordinate.y, node.getId(), new NonLocalizedString(label));
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
         * @return vertex The graph vertex. This is not always an OSM vertex; it can also be a TransitStopStreetVertex.
         */
        // TODO Set this to private once WalkableAreaBuilder is gone
        protected OsmVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
            // If the node should be decomposed to multiple levels,
            // use the numeric level because it is unique, the human level may not be (although
            // it will likely lead to some head-scratching if it is not).
            OsmVertex iv = null;
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
                        ExitVertex ev = new ExitVertex(graph, label, coordinate.x, coordinate.y, nid);
                        ev.setExitName(ref);
                        iv = ev;
                    }
                }

                /* If the OSM node represents a transit stop and has a ref=(stop_code) tag, make a special vertex for it. */
                if (node.isStop()) {
                    String ref = node.getTag("ref");
                    String name = node.getTag("name");
                    if (ref != null) {
                        TransitStopStreetVertex tsv = new TransitStopStreetVertex(graph, label, coordinate.x, coordinate.y, nid, name, ref);
                        iv = tsv;
                    }
                }

                if (node.isBollard()) {
                    BarrierVertex bv = new BarrierVertex(graph, label, coordinate.x, coordinate.y, nid);
                    bv.setBarrierPermissions(OSMFilter.getPermissionsForEntity(node, BarrierVertex.defaultBarrierPermissions));
                    iv = bv;
                }

                if (iv == null) {
                    iv = new OsmVertex(graph, label, coordinate.x, coordinate.y, node.getId(), new NonLocalizedString(label));
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

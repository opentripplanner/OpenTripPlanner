package org.opentripplanner.graph_builder.module.osm;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import com.google.common.collect.Iterables;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.Graphwide;
import org.opentripplanner.graph_builder.issues.InvalidVehicleParkingCapacity;
import org.opentripplanner.graph_builder.issues.ParkAndRideOpeningHoursUnparsed;
import org.opentripplanner.graph_builder.issues.ParkAndRideUnlinked;
import org.opentripplanner.graph_builder.issues.StreetCarSpeedZero;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.module.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.services.DefaultStreetEdgeFactory;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.StreetEdgeFactory;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.OsmOpeningHours;
import org.opentripplanner.routing.core.TimeRestriction;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.AreaEdgeList;
import org.opentripplanner.routing.edgetype.ElevatorAlightEdge;
import org.opentripplanner.routing.edgetype.ElevatorBoardEdge;
import org.opentripplanner.routing.edgetype.ElevatorHopEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.NamedArea;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParking.VehicleParkingEntranceCreator;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.ElevatorOffboardVertex;
import org.opentripplanner.routing.vertextype.ElevatorOnboardVertex;
import org.opentripplanner.routing.vertextype.ExitVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.TransitStopStreetVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.LocalizedStringFormat;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OpenStreetMapModule implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(OpenStreetMapModule.class);

    private static final String VEHICLE_PARKING_OSM_FEED_ID = "OSM";

    private DataImportIssueStore issueStore;

    // Private members that are only read or written internally.

    private Set<Object> _uniques = new HashSet<Object>();

    private HashMap<Vertex, Double> elevationData = new HashMap<Vertex, Double>();

    public boolean skipVisibility = false;

    public boolean platformEntriesLinking = false;

    // Members that can be set by clients.

    /**
     * WayPropertySet computes edge properties from OSM way data.
     */
    public WayPropertySet wayPropertySet = new WayPropertySet();

    /**
     * Providers of OSM data.
     */
    private List<BinaryOpenStreetMapProvider> _providers = new ArrayList<BinaryOpenStreetMapProvider>();

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
     * Whether we should create car P+R stations from OSM data. The default value is true. In normal
     * operation it is set by the JSON graph build configuration, but it is also initialized to
     * "true" here to provide the default behavior in tests.
     */
    public boolean staticParkAndRide = true;

    /**
     * Whether we should create bike P+R stations from OSM data. (default false)
     */
    public boolean staticBikeParkAndRide;

    private WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    public int maxAreaNodes = 500;

    public List<String> provides() {
        return Arrays.asList("streets", "turns");
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    /**
     * The source for OSM map data
     */
    public void setProvider(BinaryOpenStreetMapProvider provider) {
        _providers.add(provider);
    }

    /**
     * Multiple sources for OSM map data
     */
    public void setProviders(List<BinaryOpenStreetMapProvider> providers) {
        _providers.addAll(providers);
    }

    /**
     * Set the way properties from a {@link WayPropertySetSource} source.
     *
     * @param source the way properties source
     */
    public void setDefaultWayPropertySetSource(WayPropertySetSource source) {
        wayPropertySet = new WayPropertySet();
        source.populateProperties(wayPropertySet);
        wayPropertySetSource = source;
    }

    /**
     * Whether ways tagged foot/bicycle=discouraged should be marked as inaccessible
     */
    public boolean banDiscouragedWalking = false;
    public boolean banDiscouragedBiking = false;

    /**
     * Construct and set providers all at once.
     */
    public OpenStreetMapModule(List<BinaryOpenStreetMapProvider> providers) {
        this.setProviders(providers);
    }

    public OpenStreetMapModule() {
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        this.issueStore = issueStore;
        OSMDatabase osmdb = new OSMDatabase(issueStore);
        Handler handler = new Handler(graph, osmdb);
        for (BinaryOpenStreetMapProvider provider : _providers) {
            LOG.info("Gathering OSM from provider: " + provider);
            provider.readOSM(osmdb);
        }
        osmdb.postLoad();

        LOG.info("Using OSM way configuration from {}. Setting driving direction of the graph to {}.",
                wayPropertySetSource.getClass().getSimpleName(), wayPropertySetSource.drivingDirection());
        graph.setDrivingDirection(wayPropertySetSource.drivingDirection());
        graph.setIntersectionTraversalCostModel(wayPropertySetSource.getIntersectionTraversalCostModel());

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
            if (staticParkAndRide) {
                processParkAndRideNodes(osmdb.getCarParkingNodes(), true);
            }
            if (staticBikeParkAndRide) {
                processParkAndRideNodes(osmdb.getBikeParkingNodes(), false);
            }

            for (Area area : Iterables.concat(osmdb.getWalkableAreas(),
                    osmdb.getParkAndRideAreas(), osmdb.getBikeParkingAreas()))
                setWayName(area.parent);

            // figure out which nodes that are actually intersections
            initIntersectionNodes();

            buildBasicGraph();
            buildWalkableAreas(skipVisibility, platformEntriesLinking);

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

        private OptionalInt parseCapacity(OSMWithTags element) {
            return parseCapacity(element, "capacity");
        }

        private OptionalInt parseCapacity(OSMWithTags element, String capacityTag) {
            if (element.hasTag(capacityTag)) {
                String capacity = element.getTag(capacityTag);
                try {
                    int parsedValue = Integer.parseInt(capacity);
                    return OptionalInt.of(parsedValue);
                } catch (NumberFormatException e) {
                    issueStore.add(new InvalidVehicleParkingCapacity(element.getId(), capacity));
                }
            }
            return OptionalInt.empty();
        }

        private void processParkAndRideNodes(Collection<OSMNode> nodes, boolean isCarParkAndRide) {
            LOG.info("Processing {} P+R nodes.", isCarParkAndRide ? "car" : "bike");
            int n = 0;
            VehicleParkingService vehicleParkingService = graph.getService(
                VehicleParkingService.class, true);

            for (OSMNode node : nodes) {
                n++;

                I18NString creativeName = nameParkAndRideEntity(node);

                VehicleParkingEntranceCreator entrance = (builder) -> builder
                        .entranceId(new FeedScopedId(VEHICLE_PARKING_OSM_FEED_ID, String.format("%s/%s/entrance", node.getClass().getSimpleName(), node.getId())))
                        .name(creativeName)
                        .x(node.lon)
                        .y(node.lat)
                        .walkAccessible(true)
                        .carAccessible(isCarParkAndRide);

                var vehicleParking = createVehicleParkingObjectFromOsmEntity(
                        isCarParkAndRide, node.lon, node.lat, node, creativeName, List.of(entrance)
                );

                vehicleParkingService.addVehicleParking(vehicleParking);

                VehicleParkingEntranceVertex parkVertex = new VehicleParkingEntranceVertex(graph, vehicleParking.getEntrances().get(0));
                new VehicleParkingEdge(parkVertex);
            }

            LOG.info("Created {} {} P+R nodes.", n, isCarParkAndRide ? "car" : "bike");
        }

        private void buildBikeParkAndRideAreas() {
            LOG.info("Building bike P+R areas");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getBikeParkingAreas());
            int n = 0;
            for (AreaGroup group : areaGroups) {
                if (buildParkAndRideAreasForGroup(group, false))
                    n++;
            }
            if (n > 0) {
                graph.hasBikeRide = true;
            }
            LOG.info("Created {} bike P+R areas.", n);
        }

        private void buildWalkableAreas(boolean skipVisibility, boolean platformEntriesLinking) {
            if (skipVisibility) {
                LOG.info("Skipping visibility graph construction for walkable areas and using just area rings for edges.");
            } else {
                LOG.info("Building visibility graphs for walkable areas.");
            }
            List<AreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
            WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(graph, osmdb,
                    wayPropertySet, edgeFactory, this, issueStore, maxAreaNodes,
                    platformEntriesLinking
            );
            if (skipVisibility) {
                for (AreaGroup group : areaGroups) {
                    walkableAreaBuilder.buildWithoutVisibility(group);
                }
            } else {
                ProgressTracker progress = ProgressTracker.track(
                    "Build visibility graph for areas",
                    50,
                    areaGroups.size()
                );
                for (AreaGroup group : areaGroups) {
                    walkableAreaBuilder.buildWithVisibility(group);
                    //Keep lambda! A method-ref would causes incorrect class and line number to be logged
                    progress.step(m -> LOG.info(m));
                }
                LOG.info(progress.completeMessage());
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
            LOG.info("Building car P+R areas");
            List<AreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
            int n = 0;
            for (AreaGroup group : areaGroups) {
                if (buildParkAndRideAreasForGroup(group, true))
                    n++;
            }
            if (n > 0) {
                graph.hasParkRide = true;
            }
            LOG.info("Created {} car P+R areas.", n);
        }

        private boolean buildParkAndRideAreasForGroup(
                AreaGroup group,
                boolean isCarParkAndRide
        ) {

            Envelope envelope = new Envelope();
            Set<VertexAndName> accessVertices = new HashSet<>();

            OSMWithTags entity = null;

            // Process all nodes from outer rings
            // These are IntersectionVertices not OsmVertices because there can be both OsmVertices and TransitStopStreetVertices.
            for (Area area : group.areas) {
                entity = area.parent;

                var areaAccessVertices = processVehicleParkingArea(area, envelope);
                accessVertices.addAll(areaAccessVertices);
            }

            if (entity == null) {
                return false;
            }

            var creativeName = nameParkAndRideEntity(entity);

            // Check P+R accessibility by walking and driving.
            TraversalRequirements walkReq = new TraversalRequirements(new RoutingRequest(
                    TraverseMode.WALK));
            TraversalRequirements driveReq = new TraversalRequirements(new RoutingRequest(
                    TraverseMode.CAR));
            boolean walkAccessibleIn = false;
            boolean carAccessibleIn = false;
            boolean walkAccessibleOut = false;
            boolean carAccessibleOut = false;
            for (VertexAndName access : accessVertices) {
                var accessVertex = access.getVertex();
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

            if (isCarParkAndRide) {
                if (!walkAccessibleOut || !carAccessibleIn || !walkAccessibleIn || !carAccessibleOut) {
                    // This will prevent the P+R to be useful.
                    issueStore.add(new ParkAndRideUnlinked(creativeName.toString(), entity));
                    return false;
                }
            } else {
                if (!walkAccessibleOut || !walkAccessibleIn) {
                    // This will prevent the P+R to be useful.
                    issueStore.add(new ParkAndRideUnlinked(creativeName.toString(), entity));
                    return false;
                }
            }

            List<VehicleParking.VehicleParkingEntranceCreator> entrances = createParkingEntrancesFromAccessVertices(accessVertices, creativeName, entity);

            var vehicleParking = createVehicleParkingObjectFromOsmEntity(
                    isCarParkAndRide,
                    (envelope.getMinX() + envelope.getMaxX()) / 2,
                    (envelope.getMinY() + envelope.getMaxY()) / 2,
                    entity,
                    creativeName,
                    entrances
            );

            VehicleParkingService vehicleParkingService = graph.getService(VehicleParkingService.class, true);
            vehicleParkingService.addVehicleParking(vehicleParking);

            VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

            return true;
        }

        private VehicleParking createVehicleParkingObjectFromOsmEntity(
                boolean isCarParkAndRide,
                double lon,
                double lat,
                OSMWithTags entity,
                I18NString creativeName,
                List<VehicleParking.VehicleParkingEntranceCreator> entrances
        ) {
            OptionalInt bicycleCapacity, carCapacity, wheelchairAccessibleCarCapacity;
            if (isCarParkAndRide) {
                carCapacity = parseCapacity(entity);
                bicycleCapacity = parseCapacity(entity, "capacity:bike");
                wheelchairAccessibleCarCapacity = parseCapacity(entity, "capacity:disabled");
            } else {
                bicycleCapacity = parseCapacity(entity);
                carCapacity = OptionalInt.empty();
                wheelchairAccessibleCarCapacity = OptionalInt.empty();
            }

            VehicleParkingSpaces vehicleParkingSpaces = null;
            if (bicycleCapacity.isPresent() || carCapacity.isPresent() || wheelchairAccessibleCarCapacity.isPresent()) {
                vehicleParkingSpaces = VehicleParkingSpaces.builder()
                    .bicycleSpaces(bicycleCapacity.isPresent() ? bicycleCapacity.getAsInt() : null)
                    .carSpaces(carCapacity.isPresent() ? carCapacity.getAsInt() : null)
                    .wheelchairAccessibleCarSpaces(wheelchairAccessibleCarCapacity.isPresent() ? wheelchairAccessibleCarCapacity.getAsInt() : null)
                    .build();
            }

            var bicyclePlaces = !isCarParkAndRide || bicycleCapacity.orElse(0) > 0;
            var carPlaces = (
                    isCarParkAndRide &&
                            wheelchairAccessibleCarCapacity.isEmpty() && carCapacity.isEmpty()
            ) || carCapacity.orElse(0) > 0;
            var wheelchairAccessibleCarPlaces = wheelchairAccessibleCarCapacity.orElse(0) > 0;

            var openingHours = parseVehicleParkingOpeningHours(entity, creativeName);

            var id = new FeedScopedId(
                    VEHICLE_PARKING_OSM_FEED_ID,
                    String.format("%s/%d", entity.getClass().getSimpleName(), entity.getId())
            );

            var tags = new ArrayList<String>();

            tags.add(isCarParkAndRide ? "osm:amenity=parking" : "osm:amenity=bicycle_parking");

            if (entity.isTagTrue("fee")) {
                tags.add("osm:fee");
            }
            if (entity.hasTag("supervised") && !entity.isTagTrue("supervised")) {
                tags.add("osm:supervised");
            }
            if (entity.hasTag("covered") && !entity.isTagFalse("covered")) {
                tags.add("osm:covered");
            }
            if (entity.hasTag("surveillance") && !entity.isTagFalse("surveillance")) {
                tags.add("osm:surveillance");
            }

            return VehicleParking.builder()
                    .id(id)
                    .name(creativeName)
                    .x(lon)
                    .y(lat)
                    .tags(tags)
                    .detailsUrl(entity.getTag("website"))
                    .openingHours(openingHours)
                    .bicyclePlaces(bicyclePlaces)
                    .carPlaces(carPlaces)
                    .wheelchairAccessibleCarPlaces(wheelchairAccessibleCarPlaces)
                    .capacity(vehicleParkingSpaces)
                    .entrances(entrances)
                    .build();
        }

        private TimeRestriction parseVehicleParkingOpeningHours(OSMWithTags entity, I18NString creativeName) {
            final var openingHoursTag = entity.getTag("opening_hours");
            if (openingHoursTag != null) {
                try {
                    return OsmOpeningHours.parseFromOsm(openingHoursTag);
                } catch (OpeningHoursParseException e) {
                    issueStore.add(new ParkAndRideOpeningHoursUnparsed(
                            creativeName.toString(), entity, openingHoursTag
                    ));
                }
            }
            return null;
        }

        private I18NString nameParkAndRideEntity(OSMWithTags osmWithTags) {
            // If there is an explicit name user that. The explicit name is used so that tag-based
            // translations are used, which are not handled by "CreativeNamer"s.
            I18NString creativeName = osmWithTags.getAssumedName();
            if (creativeName == null) {
                // ... otherwise resort to "CreativeNamer"s
                creativeName = wayPropertySet.getCreativeNameForWay(osmWithTags);
            }
            return creativeName;
        }

        private List<VertexAndName> processVehicleParkingArea(Area area, Envelope envelope) {
            return area.outermostRings.stream()
                    .flatMap(ring -> processVehicleParkingArea(ring, area.parent, envelope).stream())
                    .collect(Collectors.toList());
        }

        private List<VertexAndName> processVehicleParkingArea(Ring ring, OSMWithTags entity, Envelope envelope) {
            List<VertexAndName> accessVertices = new ArrayList<>();
            for (OSMNode node : ring.nodes) {
                envelope.expandToInclude(new Coordinate(node.lon, node.lat));
                OsmVertex accessVertex = getVertexForOsmNode(node, entity);
                if (accessVertex.getIncoming().isEmpty()
                        || accessVertex.getOutgoing().isEmpty())
                    continue;
                accessVertices.add(new VertexAndName(node.getAssumedName(), accessVertex));
            }

            accessVertices.addAll(
                    ring.getHoles().stream()
                            .flatMap(innerRing -> processVehicleParkingArea(innerRing, entity, envelope).stream())
                            .collect(Collectors.toList())
            );

            return accessVertices;
        }

        private List<VehicleParking.VehicleParkingEntranceCreator> createParkingEntrancesFromAccessVertices(
                Set<VertexAndName> accessVertices,
                I18NString vehicleParkingName,
                OSMWithTags entity
        ) {
            List<VehicleParking.VehicleParkingEntranceCreator> entrances = new ArrayList<>();

            for (var access : accessVertices) {
                I18NString suffix = null;
                if (access.getName() != null) {
                    suffix = access.getName();
                }

                if (suffix == null) {
                        suffix = new NonLocalizedString(String.format("#%d", entrances.size() + 1));
                }

                var entranceName = new LocalizedStringFormat("%s (%s)", vehicleParkingName, suffix);

                entrances.add((builder) -> builder
                    .entranceId(new FeedScopedId(VEHICLE_PARKING_OSM_FEED_ID, String.format("%s/%d/%d", entity.getClass().getSimpleName(), entity.getId(), access.getVertex().nodeId)))
                    .name(entranceName)
                    .x(access.getVertex().getX())
                    .y(access.getVertex().getY())
                    .vertex(access.getVertex())
                    .walkAccessible(access.getVertex().isConnectedToWalkingEdge())
                    .carAccessible(access.getVertex().isConnectedToDriveableEdge()));
            }

            return entrances;
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
            ProgressTracker progress = ProgressTracker.track("Build street graph", 5_000, wayCount);
            LOG.info(progress.startMessage());

            WAY:
            for (OSMWay way : osmdb.getWays()) {

                if (wayIndex % 10000 == 0)
                    LOG.debug("ways=" + wayIndex + "/" + wayCount);
                wayIndex++;

                WayProperties wayData = wayPropertySet.getDataForWay(way);

                setWayName(way);

                StreetTraversalPermission permissions = OSMFilter.getPermissionsForWay(way,
                        wayData.getPermission(), graph, banDiscouragedWalking, banDiscouragedBiking,
                    issueStore
                );
                if (!OSMFilter.isWayRoutable(way) || permissions.allowsNothing())
                    continue;

                // handle duplicate nodes in OSM ways
                // this is a workaround for crappy OSM data quality
                ArrayList<Long> nodes = new ArrayList<Long>(way.getNodeRefs().size());
                long last = -1;
                double lastLat = -1, lastLon = -1;
                String lastLevel = null;
                for (TLongIterator iter = way.getNodeRefs().iterator(); iter.hasNext(); ) {
                    long nodeId = iter.next();
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
                            || osmEndNode.isBarrier()) {
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

                //Keep lambda! A method-ref would causes incorrect class and line number to be logged
                progress.step(m -> LOG.info(m));
            } // END loop over OSM ways

            LOG.info(progress.completeMessage());
        }

        // TODO Set this to private once WalkableAreaBuilder is gone
        protected void applyWayProperties(StreetEdge street, StreetEdge backStreet,
                                        WayProperties wayData, OSMWithTags way) {

            Set<T2<StreetNote, NoteMatcher>> notes = wayPropertySet.getNoteForWay(way);
            boolean motorVehicleNoThrough = wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(way);
            boolean bicycleNoThrough = wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(way);
            boolean walkNoThrough = wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(way);

            if (street != null) {
                double safety = wayData.getSafetyFeatures().first;
                street.setBicycleSafetyFactor((float)safety);
                if (safety < bestBikeSafety) {
                    bestBikeSafety = (float)safety;
                }
                if (notes != null) {
                    for (T2<StreetNote, NoteMatcher> note : notes)
                        graph.streetNotesService.addStaticNote(street, note.first, note.second);
                }
                street.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
                street.setBicycleNoThruTraffic(bicycleNoThrough);
                street.setWalkNoThruTraffic(walkNoThrough);
            }

            if (backStreet != null) {
                double safety = wayData.getSafetyFeatures().second;
                if (safety < bestBikeSafety) {
                    bestBikeSafety = (float)safety;
                }
                backStreet.setBicycleSafetyFactor((float)safety);
                if (notes != null) {
                    for (T2<StreetNote, NoteMatcher> note : notes)
                        graph.streetNotesService.addStaticNote(backStreet, note.first, note.second);
                }
                backStreet.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
                backStreet.setBicycleNoThruTraffic(bicycleNoThrough);
                backStreet.setWalkNoThruTraffic(walkNoThrough);
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
                        issueStore.add(new TurnRestrictionBad(restrictionTag.relationOSMID,
                            "No from edge found"));
                        continue;
                    }
                    if (restrictionTag.possibleTo.isEmpty()) {
                        issueStore.add(
                            new TurnRestrictionBad(restrictionTag.relationOSMID,
                                "No to edge found"));
                        continue;
                    }
                    for (StreetEdge from : restrictionTag.possibleFrom) {
                        if (from == null) {
                            issueStore.add(
                                new TurnRestrictionBad(restrictionTag.relationOSMID,
                                    "from-edge is null"));
                            continue;
                        }
                        for (StreetEdge to : restrictionTag.possibleTo) {
                            if (from == null || to == null) {
                                issueStore.add(
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
                                    issueStore.add(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "Left turn restriction is not on edges which turn left"));
                                    continue; // not a left turn
                                }
                                break;
                            case RIGHT:
                                if (angleDiff <= 200) {
                                    issueStore.add(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "Right turn restriction is not on edges which turn right"));
                                    continue; // not a right turn
                                }
                                break;
                            case U:
                                if ((angleDiff <= 150 || angleDiff > 210)) {
                                    issueStore.add(
                                        new TurnRestrictionBad(restrictionTag.relationOSMID,
                                            "U-turn restriction is not on U-turn"));
                                    continue; // not a U turn
                                }
                                break;
                            case STRAIGHT:
                                if (angleDiff >= 30 && angleDiff < 330) {
                                    issueStore.add(
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
                TLongList nodes = way.getNodeRefs();
                nodes.forEach(node -> {
                    if (possibleIntersectionNodes.contains(node)) {
                        intersectionNodes.put(node, null);
                    } else {
                        possibleIntersectionNodes.add(node);
                    }
                    return true;
                });
            }
            // Intersect ways at area boundaries if needed.
            for (Area area : Iterables.concat(osmdb.getWalkableAreas(), osmdb.getParkAndRideAreas(), osmdb.getBikeParkingAreas())) {
                for (Ring outerRing : area.outermostRings) {
                    intersectAreaRingNodes(possibleIntersectionNodes, outerRing);
                }
            }
        }

        private void intersectAreaRingNodes(Set<Long> possibleIntersectionNodes, Ring outerRing) {
            for (OSMNode node : outerRing.nodes) {
                long nodeId = node.getId();
                if (possibleIntersectionNodes.contains(nodeId)) {
                    intersectionNodes.put(nodeId, null);
                } else {
                    possibleIntersectionNodes.add(nodeId);
                }
            }

            outerRing.getHoles().forEach(hole -> intersectAreaRingNodes(possibleIntersectionNodes, hole));
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
            issueStore.add(new Graphwide(
                    "Multiplying all bike safety values by " + (1 / bestBikeSafety)));
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
                issueStore.add(new StreetCarSpeedZero(way.getId()));
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

                if (node.isBarrier()) {
                    BarrierVertex bv = new BarrierVertex(graph, label, coordinate.x, coordinate.y, nid);
                    bv.setBarrierPermissions(OSMFilter.getPermissionsForEntity(node, BarrierVertex.defaultBarrierPermissions));
                    iv = bv;
                }

                if (iv == null) {
                    iv = new OsmVertex(graph, label, coordinate.x, coordinate.y, node.getId(), new NonLocalizedString(label));
                    if (node.hasTrafficLight()) {
                        iv.trafficLight = true;
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
        for (BinaryOpenStreetMapProvider provider : _providers) {
            provider.checkInputs();
        }
    }

    static class VertexAndName {

        private final I18NString name;
        private final OsmVertex vertex;

        VertexAndName(I18NString name, OsmVertex vertex) {
            this.name = name;
            this.vertex = vertex;
        }

        public I18NString getName() {
            return this.name;
        }

        public OsmVertex getVertex() {
            return this.vertex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            final VertexAndName that = (VertexAndName) o;
            return Objects.equals(name, that.name) && vertex.equals(that.vertex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, vertex);
        }
    }
}

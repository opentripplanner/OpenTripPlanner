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

package org.opentripplanner.api.resource;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;
import com.vividsolutions.jts.geom.LineString;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.model.alertpatch.AlertPatchResponse;
import org.opentripplanner.api.parameter.QualifiedModeSetSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.TransitToStreetNetworkGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.AttributeFeatureConverter;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopMatcher;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.MemoryGraphSource;
import org.opentripplanner.routing.impl.TravelingSalesmanPathService;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/* This is a hack to hold context and graph data between test runs, since loading it is slow. */
class Context {
    /**
     * Save a temporary graph object when this is true
     */
    private static final boolean DEBUG_OUTPUT = false;

    public Graph graph = spy(new Graph());

    public GraphServiceImpl graphService = new GraphServiceImpl();

    public CommandLineParameters commandLineParameters = new CommandLineParameters();

    public OTPServer otpServer = new OTPServer(commandLineParameters, graphService);

    private static Context instance = null;

    public static Context getInstance() {
        if (instance == null) {
            instance = new Context();
        }
        return instance;
    }

    public Context() {
        graphService.registerGraph("", new MemoryGraphSource("", makeSimpleGraph())); // default graph is tiny test graph
        graphService.registerGraph("portland", new MemoryGraphSource("portland", graph));
        ShapefileStreetGraphBuilderImpl builder = new ShapefileStreetGraphBuilderImpl();
        FeatureSourceFactory factory = new ShapefileFeatureSourceFactoryImpl(new File(
                "src/test/resources/portland/Streets_pdx.shp"));
        builder.setFeatureSourceFactory(factory);
        ShapefileStreetSchema schema = new ShapefileStreetSchema();
        schema.setIdAttribute("LOCALID");
        schema.setNameAttribute("FULL_NAME");

        CaseBasedTraversalPermissionConverter perms = new CaseBasedTraversalPermissionConverter(
                "DIRECTION", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        perms.addPermission("2", StreetTraversalPermission.ALL,
                StreetTraversalPermission.PEDESTRIAN);
        perms.addPermission("3", StreetTraversalPermission.PEDESTRIAN,
                StreetTraversalPermission.ALL);
        perms.addPermission("1", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

        schema.setPermissionConverter(perms);

        // as a test, use prefixes ("NE", SE", etc) as an alert
        schema.setNoteConverter(new AttributeFeatureConverter<String>("PREFIX"));

        builder.setSchema(schema);
        builder.buildGraph(graph, new HashMap<Class<?>, Object>());
        initTransit();

        initBikeRental();
        graph.index(new DefaultStreetVertexIndexFactory());

        if (DEBUG_OUTPUT) {
            try {
                graph.save(File.createTempFile("graph", ".obj"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // to make test results more deterministic, only find the single best path
        // If this is really needed, it should be set in the RoutingRequest in individual tests
        //((GenericAStar) otpServer.sptService).nPaths = (1);

        // Create dummy TimetableResolver
        TimetableResolver resolver = new TimetableResolver();

        // Mock TimetableSnapshotSource to return dummy TimetableResolver
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);

        when(timetableSnapshotSource.getTimetableSnapshot()).thenReturn(resolver);

        graph.timetableSnapshotSource = (timetableSnapshotSource);
    }

    private void initTransit() {
        GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl();
        GtfsBundle bundle = new GtfsBundle();
        bundle.setPath(new File("src/test/resources/google_transit.zip"));

        ArrayList<GtfsBundle> bundleList = new ArrayList<GtfsBundle>();
        bundleList.add(bundle);
        GtfsBundles bundles = new GtfsBundles();
        bundles.setBundles(bundleList);
        gtfsBuilder.setGtfsBundles(bundles);

        HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();
        gtfsBuilder.buildGraph(graph, extra);

        TransitToStreetNetworkGraphBuilderImpl linker =
                new TransitToStreetNetworkGraphBuilderImpl();
        linker.buildGraph(graph, extra);

    }

    private void initBikeRental() {
        BikeRentalStationService service = graph.getService(BikeRentalStationService.class, true);
        BikeRentalStation station = new BikeRentalStation();
        station.x = -122.637634;
        station.y = 45.513084;
        station.bikesAvailable = 5;
        station.spacesAvailable = 4;
        station.id = "1";
        station.name = "bike rental station";
        service.addBikeRentalStation(station);
    }

    private Graph makeSimpleGraph() {
        Graph graph = new Graph();
        StreetVertex tl = new IntersectionVertex(graph, "tl", -80.01, 40.01, "top and left");
        StreetVertex tr = new IntersectionVertex(graph, "tr", -80.0, 40.01, "top and right");
        StreetVertex bl = new IntersectionVertex(graph, "bl", -80.01, 40.0, "bottom and left");
        StreetVertex br = new IntersectionVertex(graph, "br", -80.0, 40.0, "bottom and right");

        makeEdges(tl, tr, "top");
        makeEdges(tl, bl, "left");
        makeEdges(br, tr, "right");
        makeEdges(bl, br, "bottom");

        return graph;
    }

    private void makeEdges(StreetVertex v1, StreetVertex v2, String name) {
        LineString geometry = GeometryUtils.makeLineString(v1.getCoordinate().x,
                v1.getCoordinate().y, v2.getCoordinate().x, v2.getCoordinate().y);
        double length = SphericalDistanceLibrary.getInstance().distance(v1.getCoordinate(),
                v2.getCoordinate());
        new StreetEdge(v1, v2, geometry, name, length, StreetTraversalPermission.ALL, false);

        geometry = GeometryUtils.makeLineString(v2.getCoordinate().x, v2.getCoordinate().y,
                v1.getCoordinate().x, v1.getCoordinate().y);
        new StreetEdge(v2, v1, geometry, name, length, StreetTraversalPermission.ALL, true);
    }
}

public class TestRequest extends TestCase {
    public void testRequest() {
        /** Moved to {@link RoutingRequestTest). */
    }

    public void testBuildRequest() throws Exception {
        /** Removed on grounds of being redundant and testing internal functionality only. */
    }

    public void testPlanner() throws Exception {
        TestPlanner planner = new TestPlanner(
                "portland", "From::NE 43RD AVE at NE GLISAN ST", "To::NE 43RD AVE at NE ROYAL CT");

        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(0);
        List<WalkStep> steps = leg.walkSteps;
        assertEquals(3, steps.size());
        WalkStep step0 = steps.get(0);
        WalkStep step2 = steps.get(2);
        assertEquals(AbsoluteDirection.NORTH, step0.absoluteDirection);
        assertEquals("NE 43RD AVE", step0.streetName);
        assertEquals("NE 43RD AVE", step2.streetName);
        assertEquals(RelativeDirection.LEFT, step2.relativeDirection);
        assertTrue(step2.stayOn);
        assertEquals("From", response.getPlan().from.orig);
        assertEquals("From", leg.from.orig);
        leg = itinerary.legs.get(itinerary.legs.size() - 1);
        assertEquals("To", leg.to.orig);
        assertEquals("To", response.getPlan().to.orig);
    }

    public void testFirstAndLastLeg() throws Exception {
        /** Subsumed by tests in {@link PlanGeneratorTest}. */
    }

    public void testAlerts() throws Exception {
        // SE 47th and Ash, NE 47th and Davis (note that we cross Burnside, this goes from SE to NE)
        TestPlanner planner = new TestPlanner(
                "portland", "SE 47TH AVE at SE ASH ST", "NE 47TH AVE at NE COUCH ST");
        Response response = planner.getItineraries();

        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(0);
        List<WalkStep> steps = leg.walkSteps;
        assertEquals(2, steps.size());
        WalkStep step0 = steps.get(0);
        WalkStep step1 = steps.get(1);

        assertNotNull(step0.alerts);
        assertEquals(1, step0.alerts.size());
        assertEquals("SE", step0.alerts.get(0).alertHeaderText.getSomeTranslation());

        assertEquals(1, step1.alerts.size());
        assertEquals("NE", step1.alerts.get(0).alertHeaderText.getSomeTranslation());
    }

    public void testIntermediate() throws Exception {
        Vertex v1 = getVertexByCrossStreets("NW 10TH AVE", "W BURNSIDE ST");
        Vertex v2 = getVertexByCrossStreets("SE 82ND AVE", "SE ASH ST").getOutgoing().iterator()
                .next().getToVertex();
        Vertex v3 = getVertexByCrossStreets("NE 21ST AVE", "NE MASON ST").getOutgoing().iterator()
                .next().getToVertex();
        Vertex v4 = getVertexByCrossStreets("SE 92ND AVE", "SE FLAVEL ST");
        Vertex[] vertices = { v1, v3, v2, v4 };
        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);
        assertNotNull(v4);

        TestPlanner planner = new TestPlanner("portland", v1.getLabel(), v4.getLabel(),
                Arrays.asList(v2.getLabel(), v3.getLabel()));
        List<GraphPath> paths = planner.getPaths();

        assertTrue(paths.size() > 0);
        GraphPath path = paths.get(0);
        int curVertex = 0;
        for (State s : path.states) {
            if (s.getVertex().equals(vertices[curVertex])) {
                curVertex += 1;
            }
        }
        assertEquals(4, curVertex); // found all four, in the correct order (1, 3, 2, 4)
    }

    private Vertex getVertexByCrossStreets(String s1, String s2) {
        for (Vertex v : Context.getInstance().graph.getVertices()) {
            if (v.getName().contains(s1) && v.getName().contains(s2)) {
                return v;
            }
        }
        return null;
    }

    public void testBikeRental() {
        BikeRental bikeRental = new BikeRental();
        bikeRental.otpServer = Context.getInstance().otpServer;
        // no stations in graph
        BikeRentalStationList stations = bikeRental.getBikeRentalStations(null, null, null);
        assertEquals(0, stations.stations.size());

        // no stations in range
        stations = bikeRental.getBikeRentalStations("55.5,-122.7", "65.6,-122.6", "portland");
        assertEquals(0, stations.stations.size());
        // finally, a station
        stations = bikeRental.getBikeRentalStations("45.5,-122.7", "45.6,-122.6", "portland");
        assertEquals(1, stations.stations.size());
    }

    public void testMetadata() {
        Metadata metadata = new Metadata();
        metadata.otpServer = Context.getInstance().otpServer;
        GraphMetadata data1 = metadata.getMetadata(null);
        assertTrue("centerLatitude is not 40.005; got " + data1.getCenterLatitude(),
                Math.abs(40.005 - data1.getCenterLatitude()) < 0.000001);

        GraphMetadata data2 = metadata.getMetadata("portland");
        assertTrue(Math.abs(-122 - data2.getCenterLongitude()) < 1);
        assertTrue(Math.abs(-122 - data2.getLowerLeftLongitude()) < 2);
        assertTrue(Math.abs(-122 - data2.getUpperRightLongitude()) < 2);
    }

    /** Smoke test for patcher */
    public void testPatcher() {
        AlertPatcher p = new AlertPatcher();
        AlertPatchService service = mock(AlertPatchService.class);
        when(service.getStopPatches(any(AgencyAndId.class))).thenReturn(new ArrayList<AlertPatch>());
        when(service.getRoutePatches(any(AgencyAndId.class))).thenReturn(new ArrayList<AlertPatch>());

        p.alertPatchService = service;
        AlertPatchResponse stopPatches = p.getStopPatches("TriMet", "5678");
        assertNull(stopPatches.alertPatches);
        AlertPatchResponse routePatches = p.getRoutePatches("TriMet", "100");
        assertNull(routePatches.alertPatches);
    }

    public void testRouters() {
        /** Moved to {@link RoutersTest). */
    }

    public void testBannedTrips() {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner(
                "portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");
        // Ban trips with ids 190W1280 and 190W1260 from agency with id TriMet
        planner.setBannedTrips(Arrays.asList("TriMet:190W1280,TriMet:190W1260"));
        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        // Without bannedTrips this leg would contain a trip with id 190W1280
        assertFalse(leg.tripId.equals("190W1280"));
        // Instead a trip is now expected with id 190W1290
        assertTrue(leg.tripId.equals("190W1290"));
    }

    public void testBannedStops() throws ParameterException {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner(
                "portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");
        // Ban stops with ids 2106 and 2107 from agency with id TriMet
        // These are the two stops near NE 30TH AVE at NE GLISAN ST
        planner.setBannedStops(Arrays.asList("TriMet:2106,TriMet:2107"));
        // Do the planning
        Response response = planner.getItineraries();
        // First check the request
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        // Without bannedStops this leg would stop at the stop with id 2107
        assertFalse(leg.to.stopId.getId().equals("2107"));
        // Instead a stop is now expected with id 2109
        assertTrue(leg.to.stopId.getId().equals("2109"));
    }

    public void testBannedStopGroup() throws ParameterException {
        // Create StopMatcher instance
        StopMatcher stopMatcher = StopMatcher.parse("TriMet:2106,TriMet:65-tc");
        // Find stops in graph
        Graph graph = Context.getInstance().graph;

        Stop stop65_tc = ((TransitStationStop) graph.getVertex("TriMet:65-tc")).getStop();
        assertNotNull(stop65_tc);

        Stop stop12921 = ((TransitStationStop) graph.getVertex("TriMet:12921")).getStop();
        assertNotNull(stop12921);

        Stop stop13132 = ((TransitStationStop) graph.getVertex("TriMet:13132")).getStop();
        assertNotNull(stop13132);

        Stop stop2106 = ((TransitStationStop) graph.getVertex("TriMet:2106")).getStop();
        assertNotNull(stop2106);

        Stop stop2107 = ((TransitStationStop) graph.getVertex("TriMet:2107")).getStop();
        assertNotNull(stop2107);

        // Match stop with id 65-tc
        assertTrue(stopMatcher.matches(stop65_tc));
        // Match stop with id 12921 that has TriMet:65-tc as a parent
        assertTrue(stopMatcher.matches(stop12921));
        // Match stop with id 13132 that has TriMet:65-tc as a parent
        assertTrue(stopMatcher.matches(stop13132));
        // Match stop with id 2106
        assertTrue(stopMatcher.matches(stop2106));
        // Match stop with id 2107
        assertFalse(stopMatcher.matches(stop2107));
    }

    public void testBannedStopsHard() throws ParameterException {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner(
                "portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");

        // Do the planning
        Response response = planner.getItineraries();
        // First check the request
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        // Validate that this leg uses trip 190W1280
        assertTrue(leg.tripId.equals("190W1280"));

        // Ban stop hard with id 2009 from agency with id TriMet
        // This is a stop that will be passed when using trip 190W1280
        planner.setBannedStopsHard(Arrays.asList("TriMet:2009"));

        // Do the planning again
        response = planner.getItineraries();
        // First check the request
        itinerary = response.getPlan().itinerary.get(0);
        leg = itinerary.legs.get(1);
        // Validate that this leg doesn't use trip 190W1280
        assertFalse(leg.tripId.equals("190W1280"));
    }

    public void testWalkLimitExceeded() throws ParameterException {
        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.501115,-122.738214", "45.469487,-122.500343");
        // Do the planning
        Response response = planner.getItineraries();
        // Check itinerary for walkLimitExceeded
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        assertTrue(itinerary.walkDistance > planner.getMaxWalkDistance().get(0));
        assertTrue(itinerary.walkLimitExceeded);

        planner = new TestPlanner("portland", "45.445631,-122.845388", "45.459961,-122.752347");
        // Do the planning with high walk reluctance
        response = planner.getItineraries();
        // Check itinerary for walkLimitExceeded
        itinerary = response.getPlan().itinerary.get(0);
        assertTrue(itinerary.walkDistance <= planner.getMaxWalkDistance().get(0));
        assertFalse(itinerary.walkLimitExceeded);
    }

    public void testTransferPenalty() {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");
        // Don't use non-preferred transfer penalty
        planner.setNonpreferredTransferPenalty(Arrays.asList(0));
        // Check number of legs when using different transfer penalties
        checkLegsWithTransferPenalty(planner, 0, 7, false);
        //checkLegsWithTransferPenalty(planner, 1800, 7, true);
    }

    /**
     * Checks the number of legs when using a specific transfer penalty while planning.
     * @param planner is the test planner to use
     * @param transferPenalty is the value for the transfer penalty
     * @param expectedLegs is the number of expected legs
     * @param smaller if true, number of legs should be smaller;
     *                if false, number of legs should be exact
     */
    private void checkLegsWithTransferPenalty(TestPlanner planner, int transferPenalty,
            int expectedLegs, boolean smaller) {
        // Set transfer penalty
        planner.setTransferPenalty(Arrays.asList(transferPenalty));
        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the number of legs
        if (smaller) {
            assertTrue(itinerary.legs.size() < expectedLegs);
        } else {
            assertEquals(expectedLegs, itinerary.legs.size());
        }
    }

    public void testTripToTripTransfer() throws ParseException {
        ServiceDate serviceDate = new ServiceDate(2009, 10, 01);

        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        Graph graph = Context.getInstance().graph;
        when(graph.getTransferTable()).thenReturn(table);

        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses
        assertEquals("190W1280", itinerary.legs.get(1).tripId);
        assertEquals("751W1330", itinerary.legs.get(3).tripId);

        // Now add a transfer between the two busses of minimal 126 seconds
        //(transfer was 125 seconds)
        addTripToTripTransferTimeToTable(
                table, "2111", "7452", "19", "75", "190W1280", "751W1330", 126);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The id of the second bus should be different
        assertEquals("190W1280", itinerary.legs.get(1).tripId);
        assertFalse("751W1330".equals(itinerary.legs.get(3).tripId));

        // Now apply a real-time update: let the to-trip have a delay of 3 seconds
        Trip trip = graph.index.tripForId.get(new AgencyAndId("TriMet", "751W1330"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        applyUpdateToTripPattern(pattern, "751W1330", "7452", 79, 41228, 41228,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, they should be the original again
        assertEquals("190W1280", itinerary.legs.get(1).tripId);
        assertEquals("751W1330", itinerary.legs.get(3).tripId);

        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "751W1330", "7452", 79, 41225, 41225,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testForbiddenTripToTripTransfer() {
        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        Graph graph = Context.getInstance().graph;
        when(graph.getTransferTable()).thenReturn(table);

        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses
        assertEquals("190W1280", itinerary.legs.get(1).tripId);
        assertEquals("751W1330", itinerary.legs.get(3).tripId);

        // Now add a forbidden transfer between the two busses
        addTripToTripTransferTimeToTable(table, "2111", "7452", "19", "75", "190W1280", "751W1330",
                StopTransfer.FORBIDDEN_TRANSFER);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The ids of the first two busses should be different
        assertFalse("190W1280".equals(itinerary.legs.get(1).tripId)
                && "751W1330".equals(itinerary.legs.get(3).tripId));

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }
    /*
    TODO add tests for transfer penalties and PreferredTripToTripTransfer

    The expected results seem to be dependent on errors in the older DefaultRemainingWeightHeuristic.

    public void testPreferredTripToTripTransfer() {
        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.506077,-122.621139", "45.464637,-122.706061");

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        Graph graph = Context.getInstance().graph;
        when(graph.getTransferTable()).thenReturn(table);

        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses
        assertEquals("751W1320", itinerary.legs.get(1).tripId);
        assertEquals("91W1350", itinerary.legs.get(3).tripId);

        // Now add a preferred transfer between two other busses
        addTripToTripTransferTimeToTable(table, "7528", "9756", "75", "12", "750W1300", "120W1320"
                , StopTransfer.PREFERRED_TRANSFER);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the preferred transfer should be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);

        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }
    */

    public void testTimedTripToTripTransfer() throws ParseException {
        ServiceDate serviceDate = new ServiceDate(2009, 10, 01);

        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.506077,-122.621139", "45.464637,-122.706061");

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        Graph graph = Context.getInstance().graph;
        when(graph.getTransferTable()).thenReturn(table);

        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses
/*
        FIXME this test is expecting certain trip IDs to be present.
        These values seem to be dependent on quirks and details of the routing parameters.
        Tests should not expect very specific route combinations from real-world data.
        These can easily change due to minor tweaks in the routing process.

        assertEquals("751W1320", itinerary.legs.get(1).tripId);
        assertEquals("91W1350", itinerary.legs.get(3).tripId);
*/
        // Now add a timed transfer between two other busses
        addTripToTripTransferTimeToTable(table, "7528", "9756", "75", "12", "750W1300", "120W1320"
                , StopTransfer.TIMED_TRANSFER);
        // Don't forget to also add a TimedTransferEdge
        Vertex fromVertex = graph.getVertex("TriMet:7528_arrive");
        Vertex toVertex = graph.getVertex("TriMet:9756_depart");
        TimedTransferEdge timedTransferEdge = new TimedTransferEdge(fromVertex, toVertex);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);

        // Now apply a real-time update: let the to-trip be early by 240 seconds,
        // resulting in a transfer time of 0 seconds
        Trip trip = graph.index.tripForId.get(new AgencyAndId("TriMet", "120W1320"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 22, 41580, 41580,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should still be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);

        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 22, 41820, 41820,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);
        // Remove the timed transfer from the graph
        timedTransferEdge.detach(graph);
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testTimedStopToStopTransfer() throws ParseException {
        ServiceDate serviceDate = new ServiceDate(2009, 10, 01);

        // Plan short trip
        TestPlanner planner = new TestPlanner(
                "portland", "45.506077,-122.621139", "45.464637,-122.706061");

        // Replace the transfer table with an empty table
        TransferTable table = new TransferTable();
        Graph graph = Context.getInstance().graph;
        when(graph.getTransferTable()).thenReturn(table);

        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);

/* FIXME see similar problem in testTimedTripToTripTransfer

        // Check the ids of the first two busses
        assertEquals("751W1320", itinerary.legs.get(1).tripId);
        assertEquals("91W1350", itinerary.legs.get(3).tripId);
*/

        // Now add a timed transfer between two other busses
        addStopToStopTransferTimeToTable(table, "7528", "9756", StopTransfer.TIMED_TRANSFER);
        // Don't forget to also add a TimedTransferEdge
        Vertex fromVertex = graph.getVertex("TriMet:7528_arrive");
        Vertex toVertex = graph.getVertex("TriMet:9756_depart");
        TimedTransferEdge timedTransferEdge = new TimedTransferEdge(fromVertex, toVertex);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);

        // Now apply a real-time update: let the to-trip be early by 240 seconds,
        // resulting in a transfer time of 0 seconds
        Trip trip = graph.index.tripForId.get(new AgencyAndId("TriMet", "120W1320"));
        TripPattern pattern = graph.index.patternForTrip.get(trip);
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 22, 41580, 41580,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should still be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);

        // Now apply a real-time update: let the to-trip be early by 241 seconds,
        // resulting in a transfer time of -1 seconds
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 22, 41579, 41579,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);

        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The ids of the first two busses should be different
        assertFalse("190W1280".equals(itinerary.legs.get(1).tripId)
                && "751W1330".equals(itinerary.legs.get(3).tripId));

        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 22, 41820, 41820,
                ScheduleRelationship.SCHEDULED, 0, serviceDate);
        // Remove the timed transfer from the graph
        timedTransferEdge.detach(graph);
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    /**
     * Add a trip-to-trip transfer time to a transfer table and check the result
     */
    private void addTripToTripTransferTimeToTable(TransferTable table, String fromStopId,
            String toStopId, String fromRouteId, String toRouteId, String fromTripId,
            String toTripId, int transferTime) {
        Stop fromStop = new Stop();
        fromStop.setId(new AgencyAndId("TriMet", fromStopId));
        Stop toStop = new Stop();
        toStop.setId(new AgencyAndId("TriMet", toStopId));
        Route fromRoute = new Route();
        fromRoute.setId(new AgencyAndId("TriMet", fromRouteId));
        Route toRoute = new Route();
        toRoute.setId(new AgencyAndId("TriMet", toRouteId));
        Trip fromTrip = new Trip();
        fromTrip.setId(new AgencyAndId("TriMet", fromTripId));
        fromTrip.setRoute(fromRoute);
        Trip toTrip = new Trip();
        toTrip.setId(new AgencyAndId("TriMet", toTripId));
        toTrip.setRoute(toRoute);
        table.addTransferTime(fromStop, toStop, null, null, fromTrip, toTrip, transferTime);
        assertEquals(transferTime, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
    }

    /**
     * Add a stop-to-stop transfer time to a transfer table and check the result
     */
    private void addStopToStopTransferTimeToTable(TransferTable table, String fromStopId,
            String toStopId, int transferTime) {
        Stop fromStop = new Stop();
        fromStop.setId(new AgencyAndId("TriMet", fromStopId));
        Stop toStop = new Stop();
        toStop.setId(new AgencyAndId("TriMet", toStopId));
        Route fromRoute = new Route();
        fromRoute.setId(new AgencyAndId("TriMet", "dummy"));
        Route toRoute = new Route();
        toRoute.setId(new AgencyAndId("TriMet", "dummy"));
        Trip fromTrip = new Trip();
        fromTrip.setId(new AgencyAndId("TriMet", "dummy"));
        fromTrip.setRoute(fromRoute);
        Trip toTrip = new Trip();
        toTrip.setId(new AgencyAndId("TriMet", "dummy"));
        toTrip.setRoute(toRoute);
        table.addTransferTime(fromStop, toStop, null, null, null, null, transferTime);
        assertEquals(transferTime, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
    }

    /**
     * Apply an update to a table trip pattern and check whether the update was applied correctly
     */
    private void applyUpdateToTripPattern(TripPattern pattern, String tripId, String stopId,
            int stopSeq, int arrive, int depart, ScheduleRelationship scheduleRelationship,
            int timestamp, ServiceDate serviceDate) throws ParseException {
        Graph graph = Context.getInstance().graph;
        TimetableResolver snapshot = graph.timetableSnapshotSource.getTimetableSnapshot();
        Timetable timetable = snapshot.resolve(pattern, serviceDate);
        TimeZone timeZone = new SimpleTimeZone(-7, "PST");
        long today = serviceDate.getAsDate(timeZone).getTime() / 1000;
        TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId(tripId);

        StopTimeEvent.Builder departStopTimeEventBuilder = StopTimeEvent.newBuilder();
        StopTimeEvent.Builder arriveStopTimeEventBuilder = StopTimeEvent.newBuilder();

        departStopTimeEventBuilder.setTime(today + depart);
        arriveStopTimeEventBuilder.setTime(today + arrive);

        StopTimeUpdate.Builder stopTimeUpdateBuilder = StopTimeUpdate.newBuilder();

        stopTimeUpdateBuilder.setStopSequence(stopSeq);
        stopTimeUpdateBuilder.setDeparture(departStopTimeEventBuilder);
        stopTimeUpdateBuilder.setArrival(arriveStopTimeEventBuilder);
        stopTimeUpdateBuilder.setScheduleRelationship(scheduleRelationship);

        TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        tripUpdateBuilder.addStopTimeUpdate(0, stopTimeUpdateBuilder);

        TripUpdate tripUpdate = tripUpdateBuilder.build();

        assertTrue(timetable.update(tripUpdate, timeZone, serviceDate));
    }

    /**
     * Subclass of Planner for testing. Constructor sets fields that would usually be set by Jersey
     * from HTTP Query string.
     */
    private static class TestPlanner extends Planner {
        // TODO Shouldn't we use the Router pathService instead?
        // And why do we need a TravelingSalesmanPathService btw?
        private TravelingSalesmanPathService tsp;

        public TestPlanner(String routerId, String v1, String v2) {
            super();
            this.fromPlace = Arrays.asList(v1);
            this.toPlace = Arrays.asList(v2);
            this.date = Arrays.asList("2009-10-01");
            this.time = Arrays.asList("11:11:11");
            this.maxWalkDistance = Arrays.asList(1600.0);
            this.walkReluctance = Arrays.asList(8.0);
            this.walkSpeed = Arrays.asList(1.33);
            this.optimize = Arrays.asList(OptimizeType.QUICK);
            this.modes = Arrays.asList(new QualifiedModeSetSequence("WALK,TRANSIT"));
            this.numItineraries = Arrays.asList(1);
            this.transferPenalty = Arrays.asList(0);
            this.nonpreferredTransferPenalty = Arrays.asList(180);
            this.maxTransfers = Arrays.asList(2);
            this.bikeSwitchTime = Arrays.asList(0);
            this.bikeSwitchCost = Arrays.asList(0);
            this.routerId = routerId; // not a list because this is a path parameter not a query parameter
            this.numItineraries = Arrays.asList(1); // make results more deterministic by returning only one path
            this.otpServer = Context.getInstance().otpServer;
        }

        public TestPlanner(String routerId, String v1, String v2, List<String> intermediates) {
            this(routerId, v1, v2);
            this.modes = Arrays.asList(new QualifiedModeSetSequence("WALK"));
            this.intermediatePlaces = intermediates;
            Router router = otpServer.getRouter(routerId);
            tsp = new TravelingSalesmanPathService(router.graph, router.pathService);
        }

        public void setBannedTrips(List<String> bannedTrips) {
            this.bannedTrips = bannedTrips;
        }

        public void setBannedStops(List<String> bannedStops) {
            this.bannedStops = bannedStops;
        }

        public void setBannedStopsHard(List<String> bannedStopsHard) {
            this.bannedStopsHard = bannedStopsHard;
        }

        public void setTransferPenalty(List<Integer> transferPenalty) {
            this.transferPenalty = transferPenalty;
        }

        public void setNonpreferredTransferPenalty(List<Integer> nonpreferredTransferPenalty) {
            this.nonpreferredTransferPenalty = nonpreferredTransferPenalty;
        }

        public List<Double> getMaxWalkDistance() {
            return this.maxWalkDistance;
        }

        public RoutingRequest buildRequest() throws ParameterException {
            return super.buildRequest();
        }

        public List<GraphPath> getPaths() {
            try {
                RoutingRequest options = this.buildRequest();
                options.intermediatePlacesOrdered = false;
                return tsp.getPaths(options);
            } catch (ParameterException e) {
                e.printStackTrace();
                return null;
            }
        }

        public Response getItineraries() {
            return getItineraries(otpServer, null);
        }

        public Response getFirstTrip() {
            time = Arrays.asList("00:00:00");
            return getItineraries();
        }
    }
}

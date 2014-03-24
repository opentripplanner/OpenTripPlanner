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

/* this is in api.common so it can set package-private fields */
package org.opentripplanner.api.resource;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.Agency;
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
import org.opentripplanner.api.model.RouterInfo;
import org.opentripplanner.api.model.RouterList;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.model.internals.EdgeSet;
import org.opentripplanner.api.model.internals.FeatureCount;
import org.opentripplanner.api.model.internals.VertexSet;
import org.opentripplanner.api.model.patch.PatchResponse;
import org.opentripplanner.api.model.transit.AgencyList;
import org.opentripplanner.api.model.transit.ModeList;
import org.opentripplanner.api.model.transit.RouteData;
import org.opentripplanner.api.model.transit.RouteDataList;
import org.opentripplanner.api.model.transit.RouteList;
import org.opentripplanner.api.model.transit.StopList;
import org.opentripplanner.api.model.transit.StopTimeList;
import org.opentripplanner.api.parameter.QualifiedModeSetSequence;
import org.opentripplanner.api.resource.BikeRental;
import org.opentripplanner.api.resource.BikeRentalStationList;
import org.opentripplanner.api.resource.GraphMetadata;
import org.opentripplanner.api.resource.Metadata;
import org.opentripplanner.api.resource.Patcher;
import org.opentripplanner.api.resource.PlanGenerator;
import org.opentripplanner.api.resource.Planner;
import org.opentripplanner.api.resource.Response;
import org.opentripplanner.api.resource.Routers;
import org.opentripplanner.api.resource.TransitIndex;
import org.opentripplanner.api.resource.services.MetadataService;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.impl.GtfsGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.TransitToStreetNetworkGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.AttributeFeatureConverter;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.impl.transit_index.TransitIndexBuilder;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.GraphBuilderWithGtfsDao;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.model.json_serialization.WithGraph;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopMatcher;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.impl.TravelingSalesmanPathService;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.trippattern.TripUpdateList;
import org.opentripplanner.routing.trippattern.Update;
import org.opentripplanner.routing.trippattern.Update.Status;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TestUtils;

import com.vividsolutions.jts.geom.LineString;

class SimpleGraphServiceImpl implements GraphService {

    private HashMap<String, Graph> graphs = new HashMap<String, Graph>();

    @Override
    public void setLoadLevel(LoadLevel level) {
    }

    @Override
    public Graph getGraph() {
        return graphs.get(null);
    }

    @Override
    public Graph getGraph(String routerId) {
        return graphs.get(routerId);
    }

    @Override
    public Collection<String> getRouterIds() {
        return graphs.keySet();
    }

    public void putGraph(String graphId, Graph graph) {
        graphs.put(graphId, graph);
    }

    @Override
    public boolean registerGraph(String graphId, boolean preEvict) {
        return false;
    }

    @Override
    public boolean registerGraph(String graphId, Graph graph) {
        return false;
    }

    @Override
    public boolean evictGraph(String graphId) {
        return false;
    }

    @Override
    public int evictAll() {
        return 0;
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean save(String routerId, InputStream is) {
    	return false;
    }
}

/* This is a hack to hold context and graph data between test runs, since loading it is slow. */
class Context {
    
    /**
     * Save a temporary graph object when this is true
     */
    private static final boolean DEBUG_OUTPUT = false;

    public Graph graph = spy(new Graph());

    public SimpleGraphServiceImpl graphService = new SimpleGraphServiceImpl();

    public PlanGenerator planGenerator = new PlanGenerator();

    public RetryingPathServiceImpl pathService = new RetryingPathServiceImpl();

    private static Context instance = null;

    public static Context getInstance() {
        if (instance == null) {
            instance = new Context();
        }
        return instance;
    }

    public Context() {
        graphService.putGraph(null, makeSimpleGraph()); // default graph is tiny test graph
        graphService.putGraph("portland", graph);
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
        graph.streetIndex = new StreetVertexIndexServiceImpl(graph);

        if (DEBUG_OUTPUT) {
            try {
                graph.save(File.createTempFile("graph", ".obj"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        GenericAStar star = new GenericAStar();
        star.setNPaths(1); // to make test results more deterministic, only find the single best path
        pathService.setSptService(star);
        pathService.setGraphService(graphService);
        planGenerator.pathService = pathService;
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

        gtfsBuilder.setGtfsGraphBuilders(Arrays
                .asList((GraphBuilderWithGtfsDao) new TransitIndexBuilder()));

        HashMap<Class<?>, Object> extra = new HashMap<Class<?>, Object>();
        gtfsBuilder.buildGraph(graph, extra);

        TransitToStreetNetworkGraphBuilderImpl linker = new TransitToStreetNetworkGraphBuilderImpl();
        linker.buildGraph(graph, extra);

    }

    private void initBikeRental() {
        BikeRentalStationService service = new BikeRentalStationService();
        BikeRentalStation station = new BikeRentalStation();
        station.x = -122.637634;
        station.y = 45.513084;
        station.bikesAvailable = 5;
        station.spacesAvailable = 4;
        station.id = "1";
        station.name = "bike rental station";

        service.addStation(station);
        graph.putService(BikeRentalStationService.class, service);
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
        new PlainStreetEdge(v1, v2, geometry, name, length, StreetTraversalPermission.ALL, false);

        geometry = GeometryUtils.makeLineString(v2.getCoordinate().x, v2.getCoordinate().y,
                v1.getCoordinate().x, v1.getCoordinate().y);
        new PlainStreetEdge(v2, v1, geometry, name, length, StreetTraversalPermission.ALL, true);
    }
}

public class TestRequest extends TestCase {

    public void testRequest() {
        RoutingRequest request = new RoutingRequest();

        request.addMode(TraverseMode.CAR);
        assertTrue(request.getModes().getCar());
        request.removeMode(TraverseMode.CAR);
        assertFalse(request.getModes().getCar());
        
        request.addMode(TraverseMode.CUSTOM_MOTOR_VEHICLE);
        assertFalse(request.getModes().getCar());
        assertTrue(request.getModes().getDriving());
        request.removeMode(TraverseMode.CUSTOM_MOTOR_VEHICLE);
        assertFalse(request.getModes().getCar());
        assertFalse(request.getModes().getDriving());

        request.setModes(new TraverseModeSet("BICYCLE,WALK"));
        assertFalse(request.getModes().getCar());
        assertTrue(request.getModes().getBicycle());
        assertTrue(request.getModes().getWalk());
    }

    public void testBuildRequest() throws Exception {
        TestPlanner planner = new TestPlanner("portland", "45.58,-122.68", "45.48,-122.6");
        RoutingRequest options = planner.buildRequest();

        assertEquals(new Date(1254420671000L), options.getDateTime());
        assertEquals(1600.0, options.getMaxWalkDistance());
        assertEquals(8.0, options.getWalkReluctance());
        assertEquals(1, options.getNumItineraries());
    }

    public void testPlanner() throws Exception {

        Planner planner = new TestPlanner("portland", "NE 43RD AVE at NE GLISAN ST", "NE 43RD AVE at NE ROYAL CT");

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

    }

    public void testFirstTrip() throws Exception {

        Planner planner = new TestPlanner("portland", "45.58,-122.68", "45.48,-122.6");

        Response response = planner.getFirstTrip();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        assertTrue(leg.startTime.get(Calendar.HOUR) >= 4);
        assertTrue(leg.startTime.get(Calendar.HOUR) <= 7);

    }

    public void testAlerts() throws Exception {

        // SE 47th and Ash, NE 47th and Davis (note that we cross Burnside, this goes from SE to NE)
        Planner planner = new TestPlanner("portland", "SE 47TH AVE at SE ASH ST", "NE 47TH AVE at NE COUCH ST");
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
        bikeRental.setGraphService(Context.getInstance().graphService);
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

    public void testMetadata() throws JSONException {
        Metadata metadata = new Metadata();
        MetadataService metadataService = new MetadataService();
        metadata.setMetadataService(metadataService);
        metadataService.setGraphService(Context.getInstance().graphService);
        GraphMetadata data1 = metadata.getMetadata(null);
        assertTrue("centerLatitude is not 40.005; got " + data1.getCenterLatitude(),
                Math.abs(40.005 - data1.getCenterLatitude()) < 0.000001);

        GraphMetadata data2 = metadata.getMetadata("portland");
        assertTrue(Math.abs(-122 - data2.getCenterLongitude()) < 1);
        assertTrue(Math.abs(-122 - data2.getLowerLeftLongitude()) < 2);
        assertTrue(Math.abs(-122 - data2.getUpperRightLongitude()) < 2);

    }

    /** Smoke test for patcher */
    public void testPatcher() throws JSONException {
        Patcher p = new Patcher();
        PatchService service = mock(PatchService.class);
        when(service.getStopPatches(any(AgencyAndId.class))).thenReturn(new ArrayList<Patch>());
        when(service.getRoutePatches(any(AgencyAndId.class))).thenReturn(new ArrayList<Patch>());

        p.setPatchService(service);
        PatchResponse stopPatches = p.getStopPatches("TriMet", "5678");
        assertNull(stopPatches.patches);
        PatchResponse routePatches = p.getRoutePatches("TriMet", "100");
        assertNull(routePatches.patches);

    }

    public void testRouters() throws JSONException {
        Routers routerApi = new Routers();
        routerApi.graphService = Context.getInstance().graphService;
        RouterList routers = routerApi.getRouterIds();
        assertEquals(2, routers.routerInfo.size());
        RouterInfo router0 = routers.routerInfo.get(0);
        RouterInfo router1 = routers.routerInfo.get(1);
        RouterInfo otherRouter;
        RouterInfo defaultRouter;
        if (router0.routerId == null) {
            defaultRouter = router0;
            otherRouter = router1;
        } else {
            defaultRouter = router1;
            otherRouter = router0;
        }
        assertNull(defaultRouter.routerId);
        assertNotNull(otherRouter.routerId);
        assertTrue(otherRouter.polygon.getArea() > 0);
    }

    public void testTransitIndex() throws JSONException {
        TransitIndex index = new TransitIndex();
        index.setGraphService(Context.getInstance().graphService);
        String routerId = "portland";
        AgencyList agencyIds = index.getAgencyIds(routerId);
        assertEquals(agencyIds.agencies.toArray(new Agency[0])[0].getId(), ("TriMet"));
        assertEquals(1, agencyIds.agencies.size());

        RouteDataList routeDataList = (RouteDataList) index.getRouteData("TriMet", "100", false, false,
                routerId);
        assertEquals(new AgencyAndId("TriMet", "100"), routeDataList.routeData.toArray(new RouteData[0])[0].id);
        assertTrue(routeDataList.routeData.toArray(new RouteData[0])[0].variants.size() >= 2);

        RouteList routes = (RouteList) index.getRoutes("TriMet", false, routerId);
        assertTrue(routes.routes.size() > 50);

        //without agencyId
        routes = (RouteList) index.getRoutes(null, true, routerId);
        assertTrue(routes.routes.size() > 50);

        //without agencyId
        routes = (RouteList) index.getRoutes(null, true, routerId);
        assertTrue(routes.routes.size() > 50);

        ModeList modes = (ModeList) index.getModes(routerId);
        assertTrue(modes.modes.contains(TraverseMode.TRAM));
        assertFalse(modes.modes.contains(TraverseMode.FUNICULAR));

        RouteList routesForStop = (RouteList) index.getRoutesForStop("TriMet", "10579", false,
                routerId);
        assertEquals(1, routesForStop.routes.size());

        routesForStop = (RouteList) index.getRoutesForStop(null, "10579", false,
                routerId);
        assertEquals(1, routesForStop.routes.size());
        // assertEquals("MAX Red Line", routesForStop.routes.get(0).routeLongName);

        StopList stopsNearPoint = (StopList) index.getStopsNearPoint("TriMet", 45.464783,
                -122.578918, false, routerId, null);
        assertTrue(stopsNearPoint.stops.size() > 0);

        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 9, 1, 7, 50, 0);
        long endTime = startTime + 60 * 60;
        StopTimeList stopTimesForStop = (StopTimeList) index.getStopTimesForStop("TriMet", "10579",
                startTime, endTime, false, false, null, routerId);
        assertTrue(stopTimesForStop.stopTimes.size() > 0);

        stopTimesForStop = (StopTimeList) index.getStopTimesForStop(null, "10579",
                startTime, endTime, false, false, null, routerId);
        assertTrue(stopTimesForStop.stopTimes.size() > 0);

        stopTimesForStop = (StopTimeList) index.getStopTimesForStop(null, "10579",
                startTime, endTime, false, false, null, routerId);
        assertTrue(stopTimesForStop.stopTimes.size() > 0);

        stopTimesForStop = (StopTimeList) index.getStopTimesForStop(null, "10579",
                startTime, endTime, false, false, null, routerId);
        assertTrue(stopTimesForStop.stopTimes.size() > 0);

        // StopTimeList stopTimesForTrip = (StopTimeList) index.getStopTimesForTrip("TriMet", "1254",
        // "TriMet", "10W1040", startTime, routerId);
        // assertTrue(stopTimesForTrip.stopTimes.size() > 0);
    }

    public void testBannedTrips() throws JSONException {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner("portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");
        // Ban trips with ids 190W1280 and 190W1260 from agency with id TriMet
        planner.setBannedTrips(Arrays.asList("TriMet_190W1280,TriMet_190W1260"));
        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        // Without bannedTrips this leg would contain a trip with id 190W1280
        assertFalse(leg.tripId.equals("190W1280"));
        // Instead a trip is now expected with id 190W1290
        assertTrue(leg.tripId.equals("190W1290"));
    }

    public void testBannedStops() throws JSONException, ParameterException {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner("portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");
        // Ban stops with ids 2106 and 2107 from agency with id TriMet
        // These are the two stops near NE 30TH AVE at NE GLISAN ST
        planner.setBannedStops(Arrays.asList("TriMet_2106,TriMet_2107"));
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
    
    @SuppressWarnings("deprecation")
    public void testBannedStopGroup() throws JSONException, ParameterException {
        // Create StopMatcher instance
        StopMatcher stopMatcher = StopMatcher.parse("TriMet_2106,TriMet_65-tc");
        // Find stops in graph
        Graph graph = Context.getInstance().graph;
        
        Stop stop65_tc = ((TransitStop) graph.getVertex("TriMet_65-tc")).getStop();
        assertNotNull(stop65_tc);
        
        Stop stop12921 = ((TransitStop) graph.getVertex("TriMet_12921")).getStop();
        assertNotNull(stop12921);
        
        Stop stop13132 = ((TransitStop) graph.getVertex("TriMet_13132")).getStop();
        assertNotNull(stop13132);
        
        Stop stop2106 = ((TransitStop) graph.getVertex("TriMet_2106")).getStop();
        assertNotNull(stop2106);
        
        Stop stop2107 = ((TransitStop) graph.getVertex("TriMet_2107")).getStop();
        assertNotNull(stop2107);
        
        // Match stop with id 65-tc
        assertTrue(stopMatcher.matches(stop65_tc));
        // Match stop with id 12921 that has TriMet_65-tc as a parent
        assertTrue(stopMatcher.matches(stop12921));
        // Match stop with id 13132 that has TriMet_65-tc as a parent
        assertTrue(stopMatcher.matches(stop13132));
        // Match stop with id 2106
        assertTrue(stopMatcher.matches(stop2106));
        // Match stop with id 2107
        assertFalse(stopMatcher.matches(stop2107));
    }
    
    public void testBannedStopsHard() throws JSONException, ParameterException {
        // Plan short trip along NE GLISAN ST
        TestPlanner planner = new TestPlanner("portland", "NE 57TH AVE at NE GLISAN ST #2", "NE 30TH AVE at NE GLISAN ST");

        // Do the planning
        Response response = planner.getItineraries();
        // First check the request
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(1);
        // Validate that this leg uses trip 190W1280
        assertTrue(leg.tripId.equals("190W1280"));
        
        // Ban stop hard with id 2009 from agency with id TriMet
        // This is a stop that will be passed when using trip 190W1280
        planner.setBannedStopsHard(Arrays.asList("TriMet_2009"));
        
        // Do the planning again
        response = planner.getItineraries();
        // First check the request
        itinerary = response.getPlan().itinerary.get(0);
        leg = itinerary.legs.get(1);
        // Validate that this leg doesn't use trip 190W1280
        assertFalse(leg.tripId.equals("190W1280"));
    }
    
    public void testWalkLimitExceeded() throws JSONException, ParameterException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.501115,-122.738214", "45.469487,-122.500343");
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
    
    public void testTransferPenalty() throws JSONException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");
        // Don't use non-preferred transfer penalty
        planner.setNonpreferredTransferPenalty(Arrays.asList(0));
        // Check number of legs when using different transfer penalties
        checkLegsWithTransferPenalty(planner, 0, 7, false);
        checkLegsWithTransferPenalty(planner, 1800, 7, true);
    }

    public void testTransferPenalty2() throws JSONException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.514861,-122.612035", "45.483096,-122.540624");
        // Don't use non-preferred transfer penalty
        planner.setNonpreferredTransferPenalty(Arrays.asList(0));
        // Check number of legs when using different transfer penalties
        checkLegsWithTransferPenalty(planner, 0, 5, false);
        checkLegsWithTransferPenalty(planner, 1800, 5, true);
    }
    
    /**
     * Checks the number of legs when using a specific transfer penalty while planning.
     * @param planner is the test planner to use
     * @param transferPenalty is the value for the transfer penalty
     * @param expectedLegs is the number of expected legs
     * @param smaller if true, number of legs should be smaller; if false, number of legs should be exact
     * @throws JSONException
     */
    private void checkLegsWithTransferPenalty(TestPlanner planner, int transferPenalty,
            int expectedLegs, boolean smaller) throws JSONException {
        // Set transfer penalty
        planner.setTransferPenalty(Arrays.asList(transferPenalty));
        // Do the planning
        Response response = planner.getItineraries();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        // Check the number of legs
        if (smaller) {
            assertTrue(itinerary.legs.size() < expectedLegs);
        }
        else {
            assertEquals(expectedLegs, itinerary.legs.size());
        }
    }
    
    public void testTripToTripTransfer() throws JSONException, ParseException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");
        
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
        
        // Now add a transfer between the two busses of minimal 126 seconds (transfer was 125 seconds)
        addTripToTripTransferTimeToTable(table, "2111", "7452", "19", "75", "190W1280", "751W1330", 126);
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The ids of the first two busses should be different
        assertFalse("190W1280".equals(itinerary.legs.get(1).tripId)
                && "751W1330".equals(itinerary.legs.get(3).tripId));
        
        // Now apply a real-time update: let the to-trip have a delay of 3 seconds
        @SuppressWarnings("deprecation")
        TableTripPattern pattern = ((PatternStopVertex) graph.getVertex("TriMet_7452_TriMet_751W1090_79_A")).getTripPattern();
        applyUpdateToTripPattern(pattern, "751W1330", "7452", 78, 41228, 41228, Update.Status.PREDICTION, 0, "20091001");
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, they should be the original again
        assertEquals("190W1280", itinerary.legs.get(1).tripId);
        assertEquals("751W1330", itinerary.legs.get(3).tripId);
        
        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "751W1330", "7452", 78, 41225, 41225, Update.Status.PREDICTION, 0, "20091001");
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testForbiddenTripToTripTransfer() throws JSONException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.5264892578125,-122.60479259490967", "45.511622,-122.645564");
        
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
        addTripToTripTransferTimeToTable(table, "2111", "7452", "19", "75", "190W1280", "751W1330"
                , StopTransfer.FORBIDDEN_TRANSFER);
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The ids of the first two busses should be different
        assertFalse("190W1280".equals(itinerary.legs.get(1).tripId)
                && "751W1330".equals(itinerary.legs.get(3).tripId));
        
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }

    public void testPreferredTripToTripTransfer() throws JSONException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.506077,-122.621139", "45.464637,-122.706061");
        
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

    public void testTimedTripToTripTransfer() throws JSONException, ParseException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.506077,-122.621139", "45.464637,-122.706061");
        
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
        
        // Now add a timed transfer between two other busses
        addTripToTripTransferTimeToTable(table, "7528", "9756", "75", "12", "750W1300", "120W1320"
                , StopTransfer.TIMED_TRANSFER);
        // Don't forget to also add a TimedTransferEdge
        @SuppressWarnings("deprecation")
        Vertex fromVertex = graph.getVertex("TriMet_7528_arrive");
        @SuppressWarnings("deprecation")
        Vertex toVertex = graph.getVertex("TriMet_9756_depart");
        TimedTransferEdge timedTransferEdge = new TimedTransferEdge(fromVertex, toVertex);
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);
        
        // Now apply a real-time update: let the to-trip be early by 240 seconds, resulting in a transfer time of 0 seconds
        @SuppressWarnings("deprecation")
        TableTripPattern pattern = ((PatternStopVertex) graph.getVertex("TriMet_9756_TriMet_120W1320_22_A")).getTripPattern();
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 21, 41580, 41580, Update.Status.PREDICTION, 0, "20091001");
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should still be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);
        
        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 21, 41820, 41820, Update.Status.PREDICTION, 0, "20091001");
        // Remove the timed transfer from the graph
        timedTransferEdge.detach();
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }
    
    public void testTimedStopToStopTransfer() throws JSONException, ParseException {
        // Plan short trip
        TestPlanner planner = new TestPlanner("portland", "45.506077,-122.621139", "45.464637,-122.706061");
        
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
        
        // Now add a timed transfer between two other busses
        addStopToStopTransferTimeToTable(table, "7528", "9756", StopTransfer.TIMED_TRANSFER);
        // Don't forget to also add a TimedTransferEdge
        @SuppressWarnings("deprecation")
        Vertex fromVertex = graph.getVertex("TriMet_7528_arrive");
        @SuppressWarnings("deprecation")
        Vertex toVertex = graph.getVertex("TriMet_9756_depart");
        TimedTransferEdge timedTransferEdge = new TimedTransferEdge(fromVertex, toVertex);
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);
        
        // Now apply a real-time update: let the to-trip be early by 240 seconds, resulting in a transfer time of 0 seconds
        @SuppressWarnings("deprecation")
        TableTripPattern pattern = ((PatternStopVertex) graph.getVertex("TriMet_9756_TriMet_120W1320_22_A")).getTripPattern();
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 21, 41580, 41580, Update.Status.PREDICTION, 0, "20091001");
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // Check the ids of the first two busses, the timed transfer should still be used
        assertEquals("750W1300", itinerary.legs.get(1).tripId);
        assertEquals("120W1320", itinerary.legs.get(3).tripId);
        
        // Now apply a real-time update: let the to-trip be early by 241 seconds, resulting in a transfer time of -1 seconds
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 21, 41579, 41579, Update.Status.PREDICTION, 0, "20091001");
        
        // Do the planning again
        response = planner.getItineraries();
        itinerary = response.getPlan().itinerary.get(0);
        // The ids of the first two busses should be different
        assertFalse("190W1280".equals(itinerary.legs.get(1).tripId)
                && "751W1330".equals(itinerary.legs.get(3).tripId));
        
        // "Revert" the real-time update
        applyUpdateToTripPattern(pattern, "120W1320", "9756", 21, 41820, 41820, Update.Status.PREDICTION, 0, "20091001");
        // Remove the timed transfer from the graph
        timedTransferEdge.detach();
        // Revert the graph, thus using the original transfer table again
        reset(graph);
    }
    
    /**
     * Add a trip-to-trip transfer time to a transfer table and check the result
     */
    private void addTripToTripTransferTimeToTable(TransferTable table, String fromStopId, String toStopId,
            String fromRouteId, String toRouteId, String fromTripId, String toTripId,
            int transferTime) {
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
    private void addStopToStopTransferTimeToTable(TransferTable table, String fromStopId, String toStopId,
            int transferTime) {
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
     * @param serviceDate is a string of format YYYYMMDD indicating the date of the update
     */
    private void applyUpdateToTripPattern(TableTripPattern pattern, String tripId, String stopId,
            int stopSeq, int arrive, int depart, Status prediction, int timestamp, String serviceDate) throws ParseException {
        Update update = new Update(new AgencyAndId("TriMet", tripId), new AgencyAndId("TriMet", stopId), stopSeq, arrive, depart, prediction, timestamp, ServiceDate.parseString(serviceDate));
        ArrayList<Update> updates = new ArrayList<Update>(Arrays.asList(update));
        TripUpdateList tripUpdateList = TripUpdateList.splitByTrip(updates).get(0);
        boolean success = pattern.update(tripUpdateList);
        assertTrue(success);
    }

    /**
     * Subclass of Planner for testing. Constructor sets fields that would usually be set by Jersey from HTTP Query string.
     */
    private static class TestPlanner extends Planner {
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
            this.routerId = routerId; // not a list because this is a path parameter not a query parameter
            this.planGenerator = Context.getInstance().planGenerator;
            this.graphService = Context.getInstance().graphService;
            this.planGenerator.graphService = Context.getInstance().graphService;
            this.prototypeRoutingRequest = new RoutingRequest();
            this.numItineraries = Arrays.asList(1); // make results more deterministic by returning only one path
        }

        public TestPlanner(String routerId, String v1, String v2, List<String> intermediates) {
            this(routerId, v1, v2);
            this.modes = Arrays.asList(new QualifiedModeSetSequence("WALK"));
            this.intermediatePlaces = intermediates;
            TravelingSalesmanPathService tsp = new TravelingSalesmanPathService();
            tsp.setChainedPathService(Context.getInstance().pathService);
            tsp.graphService = Context.getInstance().graphService;
            this.planGenerator.pathService = tsp;
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
        
        public void setMaxWalkDistance(List<Double> maxWalkDistance) {
            this.maxWalkDistance = maxWalkDistance;
        }
        
        public RoutingRequest buildRequest() throws ParameterException {
            return super.buildRequest();
        }
        
        public List<GraphPath> getPaths() {
            try {
                RoutingRequest options = this.buildRequest();
                options.intermediatePlacesOrdered = false;
                return this.planGenerator.pathService.getPaths(options);
            } catch (ParameterException e) {
                e.printStackTrace();
                return null;
            }
        }

    }

}

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
package org.opentripplanner.api.ws;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
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
import org.opentripplanner.api.ws.internals.Components;
import org.opentripplanner.api.ws.internals.GraphInternals;
import org.opentripplanner.api.ws.services.MetadataService;
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
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
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
import org.opentripplanner.routing.vertextype.IntersectionVertex;
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

}

/* This is a hack to hold context and graph data between test runs, since loading it is slow. */
class Context {
    public Graph graph = new Graph();

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

        try {
            graph.save(File.createTempFile("graph", ".obj"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pathService.setSptService(new GenericAStar());
        pathService.setGraphService(graphService);
        planGenerator.pathService = pathService;
    }

    private void initTransit() {
        GtfsGraphBuilderImpl gtfsBuilder = new GtfsGraphBuilderImpl();
        GtfsBundle bundle = new GtfsBundle();
        bundle.setPath(new File("../opentripplanner-routing/src/test/resources/google_transit.zip"));

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

    public void testLastLeg() throws Exception {

        Planner planner = new TestPlanner("portland", "45.58,-122.68", "45.48,-122.6");

        Response response = planner.getFirstTrip();
        Itinerary itinerary = response.getPlan().itinerary.get(0);
        List<Leg> legs = itinerary.legs;
        Leg lastLeg = legs.get(legs.size() - 1);

        // The end time of the last leg should equal the end time of the entire itinerary
        assertEquals(itinerary.endTime, lastLeg.endTime);

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

        long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 9, 1, 7, 50, 0) * 1000;
        long endTime = startTime + 60 * 60 * 1000;
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

    public void testComponents() {
        Components components = new Components();
        components.setGraphService(Context.getInstance().graphService);
        // GraphComponentPolygons componentPolygons = components.getComponentPolygons(
        // new TraverseModeSet(TraverseMode.WALK), "2009/10/1", "12:00:00", "", "portland");
        // assertTrue(componentPolygons.components.size() >= 1);
    }

    public void testGraphInternals() {
        GraphInternals internals = new GraphInternals();
        internals.setGraphService(Context.getInstance().graphService);
        FeatureCount counts = internals.countVertices("45.5,-122.6", "45.6,-122.5", "portland");
        assertTrue(counts.vertices > 0);
        assertTrue(counts.edges > 0);

        WithGraph obj = (WithGraph) internals.getEdges("45.5,-122.6", "45.55,-122.55", "", false,
                false, true, "portland");
        EdgeSet edges = (EdgeSet) obj.getObject();
        assertTrue(edges.edges.size() > 0);

        obj = (WithGraph) internals.getVertices("45.5,-122.6", "45.55,-122.55", false, "", false,
                false, "portland");
        VertexSet vertices = (VertexSet) obj.getObject();
        assertTrue(vertices.vertices.size() > 0);

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
    
    public void testBannedStopGroup() throws JSONException, ParameterException {
        // Create StopMatcher instance
        StopMatcher stopMatcher = StopMatcher.parse("TriMet_2106,TriMet_65-tc");
        // Find stops in graph
        Stop stop65_tc = null;
        Stop stop12921 = null;
        Stop stop13132 = null;
        Stop stop2106 = null;
        Stop stop2107 = null;
        {
            Graph graph = Context.getInstance().graph;
            for (Vertex v : graph.getVertices()) {
               if (v instanceof TransitStop) {
                   Stop stop = ((TransitStop)v).getStop();
                   if (stop.getId().getAgencyId().equals("TriMet")) {
                       if (stop.getId().getId().equals("65-tc")) {
                           stop65_tc = stop;
                       }
                       else if (stop.getId().getId().equals("12921")) {
                           stop12921 = stop;
                       }
                       else if (stop.getId().getId().equals("13132")) {
                           stop13132 = stop;
                       }
                       else if (stop.getId().getId().equals("2106")) {
                           stop2106 = stop;
                       }
                       else if (stop.getId().getId().equals("2107")) {
                           stop2107 = stop;
                       }
                   }
               }
            }
        }
        // All stops should be found
        assertNotNull(stop65_tc);
        assertNotNull(stop12921);
        assertNotNull(stop13132);
        assertNotNull(stop2106);
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
            this.walkSpeed = Arrays.asList(1.33);
            this.optimize = Arrays.asList(OptimizeType.QUICK);
            this.modes = Arrays.asList(new TraverseModeSet("WALK,TRANSIT"));
            this.numItineraries = Arrays.asList(1);
            this.transferPenalty = Arrays.asList(0);
            this.maxTransfers = Arrays.asList(2);
            this.routerId = Arrays.asList(routerId);
            this.planGenerator = Context.getInstance().planGenerator;
            this.graphService = Context.getInstance().graphService;
            this.planGenerator.graphService = Context.getInstance().graphService;
            this.prototypeRoutingRequest = new RoutingRequest();
        }

        public TestPlanner(String routerId, String v1, String v2, List<String> intermediates) {
            this(routerId, v1, v2);
            this.modes = Arrays.asList(new TraverseModeSet("WALK"));
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

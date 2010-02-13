package org.opentripplanner.api.ws;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.ws.RequestInf;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.impl.PathServiceImpl;
import org.opentripplanner.routing.impl.RoutingServiceImpl;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.util.DateUtils;

import junit.framework.TestCase;

class DataHolder {
    public Graph graph = null;
    public Planner planner = null;
    public PathServiceImpl pathService = null;
    
    private static DataHolder instance = null;
    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }
        return instance;
    }
}

public class TestRequest extends TestCase {

    private PathServiceImpl pathService;
    private Graph graph;
    private Planner planner;
    
    public void testRequest() {
        RequestInf request = new Request();
        
        request.addMode(TraverseMode.CAR);
        assertTrue(request.getModes().getCar());
        request.removeMode(TraverseMode.CAR);
        assertFalse(request.getModes().getCar());
     
        request.setModes(new TraverseModeSet("BICYCLE,WALK"));
        assertFalse(request.getModes().getCar());
        assertTrue(request.getModes().getBicycle());
        assertTrue(request.getModes().getWalk());
    }

    public void setUp() {
        DataHolder holder = DataHolder.getInstance();
        graph = holder.graph;
        planner = holder.planner;
        pathService = holder.pathService;
        if (graph != null) {
            return;
        }
        planner = new Planner();
        pathService = new PathServiceImpl();
        graph = new Graph();
        ShapefileStreetGraphBuilderImpl builder = new ShapefileStreetGraphBuilderImpl();
        FeatureSourceFactory factory = new ShapefileFeatureSourceFactoryImpl(new File("src/test/resources/portland/Streets_pdx.shp"));
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

        builder.setSchema(schema );
        builder.buildGraph(graph);
        pathService.setGraph(graph);
        RoutingServiceImpl routingService = new RoutingServiceImpl();
        routingService.setGraph(graph);
        pathService.setRoutingService(routingService);
        planner.setPathService(pathService);

        holder.graph = graph;
        holder.planner = planner;
        holder.pathService = pathService;
    }
    
    private Vertex getVertexByCrossStreets(String s1, String s2) {
        for (Vertex v : graph.getVertices()) {
            if (v.getName().contains(s1) && v.getName().contains(s2)) {
                return v;
            }
        }
        return null;
    }

    public void testPlanner() throws Exception {
        
        Vertex v1 = getVertexByCrossStreets("NE 43RD AVE", "NE FLANDERS ST");
        Vertex v2 = getVertexByCrossStreets("NE 43RD AVE", "NE ROYAL CT");
        assertNotNull(v1);
        assertNotNull(v2);
        
        Response response = planner.getItineraries(
                v1.getLabel(),
                v2.getLabel(),
                new ArrayList<String>(),
                "2009-01-01", 
                "11:11:11",
                false,
                false,
                840.0,
                1.33,
                OptimizeType.QUICK,
                new TraverseModeSet("WALK"),
                1);
        
        Itinerary itinerary = response.plan.itinerary.get(0);
        Leg leg = itinerary.leg.get(0);
        List<WalkStep> steps = leg.walkSteps;
        assertEquals(3, steps.size());
        WalkStep step0 = steps.get(0);
        WalkStep step1 = steps.get(1);
        WalkStep step2 = steps.get(2);
        assertEquals(AbsoluteDirection.NORTH, step0.absoluteDirection);
        assertEquals("NE 43RD AVE", step0.streetName);
        
        assertEquals("NE 43RD AVE", step1.streetName);
        assertEquals(RelativeDirection.SLIGHTLY_RIGHT, step1.relativeDirection);
        assertTrue(step1.stayOn);
        
        assertEquals("NE 43RD AVE", step2.streetName);
        assertEquals(RelativeDirection.LEFT, step2.relativeDirection);
        assertTrue(step2.stayOn);
    }
    

    public void testIntermediate() throws Exception {
        
        Vertex v1 = getVertexByCrossStreets("NW 10TH AVE", "W BURNSIDE ST");
        Vertex v2 = getVertexByCrossStreets("NE 21ST AVE", "NE MASON ST");
        Vertex v3 = getVertexByCrossStreets("SE 82ND AVE", "SE ASH ST");
        Vertex v4 = getVertexByCrossStreets("SE 92ND AVE", "SE FLAVEL ST");
        Vertex[] vertices = {v1, v2, v3, v4};
        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);
        assertNotNull(v4);
        
        ArrayList<String> intermediates = new ArrayList<String>();
        intermediates.add(v3.getLabel());
        intermediates.add(v2.getLabel());
        Date dateTime = DateUtils.toDate("2009-01-01", "10:00:00");
        TraverseOptions options = new TraverseOptions();
        List<GraphPath> paths = pathService.plan(v1.getLabel(), v4.getLabel(),
                intermediates, dateTime, options);
        
        assertTrue(paths.size() > 0);
        GraphPath path = paths.get(0);
        int curVertex = 0;
        for (SPTVertex v: path.vertices) {
            if (v.mirror.equals(vertices[curVertex])) {
                curVertex += 1;
            }
        }
        assertEquals(4, curVertex); //found all four, in the correct order
    }
}

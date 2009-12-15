package org.opentripplanner.api.ws;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.ws.RequestInf;
import org.opentripplanner.api.ws.RequestInf.OptimizeType;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.impl.PathServiceImpl;
import org.opentripplanner.routing.impl.RoutingServiceImpl;

import junit.framework.TestCase;

public class TestRequest extends TestCase {

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
    
    public void testPlanner() throws Exception {
        Planner planner = new Planner();
        PathServiceImpl pathService = new PathServiceImpl();
        Graph graph = new Graph();
        ShapefileStreetGraphBuilderImpl builder = new ShapefileStreetGraphBuilderImpl();
        FeatureSourceFactory factory = new ShapefileFeatureSourceFactoryImpl(new File("src/test/resources/portland/Streets_pdx.shp"));
        builder.setFeatureSourceFactory(factory);
        ShapefileStreetSchema schema = new ShapefileStreetSchema();
        schema.setIdAttribute("LOCALID");
        schema.setNameAttribute("FULL_NAME");

        CaseBasedTraversalPermissionConverter perms = new CaseBasedTraversalPermissionConverter(
                "DIRECTION", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE_ONLY);

        perms.addPermission("2", StreetTraversalPermission.ALL,
                StreetTraversalPermission.PEDESTRIAN_ONLY);
        perms.addPermission("3", StreetTraversalPermission.PEDESTRIAN_ONLY,
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
        
        Vertex v1 = graph.getVertex("NE 43RD AVE at NE FLANDERS ST");
        Vertex v2 = graph.getVertex("NE 43RD AVE at NE ROYAL CT");
        assertNotNull(v1);
        assertNotNull(v2);
        
        Response response = planner.getItineraries(
                v1.getLabel(),
                v2.getLabel(),
                "2009-01-01", 
                "11:11:11",
                false,
                840.0,
                1.33,
                new ArrayList<OptimizeType>(),
                new TraverseModeSet("WALK"),
                1,
                MediaType.APPLICATION_JSON);
        
        List<WalkStep> steps = response.plan.itinerary.get(0).leg.get(0).walkSteps;
        assertEquals(3, steps.size());
        WalkStep step0 = steps.get(0);
        WalkStep step1 = steps.get(1);
        WalkStep step2 = steps.get(2);
        assertEquals(AbsoluteDirection.NORTH, step0.absoluteDirection);
        assertEquals("NE 43RD AVE", step0.streetName);
        
        assertEquals("NE 43RD AVE", step1.streetName);
        assertEquals(RelativeDirection.RIGHT, step1.relativeDirection);
        assertTrue(step1.stayOn);
        
        assertEquals("NE 43RD AVE", step2.streetName);
        assertEquals(RelativeDirection.LEFT, step2.relativeDirection);
        assertTrue(step2.stayOn);
    }   
}

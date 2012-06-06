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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.ws.PlanGenerator;
import org.opentripplanner.api.ws.Planner;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.graph_builder.impl.shapefile.AttributeFeatureConverter;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.GraphServiceBeanImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.impl.TravelingSalesmanPathService;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.GraphPath;

/* This is a hack to hold context and graph data between test runs, since loading it is slow. */
class Context {
    public Graph graph = new Graph();
    public GraphService graphService = new GraphServiceBeanImpl(graph); 
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

        //as a test, use prefixes ("NE", SE", etc) as an alert
        schema.setNoteConverter(new AttributeFeatureConverter<String> ("PREFIX"));

        builder.setSchema(schema);
        builder.buildGraph(graph, new HashMap<Class<?>, Object>());
        graph.streetIndex = new StreetVertexIndexServiceImpl(graph);
        
        pathService.sptService = new GenericAStar();
        pathService.graphService = graphService;
        planGenerator.pathService = pathService;
    }
}


public class TestRequest extends TestCase {

    public void testRequest() {
        RoutingRequest request = new RoutingRequest();
        
        request.addMode(TraverseMode.CAR);
        assertTrue(request.getModes().getCar());
        request.removeMode(TraverseMode.CAR);
        assertFalse(request.getModes().getCar());
     
        request.setModes(new TraverseModeSet("BICYCLE,WALK"));
        assertFalse(request.getModes().getCar());
        assertTrue(request.getModes().getBicycle());
        assertTrue(request.getModes().getWalk());
        assertTrue("intentional failure", false);
    }

    public void testPlanner() throws Exception {
        
        Planner planner = new TestPlanner("113410", "137427");
        
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

    public void testAlerts() throws Exception {

	//SE 47th and Ash, NE 47th and Davis (note that we cross Burnside, this goes from SE to NE)
	Planner planner = new TestPlanner("114789 back", "114237");
	Response response = planner.getItineraries();

        Itinerary itinerary = response.getPlan().itinerary.get(0);
        Leg leg = itinerary.legs.get(0);
        List<WalkStep> steps = leg.walkSteps;
        assertEquals(2, steps.size());
        WalkStep step0 = steps.get(0);
        WalkStep step1 = steps.get(1);

        assertNotNull(step0.alerts);
		assertEquals(1, step0.alerts.size());
		assertEquals("SE",
				step0.alerts.get(0).alertHeaderText.getSomeTranslation());

		assertEquals(1, step1.alerts.size());
		assertEquals("NE",
				step1.alerts.get(0).alertHeaderText.getSomeTranslation());
	}

    public void testIntermediate() throws Exception {
        
        Graph graph = Context.getInstance().graph;
        Vertex v1 = graph.getVertex("114080 back");//getVertexByCrossStreets("NW 10TH AVE", "W BURNSIDE ST", false);
        Vertex v2 = graph.getVertex("115250");//graph.getOutgoing(getVertexByCrossStreets("SE 82ND AVE", "SE ASH ST", false)).iterator().next().getToVertex();
        Vertex v3 = graph.getVertex("108406");//graph.getOutgoing(getVertexByCrossStreets("NE 21ST AVE", "NE MASON ST", false)).iterator().next().getToVertex();
        Vertex v4 = graph.getVertex("192532");//getVertexByCrossStreets("SE 92ND AVE", "SE FLAVEL ST", true);
        Vertex[] vertices = {v1, v3, v2, v4};
        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);
        assertNotNull(v4);
        
        TestPlanner planner = new TestPlanner(v1.getLabel(), v4.getLabel(), Arrays.asList(v3.getLabel(), v2.getLabel()));
        List<GraphPath> paths = planner.getPaths();
        
        assertTrue(paths.size() > 0);
        GraphPath path = paths.get(0);
        int curVertex = 0;
        for (State s: path.states) {
            if (s.getVertex().equals(vertices[curVertex])) {
                curVertex += 1;
            }
        }
        assertEquals(4, curVertex); //found all four, in the correct order (1, 3, 2, 4)
    }
    
    /**
     * Subclass of Planner for testing. Constructor sets fields that would usually be set by 
     * Jersey from HTTP Query string.
     */
    private static class TestPlanner extends Planner {
        public TestPlanner(String v1, String v2) {
            super();
            this.fromPlace = Arrays.asList(v1);
            this.toPlace = Arrays.asList(v2);
            this.date = Arrays.asList("2009-01-01");
            this.time = Arrays.asList("11:11:11");
            this.maxWalkDistance = Arrays.asList(840.0);
            this.walkSpeed = Arrays.asList(1.33);
            this.optimize = Arrays.asList(OptimizeType.QUICK);
            this.modes = Arrays.asList(new TraverseModeSet("WALK"));
            this.numItineraries = Arrays.asList(1);
            this.transferPenalty = Arrays.asList(0);
            this.maxTransfers = Arrays.asList(2);
            
            this.planGenerator = Context.getInstance().planGenerator;
        }

        public TestPlanner(String v1, String v2, List<String> intermediates) {
            this(v1, v2);
            this.intermediatePlaces = intermediates;
            TravelingSalesmanPathService tsp = new TravelingSalesmanPathService();
            tsp.chainedPathService = Context.getInstance().pathService;
            tsp.graphService = Context.getInstance().graphService;
            this.planGenerator.pathService = tsp;
        }
        
        public List<GraphPath> getPaths() {
            try {
                return this.planGenerator.pathService.getPaths(this.buildRequest());
            } catch (ParameterException e) {
                e.printStackTrace();
                return null;
            }
        }
        
    }
    
}

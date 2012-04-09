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

package org.opentripplanner.api.ws;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.api.ws.RequestInf;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.graph_builder.impl.shapefile.AttributeFeatureConverter;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.ContractionRoutingServiceImpl;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.SingletonPathServiceFactoryImpl;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.util.DateUtils;

import junit.framework.TestCase;

/**
 * This is a hack to hold graph data between test runs, since loading it takes a long time.
 * 
 */
class DataHolder {
    public Graph graph = null;
    public Planner planner = null;
    public RetryingPathServiceImpl pathService = null;
    
    private static DataHolder instance = null;
    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }
        return instance;
    }
}

public class TestRequest extends TestCase {

    private RetryingPathServiceImpl pathService;
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
        pathService = new RetryingPathServiceImpl();
        graph = new Graph();
        pathService.setRemainingWeightHeuristicFactory(
                new DefaultRemainingWeightHeuristicFactoryImpl());
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

        builder.setSchema(schema );
        builder.buildGraph(graph, new HashMap<Class<?>, Object>());
        GraphServiceImpl graphService = new GraphServiceImpl();
        graphService.setGraph(graph);
        
        pathService.setGraphService(graphService);
        ContractionRoutingServiceImpl routingService = new ContractionRoutingServiceImpl();
        routingService.setGraphService(graphService);
        pathService.setRoutingService(routingService);
        SingletonPathServiceFactoryImpl pathServiceFactory = new SingletonPathServiceFactoryImpl();
        pathServiceFactory.setPathService(pathService);
        planner.setPathServiceFactory(pathServiceFactory);

        holder.graph = graph;
        holder.planner = planner;
        holder.pathService = pathService;
    }

    public void testPlanner() throws Exception {
        
        Vertex v1 = graph.getVertex("113410");//getVertexByCrossStreets("NE 43RD AVE", "NE FLANDERS ST", false);
        Vertex v2 = graph.getVertex("137427");//getVertexByCrossStreets("NE 43RD AVE", "NE ROYAL CT", true);
        assertNotNull(v1);
        assertNotNull(v2);
        
        Response response = planner.getItineraries(
                v1.getLabel(),
                v2.getLabel(),
                null,
                null,
                "2009-01-01", 
                "11:11:11",
                null,
                false,
                false,
                840.0,
                1.33,
                null,
                null,
                null,
                OptimizeType.QUICK,
                new TraverseModeSet("WALK"),
                1,
                null, false,
                "", "", "", 0, 2);
        
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
		Vertex v1 = graph.getVertex("114789 back");//SE 47th and Ash
		Vertex v2 = graph.getVertex("114237");// NE 47th and Davis note that we cross Burnside this go from SE to NE
		assertNotNull(v1);
		assertNotNull(v2);

		Response response = planner.getItineraries(v1.getLabel(),
				v2.getLabel(), null, null, "2009-01-01", "11:11:11", null, false,
				false, 840.0, 1.33, null, null, null, OptimizeType.QUICK,
				new TraverseModeSet("WALK"), 1, null, false, "", "", "", 0, 2);

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
        
        Vertex v1 = graph.getVertex("114080 back");//getVertexByCrossStreets("NW 10TH AVE", "W BURNSIDE ST", false);
        Vertex v2 = graph.getVertex("115250");//graph.getOutgoing(getVertexByCrossStreets("SE 82ND AVE", "SE ASH ST", false)).iterator().next().getToVertex();
        Vertex v3 = graph.getVertex("108406");//graph.getOutgoing(getVertexByCrossStreets("NE 21ST AVE", "NE MASON ST", false)).iterator().next().getToVertex();
        Vertex v4 = graph.getVertex("192532");//getVertexByCrossStreets("SE 92ND AVE", "SE FLAVEL ST", true);
        Vertex[] vertices = {v1, v3, v2, v4};
        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);
        assertNotNull(v4);
        
        ArrayList<NamedPlace> intermediates = new ArrayList<NamedPlace>();
        intermediates.add(new NamedPlace(v3.getLabel()));
        intermediates.add(new NamedPlace(v2.getLabel()));
        Date dateTime = DateUtils.toDate("2009-01-01", "10:00:00");
        TraverseOptions options = new TraverseOptions(new TraverseModeSet(TraverseMode.WALK));
        List<GraphPath> paths = pathService.plan(new NamedPlace(v1.getLabel()), new NamedPlace(v4.getLabel()),
                intermediates, false, dateTime, options);
        
        assertTrue(paths.size() > 0);
        GraphPath path = paths.get(0);
        int curVertex = 0;
        for (State s: path.states) {
            if (s.getVertex().equals(vertices[curVertex])) {
                curVertex += 1;
            }
        }
        assertEquals(4, curVertex); //found all four, in the correct order
    }
}

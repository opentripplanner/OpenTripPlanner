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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.impl.shapefile.AttributeFeatureConverter;
import org.opentripplanner.graph_builder.impl.shapefile.CaseBasedTraversalPermissionConverter;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileFeatureSourceFactoryImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetGraphBuilderImpl;
import org.opentripplanner.graph_builder.impl.shapefile.ShapefileStreetSchema;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
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
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.LineString;

class SimpleGraphServiceImpl implements GraphService {

    private HashMap<String, Graph> graphs = new HashMap<String, Graph>();

    @Override
    public void setLoadLevel(LoadLevel level) {      
    }

    @Override
    public void refreshGraphs() {
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
    public Collection<String> getGraphIds() {
        return graphs.keySet();
    }
    
    public void putGraph(String graphId, Graph graph) {
        graphs.put(graphId, graph);
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
        graphService.putGraph(null, makeSimpleGraph()); //default graph is tiny test graph
        graphService.putGraph("portland", graph);
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
        
        initBikeRental();

        pathService.sptService = new GenericAStar();
        pathService.graphService = graphService;
        planGenerator.pathService = pathService;
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
        double length = DistanceLibrary.distance(v1.getCoordinate(), v2.getCoordinate());
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

        request.setModes(new TraverseModeSet("BICYCLE,WALK"));
        assertFalse(request.getModes().getCar());
        assertTrue(request.getModes().getBicycle());
        assertTrue(request.getModes().getWalk());
    }

    public void testPlanner() throws Exception {

        Planner planner = new TestPlanner("portland", "113410", "137427");

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
	Planner planner = new TestPlanner("portland", "114789 back", "114237");
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

        TestPlanner planner = new TestPlanner("portland", v1.getLabel(), v4.getLabel(), Arrays.asList(v2.getLabel(), v3.getLabel()));
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

    public void testBikeRental() {
        BikeRental bikeRental = new BikeRental();
        bikeRental.setGraphService(Context.getInstance().graphService);
        //no stations in graph
        BikeRentalStationList stations = bikeRental.getBikeRentalStations(null,
                null, null);
        assertEquals(0, stations.stations.size());

        //no stations in range
        stations = bikeRental.getBikeRentalStations("55.5,-122.7",
                "65.6,-122.6", "portland");
        assertEquals(0, stations.stations.size());
        //finally, a station
        stations = bikeRental.getBikeRentalStations("45.5,-122.7",
                "45.6,-122.6", "portland");
        assertEquals(1, stations.stations.size());
    }

    public void testMetadata() throws JSONException {
        Metadata metadata = new Metadata();
        metadata.graphService = Context.getInstance().graphService;
        GraphMetadata data1 = metadata.getMetadata(null);
        assertTrue("centerLatitude is not 40.005; got " + data1.getCenterLatitude(), 
                Math.abs(40.005 - data1.getCenterLatitude()) < 0.000001);
        
        GraphMetadata data2 = metadata.getMetadata("portland");
        assertTrue(Math.abs(-122 - data2.getCenterLongitude()) < 1);
        assertTrue(Math.abs(-122 - data2.getLowerLeftLongitude()) < 2);
        assertTrue(Math.abs(-122 - data2.getUpperRightLongitude()) < 2);

    }

    /**
     * Subclass of Planner for testing. Constructor sets fields that would usually be set by 
     * Jersey from HTTP Query string.
     */
    private static class TestPlanner extends Planner {
        public TestPlanner(String routerId, String v1, String v2) {
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
            this.routerId = Arrays.asList(routerId);
            this.planGenerator = Context.getInstance().planGenerator;
        }

        public TestPlanner(String routerId, String v1, String v2, List<String> intermediates) {
            this(routerId, v1, v2);
            this.intermediatePlaces = intermediates;
            TravelingSalesmanPathService tsp = new TravelingSalesmanPathService();
            tsp.setChainedPathService(Context.getInstance().pathService);
            tsp.graphService = Context.getInstance().graphService;
            this.planGenerator.pathService = tsp;
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

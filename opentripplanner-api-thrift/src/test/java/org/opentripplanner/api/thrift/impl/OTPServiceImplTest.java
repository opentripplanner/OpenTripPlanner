/* This program is free software: you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation, either version 3 of
the License, or (props, at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.thrift.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.api.thrift.definition.BulkPathsRequest;
import org.opentripplanner.api.thrift.definition.BulkPathsResponse;
import org.opentripplanner.api.thrift.definition.FindNearestVertexRequest;
import org.opentripplanner.api.thrift.definition.FindNearestVertexResponse;
import org.opentripplanner.api.thrift.definition.FindPathsRequest;
import org.opentripplanner.api.thrift.definition.FindPathsResponse;
import org.opentripplanner.api.thrift.definition.GraphEdge;
import org.opentripplanner.api.thrift.definition.GraphEdgesRequest;
import org.opentripplanner.api.thrift.definition.GraphEdgesResponse;
import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.GraphVerticesRequest;
import org.opentripplanner.api.thrift.definition.GraphVerticesResponse;
import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.api.thrift.definition.Path;
import org.opentripplanner.api.thrift.definition.PathOptions;
import org.opentripplanner.api.thrift.definition.TravelMode;
import org.opentripplanner.api.thrift.definition.TravelState;
import org.opentripplanner.api.thrift.definition.TripParameters;
import org.opentripplanner.api.thrift.definition.TripPaths;
import org.opentripplanner.api.thrift.definition.VertexQuery;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.impl.osm.wayProperties.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.SimplifiedPathServiceImpl;

import com.vividsolutions.jts.geom.Coordinate;

public class OTPServiceImplTest {

    private static HashMap<Class<?>, Object> extra;

    private static Graph graph;

    private Random rand;

    private OTPServiceImpl serviceImpl;

    @BeforeClass
    public static void beforeClass() {
        extra = new HashMap<Class<?>, Object>();
        graph = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(OTPServiceImplTest.class.getResource("NYC_small.osm.gz").getFile());

        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(graph, extra);

        // Need to set up the index because buildGraph doesn't do it.
        graph.rebuildVertexAndEdgeIndices();
        graph.streetIndex = (new DefaultStreetVertexIndexFactory()).newIndex(graph);
    }

    @Before
    public void before() {
        rand = new Random(1234);

        GraphServiceImpl graphService = new GraphServiceImpl();
        graphService.registerGraph("", graph);
        SimplifiedPathServiceImpl pathService = new SimplifiedPathServiceImpl();
        pathService.setGraphService(graphService);
        pathService.setSptService(new GenericAStar());

        serviceImpl = new OTPServiceImpl();
        serviceImpl.setGraphService(graphService);
        serviceImpl.setPathService(pathService);
    }

    @Test
    public void testGetGraphEdges() throws TException {
        GraphEdgesRequest req = new GraphEdgesRequest();
        req.validate();

        req.setStreet_edges_only(true);
        GraphEdgesResponse res = serviceImpl.GetEdges(req);

        Set<Integer> expectedEdgeIds = new HashSet<Integer>();
        for (Edge e : graph.getStreetEdges()) {
            expectedEdgeIds.add(e.getId());
        }

        Set<Integer> actualEdgeIds = new HashSet<Integer>(res.getEdgesSize());
        for (GraphEdge ge : res.getEdges()) {
            actualEdgeIds.add(ge.getId());
        }

        assertTrue(expectedEdgeIds.equals(actualEdgeIds));
    }

    @Test
    public void testGetGraphVertices() throws TException {
        GraphVerticesRequest req = new GraphVerticesRequest();
        req.validate();

        GraphVerticesResponse res = serviceImpl.GetVertices(req);

        Set<Integer> expectedVertexIds = new HashSet<Integer>();
        for (Vertex v : graph.getVertices()) {
            expectedVertexIds.add(v.getIndex());
        }

        Set<Integer> actualVertexIds = new HashSet<Integer>(res.getVerticesSize());
        for (GraphVertex gv : res.getVertices()) {
            actualVertexIds.add(gv.getId());
        }

        assertTrue(expectedVertexIds.equals(actualVertexIds));
    }

    private Location getLocationForVertex(Vertex v) {
        Location loc = new Location();
        Coordinate c = v.getCoordinate();
        loc.setLat_lng(new LatLng(c.y, c.x));
        return loc;
    }
    
    private Location getLocationForTravelState(TravelState ts) {
        Location loc = new Location();
        loc.setLat_lng(ts.getVertex().getLat_lng());
        return loc;
    }

    private P2<Location> pickOriginAndDest() {
        List<Vertex> vList = new ArrayList<Vertex>(graph.getVertices());
        Collections.shuffle(vList, rand);

        P2<Location> pair = new P2<Location>(getLocationForVertex(vList.get(0)),
                getLocationForVertex(vList.get(1)));
        return pair;
    }

    private void checkPath(Path p) {
        int duration = p.getDuration();
        int nStates = p.getStatesSize();
        long startTime = p.getStates().get(0).getArrival_time();
        long endTime = p.getStates().get(nStates - 1).getArrival_time();
        int computedDuration = (int) (endTime - startTime);

        assertEquals(duration, computedDuration);
    }
    
    @Test
    public void testFindPaths() throws TException {
        PathOptions opts = new PathOptions();
        opts.setNum_paths(1);
        opts.setReturn_detailed_path(true);

        TripParameters trip = new TripParameters();
        trip.addToAllowed_modes(TravelMode.CAR);

        P2<Location> pair = pickOriginAndDest();
        trip.setOrigin(pair.getFirst());
        trip.setDestination(pair.getSecond());

        FindPathsRequest req = new FindPathsRequest();
        req.setOptions(opts);
        req.setTrip(trip);
        req.validate();

        FindPathsResponse res = serviceImpl.FindPaths(req);
        TripPaths paths = res.getPaths();
        assertEquals(1, paths.getPathsSize());
        Path p = paths.getPaths().get(0);
        checkPath(p);
        
        // Check what happens when we decompose this path into subpaths.
        int expectedTotalDuration = p.getDuration();
        int subPathDurations = 0;
        for (int i = 1; i < p.getStatesSize(); ++i) {
            TravelState firstState = p.getStates().get(i - 1);
            TravelState secondState = p.getStates().get(i);
            
            Location startLoc = getLocationForTravelState(firstState);
            Location endLoc = getLocationForTravelState(secondState);
            
            trip.setOrigin(startLoc);
            trip.setDestination(endLoc);
            
            req = new FindPathsRequest();
            req.setOptions(opts);
            req.setTrip(trip);
            req.validate();
            
            res = serviceImpl.FindPaths(req);
            paths = res.getPaths();
            assertEquals(1, paths.getPathsSize());
            Path subPath = paths.getPaths().get(0);
            checkPath(subPath);
            
            subPathDurations += subPath.getDuration();
        }
        
        // Subpaths may take less time because they need not start on the
        // the same edges as the original path.
        assertTrue(subPathDurations <= expectedTotalDuration); 
    }

    @Test
    public void testBulkFindPaths() throws TException {
        PathOptions opts = new PathOptions();
        opts.setNum_paths(1);
        opts.setReturn_detailed_path(true);

        BulkPathsRequest req = new BulkPathsRequest();
        req.setOptions(opts);

        for (int i = 0; i < 4; ++i) {
            TripParameters trip = new TripParameters();
            trip.addToAllowed_modes(TravelMode.CAR);

            P2<Location> pair = pickOriginAndDest();
            trip.setOrigin(pair.getFirst());
            trip.setDestination(pair.getSecond());
            req.addToTrips(trip);
        }

        req.validate();

        BulkPathsResponse res = serviceImpl.BulkFindPaths(req);

        for (TripPaths paths : res.getPaths()) {
            assertEquals(1, paths.getPathsSize());
            Path p = paths.getPaths().get(0);
            checkPath(p);
        }
    }
    
    @Test
    public void testFindNearestVertex() throws TException {
        for (Vertex v : graph.getVertices()) {
            FindNearestVertexRequest req = new FindNearestVertexRequest();
            VertexQuery q = new VertexQuery();
            q.setLocation(getLocationForVertex(v));
            req.setQuery(q);
            
            FindNearestVertexResponse res = serviceImpl.FindNearestVertex(req);
            
            GraphVertex gv = res.getResult().getNearest_vertex();
            int expectedId = v.getIndex();
            int actualId = gv.getId();
            
            if (expectedId != actualId) {
                // If not equal, then the distance should be approaching 0.
                LatLng ll = gv.getLat_lng();
                Coordinate outCoord = new Coordinate(ll.getLng(), ll.getLat());
                assertTrue(v.getCoordinate().distance(outCoord) < 0.00001);
            }            
        }
    }
    
}

package org.opentripplanner.routing.test;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;


public class TestHalfEdges extends TestCase {

    public void testHalfEdges() {
        Graph graph = new Graph();
        //a 1 degree x 1 degree box
        Vertex tl = new Vertex("tl", -74, 41);
        Vertex tr = new Vertex("tr", -73, 41);
        Vertex bl = new Vertex("bl", -74, 40);
        Vertex br = new Vertex("bl", -73, 40);

        double td = GtfsLibrary.distance(tl.getCoordinate().y, tl.getCoordinate().x, tr.getCoordinate().y, tr.getCoordinate().x);
        graph.addEdge(tl, tr, new Street(td));
        double bd = GtfsLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, br.getCoordinate().y, br.getCoordinate().x);
        graph.addEdge(bl, br, new Street(bd));

        double d = GtfsLibrary.distance(bl.getCoordinate().y, bl.getCoordinate().x, tl.getCoordinate().y, tl.getCoordinate().x);
        graph.addEdge(tl, bl, new Street(d));
        Edge startEdge = graph.addEdge(bl, tl, new Street(d));
        
        graph.addEdge(tr, br, new Street(d));
        Edge endEdge = graph.addEdge(br, tr, new Street(d));
        
        StreetLocation start = new StreetLocation("start", startEdge, 0.6, false);
        StreetLocation end = new StreetLocation("end", endEdge, 0.8, true);
        
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, new State(
                    startTime.getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(end.vertex);
        assertNotNull(path);
        
    }
    
}

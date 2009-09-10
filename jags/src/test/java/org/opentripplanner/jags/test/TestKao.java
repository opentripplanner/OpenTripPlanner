package org.opentripplanner.jags.test;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.opentripplanner.jags.algorithm.kao.Kao;
import org.opentripplanner.jags.algorithm.kao.KaoGraph;
import org.opentripplanner.jags.algorithm.kao.Tree;
import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.spt.ShortestPathTree;


import junit.framework.TestCase;

public class TestKao extends TestCase {
	public void testBasic() throws Exception {
		Feed feed = new Feed( "caltrain_gtfs.zip" );
		KaoGraph kg = new KaoGraph( );
		GTFSHopLoader hl = new GTFSHopLoader(kg, feed);
		hl.load();
		
		GregorianCalendar t_0 = new GregorianCalendar(2009,8,7,12,0,0);
		long delta = 1000000000;
		Vertex mlb = kg.getVertex("Millbrae Caltrain");
		Vertex mtv = kg.getVertex("Mountain View Caltrain" );
		
		Tree tree = Kao.find(kg, t_0, mlb, delta);
		ArrayList<Edge> path = tree.path(mtv);
		
		assertTrue( ((Hop)path.get(path.size()-1).payload).end.arrival_time.getSecondsSinceMidnight()==48540 );
		
		Graph gg = new Graph();
		GTFSHopLoader h2 = new GTFSHopLoader(gg,feed);
		h2.load();
		ShortestPathTree spt = org.opentripplanner.jags.algorithm.Dijkstra.getShortestPathTree(gg, 
											   "Millbrae Caltrain", 
											   "Mountain View Caltrain", 
											   new State(t_0), 
											   new WalkOptions());
		assertTrue( ((Hop)spt.getPath(gg.getVertex("Mountain View Caltrain")).vertices.lastElement().incoming.payload).end.arrival_time.getSecondsSinceMidnight()==48540 );
		
	}
}

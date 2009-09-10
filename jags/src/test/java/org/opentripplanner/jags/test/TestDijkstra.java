package org.opentripplanner.jags.test;

import java.util.GregorianCalendar;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.spt.ShortestPathTree;

import junit.framework.TestCase;

public class TestDijkstra extends TestCase {
	public void testBasic() throws Exception {
		Feed feed = new Feed( "caltrain_gtfs.zip" );
		Graph gg = new Graph();
		GTFSHopLoader hl = new GTFSHopLoader(gg,feed);
		hl.load();
		
		ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, 
				   "Millbrae Caltrain", 
				   "Mountain View Caltrain", 
				   new State(new GregorianCalendar(2009,8,7,12,0,0)), 
				   new WalkOptions());
		assertTrue( ((Hop)spt.getPath(gg.getVertex("Mountain View Caltrain")).vertices.lastElement().incoming.payload).end.arrival_time.getSecondsSinceMidnight()==48540 );
	}
}

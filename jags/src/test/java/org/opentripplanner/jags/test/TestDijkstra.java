package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.spt.ShortestPathTree;

import java.io.File;
import java.util.GregorianCalendar;

public class TestDijkstra extends TestCase {
	public void testBasic() throws Exception {
	  
	  GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.CALTRAIN_GTFS));

    WalkOptions options = new WalkOptions();
    options.setGtfsContext(context);
    
		Graph gg = new Graph();
		GTFSHopLoader hl = new GTFSHopLoader(gg,context);
		hl.load();
		
		ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, 
				   "Caltrain_Millbrae Caltrain", 
				   "Caltrain_Mountain View Caltrain", 
				   new State(new GregorianCalendar(2009,8,7,12,0,0)), 
				   options);
		
		assertTrue( ((Hop)spt.getPath(gg.getVertex("Caltrain_Mountain View Caltrain")).vertices.lastElement().incoming.payload).getEndStopTime().getArrivalTime()==48540 );
	}
}

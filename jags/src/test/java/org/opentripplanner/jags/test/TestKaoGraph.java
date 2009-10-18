package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.opentripplanner.jags.algorithm.kao.EdgeOption;
import org.opentripplanner.jags.algorithm.kao.KaoGraph;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;

public class TestKaoGraph extends TestCase {
	public void testBasic() throws Exception {
		KaoGraph kg = new KaoGraph();
		assertNotNull( kg );
	}
	
	public void testImport() throws Exception {
	  
	  GtfsContext context = GtfsLibrary.readGtfs(new File(TestConstants.CALTRAIN_GTFS));
		
		KaoGraph kg = new KaoGraph();
		GTFSHopLoader hl = new GTFSHopLoader(kg,context);
		hl.load();
		
		Vertex mlb = kg.getVertex( "Caltrain_Millbrae Caltrain" );
		assertTrue( mlb.getDegreeOut() == 236 );
		assertTrue( mlb.getDegreeIn() == 236 );
		
		Vertex gilroy = kg.getVertex( "Caltrain_Gilroy Caltrain" );
		assertNotNull( gilroy );
	}
	
	public void testF() throws Exception {
	  
	  GtfsContext context = GtfsLibrary.readGtfs(new File(TestConstants.CALTRAIN_GTFS));
		
		KaoGraph kg = new KaoGraph();
		kg.setGtfsContext(context);
		
		GTFSHopLoader hl = new GTFSHopLoader(kg,context);
		hl.load();
		
		ArrayList<EdgeOption> hol = kg.sortedEdges(new GregorianCalendar(2009,8,7,12,0,0), 1000000000);
		assertEquals(43200,((Hop)hol.get(0).edge.payload).getStartStopTime().getArrivalTime());
		assertEquals(82260, ((Hop)hol.get(hol.size()-1).edge.payload).getEndStopTime().getArrivalTime());
	}
}

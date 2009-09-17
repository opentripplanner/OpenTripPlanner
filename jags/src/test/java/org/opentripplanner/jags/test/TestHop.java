package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.PackagedFeed;


public class TestHop extends TestCase {
	
	public void testHopAfterMidnight() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		GTFSHopFactory hf = new GTFSHopFactory( feed );
		ArrayList<Hop> hops = hf.run();
		
		Collections.sort(hops, new Hop.HopArrivalTimeComparator());
		Hop last = hops.get(hops.size()-1);
		
		GregorianCalendar aSundayAtMidnight = new GregorianCalendar(2009,7,30,0,0,0);
		assertTrue(last.walk(new State(aSundayAtMidnight), new WalkOptions()).weight==5820.0);
	}
}

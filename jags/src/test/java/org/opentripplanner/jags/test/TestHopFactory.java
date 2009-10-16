package org.opentripplanner.jags.test;

import java.util.ArrayList;
import java.util.Collections;

import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.gtfs.PackagedFeed;

import junit.framework.TestCase;



public class TestHopFactory extends TestCase {
	
	public void testBasic() throws Exception {
		Feed feed = new Feed(new PackagedFeed( TestConstants.CALTRAIN_GTFS ));
		GTFSHopFactory hf = new GTFSHopFactory( feed );
		ArrayList<Hop> hops = hf.run();
		
		Collections.sort(hops, new Hop.HopArrivalTimeComparator());
		Hop last = hops.get(hops.size()-1);
		assertTrue(last.start.departure_time.getSecondsSinceMidnight()==91740);
	}
}

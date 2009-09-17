package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.opentripplanner.jags.gtfs.PackagedFeed;
import org.opentripplanner.jags.gtfs.Stop;
import org.opentripplanner.jags.gtfs.StopTime;


public class TestPackagedFeed  extends TestCase {
	public void testGetStopTable() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		
		for(Stop stop : feed.stopTable) {
			assertEquals( stop.stop_name, "San Francisco Caltrain" );
			break;
		}
	}
	
	public void testGetStopTimeTable() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		
		for(StopTime st : feed.stopTimeTable) {
			assertEquals( st.stop_id, "22nd Street Caltrain" );
			break;
		}
	}
}

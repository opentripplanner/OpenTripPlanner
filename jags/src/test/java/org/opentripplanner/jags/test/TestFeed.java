package org.opentripplanner.jags.test;

import org.opentripplanner.jags.gtfs.PackagedFeed;
import org.opentripplanner.jags.gtfs.ServiceCalendar;
import org.opentripplanner.jags.gtfs.Table;
import org.opentripplanner.jags.gtfs.Trip;
import org.opentripplanner.jags.gtfs.types.GTFSDate;

import junit.framework.TestCase;

public class TestFeed extends TestCase {
	public void testBasic() throws Exception {
		String feed_name = "google_transit.zip";
		PackagedFeed feed = new PackagedFeed( feed_name );
		assertEquals( feed.getZippedFeed().getName(), feed_name);
	}
	
	public void testGetTable() throws Exception {
		PackagedFeed feed = new PackagedFeed( "google_transit.zip" );
		Table table = feed.getTable( "stop_times" );
		
		assertEquals( table.getHeader().index("trip_id"), 0 );
		
		for(String[] record : table ) {
			String fromElement = record[0];
			assertEquals( fromElement, "10W1010" );
			break;
		}
	}
	
	public void testGetStop() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadStops();
		assertEquals( feed.stops.get( "San Carlos Caltrain" ).stop_name, "San Carlos Caltrain" );
	}
	
	public void testGetTrip() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadTrips();
		
		assertEquals( feed.trips.get("21520090831").trip_id, "21520090831" );
		
	}
	
	public void testGetStopTime() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadStopTimes();
		
		assertEquals( feed.trips.get("21520090831").getStopTimes().get(0).stop_sequence, new Integer(1) );
		assertEquals( feed.trips.get("21520090831").getStopTimes().get(0).getTrip().trip_id, "21520090831" );
	}
	
	public void testGetCalendar() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadCalendar();
		
		assertEquals( feed.getAllServiceCalendars().get(0).service_id, "WD20090831" );
	}
	
	public void testGetCalendarDate() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadCalendarDates();
		
		assertEquals( feed.getServiceCalendar("WD20090831").getServiceCalendarDates().get(0).service_id, "WD20090831" );
		
		ServiceCalendar sc = feed.getServiceCalendar("WD20090831");
		assertEquals( sc.getServiceCalendarDate( new GTFSDate(2009,12,25)).exception_type, Integer.valueOf(2) );
		assertEquals( sc.getServiceCalendarDate( new GTFSDate(2009,12,26)), null );
	}
	
	public void testCalendarDateRunsOn() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadCalendarDates();
		
		ServiceCalendar sc = feed.getServiceCalendar("WD20090831");
		// TEST EXCEPTION BEFORE PEROID START
		try {
			sc.runsOn(new GTFSDate(2009,8,30));
			assertTrue(false); // should have popped error by now
		} catch (Exception ex) {
			assertEquals( ex.getMessage(), "2009.8.30 is before period start 2009.8.31");
		}
		// TEST EXCEPTION AFTER PERIOD END
		try {
			sc.runsOn(new GTFSDate(2019,9,1));
			assertTrue(false); // should have popped error by now
		} catch (Exception ex) {
			assertEquals( ex.getMessage(), "2019.9.1 is after period end 2019.8.31");
		}
		
		// TEST DOWS WORK RIGHT
		assertTrue( sc.runsOn( new GTFSDate(2009,9,14) ) );
		assertTrue( sc.runsOn( new GTFSDate(2009,9,15) ) );
		assertTrue( sc.runsOn( new GTFSDate(2009,9,16) ) );
		assertTrue( sc.runsOn( new GTFSDate(2009,9,17) ) );
		assertTrue( sc.runsOn( new GTFSDate(2009,9,18) ) );
		assertFalse( sc.runsOn( new GTFSDate(2009,9,19) ) );
		assertFalse( sc.runsOn( new GTFSDate(2009,9,20) ) );
		
		// TEST EXCEPTIONS WORK RIGHT
		assertFalse( sc.runsOn( new GTFSDate(2009,9,7) ) );
		assertFalse( sc.runsOn( new GTFSDate(2009,12,25) ) );
		assertFalse( sc.runsOn( new GTFSDate(2009,11,26) ) );
	}
	
	public void testTripServiceCalendar() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadTrips();
		
		Trip tr = feed.getTrip( "36520090302" );
		
		try {
			System.out.println( tr.getServiceCalendar() );
		} catch (Exception e) {
			assertEquals( e.getMessage(), "ServiceCalendars have not been loaded" );
		}
		
		feed.loadCalendarDates();
		
		assertEquals( tr.getServiceCalendar().service_id, "WD20090302" );
	}
	
	public void testGetServiceCalendars() throws Exception {
		PackagedFeed feed = new PackagedFeed( "caltrain_gtfs.zip" );
		feed.loadCalendarDates();
		
		assertTrue( feed.getServiceCalendars(new GTFSDate(2009,12,5)).size() == 2 );
	}
}

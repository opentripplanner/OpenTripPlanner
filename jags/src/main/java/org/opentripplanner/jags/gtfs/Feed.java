package org.opentripplanner.jags.gtfs;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.opentripplanner.jags.gtfs.types.GTFSDate;

public class Feed {
	
	private ZipFile zippedFeed;
	public HashMap<String,Stop> stops;
	public HashMap<String,Trip> trips;
	public HashMap<String,ServiceCalendar> serviceCalendars;
	
	public Feed( String feed_name ) throws ZipException, IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    	File ff = new File( feed_name );
		zippedFeed = new ZipFile(ff);
	}
	
	public void loadCalendar() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		serviceCalendars = new HashMap<String,ServiceCalendar>();
		Table table = this.getTable( "calendar" );
		while( table.hasMoreElements() ) {
			ServiceCalendar cal = new ServiceCalendar( table, table.nextElement() );
			serviceCalendars.put( cal.service_id, cal );
		}
	}
	
	public void loadCalendarDates() throws Exception {
		loadCalendar();
		
		Table table = this.getTable( "calendar_dates" );
		while( table.hasMoreElements() ) {
			ServiceCalendarDate scd = new ServiceCalendarDate( table, table.nextElement() );
			ServiceCalendar sc = getServiceCalendar( scd.service_id );
			// if a service calendar doesn't exist for this exception, create one
			if(sc==null) {
				sc = new ServiceCalendar(scd);
				serviceCalendars.put(scd.service_id, sc);
			}
			sc.addServiceCalendarDate( scd );
		}
	}
	
	public void loadStops() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		stops = new HashMap<String,Stop>();
		Table stops_table = this.getTable( "stops" );
		while( stops_table.hasMoreElements() ) {
			Stop stop = new Stop( stops_table, stops_table.nextElement() );
			stops.put( stop.stop_id, stop );
		}
	}
	
	public void loadTrips() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		trips = new HashMap<String,Trip>();
		Table trips_table = this.getTable( "trips" );
		while( trips_table.hasMoreElements() ) {
			Trip trip = new Trip( trips_table, trips_table.nextElement() );
			trips.put( trip.trip_id, trip);
		}
	}
	
	public void loadStopTimes() throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		loadTrips();
		
		Table stoptimes_table = this.getTable( "stop_times" );
		while( stoptimes_table.hasMoreElements() ) {
			StopTime stoptime = new StopTime( stoptimes_table, stoptimes_table.nextElement() );
			trips.get(stoptime.trip_id).addStopTime( stoptime );
		}
	}
	

	
	public void load() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException{
		loadStops();
		
		loadTrips();
		
		loadStopTimes();
	}
	
	public Table getTable( String tableName ) throws IOException {
		InputStream table_stream = zippedFeed.getInputStream( zippedFeed.getEntry( tableName+".txt" ) );
		return new Table( this, table_stream );
	}
	
	public ZipFile getZippedFeed() {
		return zippedFeed;
	}
	
	public Collection<Stop> getAllStops() {
		return this.stops.values();
	}

	public Collection<Trip> getAllTrips() {
		return this.trips.values();
	}
	
	public ArrayList<ServiceCalendar> getAllServiceCalendars() {
		return new ArrayList<ServiceCalendar>( this.serviceCalendars.values() );
	}

	public ServiceCalendar getServiceCalendar(String service_id) throws Exception {
		if( serviceCalendars == null ) {
			throw new Exception( "ServiceCalendars have not been loaded" );
		}
		
		return this.serviceCalendars.get( service_id );
	}

	public Trip getTrip(String tripId) {
		return this.trips.get(tripId);
	}
	
	public ArrayList<ServiceCalendar> getServiceCalendars(GTFSDate date) {
		ArrayList<ServiceCalendar> ret = new ArrayList<ServiceCalendar>();
		for( ServiceCalendar sc : this.serviceCalendars.values() ) {
			try {
				if( sc.runsOn( date) ) {
					ret.add( sc );
				}
			} catch (IndexOutOfBoundsException e) {
				//totally ignore it
			}
		}
		return ret;
	}
}

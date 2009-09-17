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

public class PackagedFeed {
	
	private ZipFile zippedFeed;
	public HashMap<String,Stop> stops;
	public HashMap<String,Trip> trips;
	public HashMap<String,ServiceCalendar> serviceCalendars;
	
	public PackagedFeed( String feed_name ) throws ZipException, IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
    	File ff = new File( feed_name );
		zippedFeed = new ZipFile(ff);
	}
	
	public void loadCalendar() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		serviceCalendars = new HashMap<String,ServiceCalendar>();
		Table table = this.getTable( "calendar" );
		for( String[] record : table ) {
			ServiceCalendar cal = new ServiceCalendar( this, table.getHeader(), record );
			serviceCalendars.put( cal.service_id, cal );
		}
	}
	
	public void loadCalendarDates() throws Exception {
		loadCalendar();
		
		Table table = this.getTable( "calendar_dates" );
		for( String[] record : table ) {
			ServiceCalendarDate scd = new ServiceCalendarDate( this, table.getHeader(), record );
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
		Table table = this.getTable( "stops" );
		for( String[] record : table ) {
			Stop stop = new Stop( this, table.getHeader(), record );
			stops.put( stop.stop_id, stop );
		}
	}
	
	public void loadTrips() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		trips = new HashMap<String,Trip>();
		Table table = this.getTable( "trips" );
		for( String[] record : table ) {
			Trip trip = new Trip( this, table.getHeader(), record );
			trips.put( trip.trip_id, trip);
		}
	}
	
	public void loadStopTimes(boolean verbose) throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		loadTrips();
		
		Table table = this.getTable( "stop_times" );
		int i=0;
		for( String[] record : table ) {
			i++;
			if(verbose && i%1000==0){ System.out.println( "stoptime "+i ); }
			StopTime stoptime = new StopTime( this, table.getHeader(), record );
			trips.get(stoptime.trip_id).addStopTime( stoptime );
		}
	}
	
	public void loadStopTimes() throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		loadStopTimes(false);
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

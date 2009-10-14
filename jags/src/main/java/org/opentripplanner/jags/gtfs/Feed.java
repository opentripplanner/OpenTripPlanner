package org.opentripplanner.jags.gtfs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.opentripplanner.jags.gtfs.types.GTFSDate;

public class Feed {
	public HashMap<String,Route> routes;
	public HashMap<String,Stop> stops;
	public HashMap<String,Trip> trips;
	public HashMap<String,ServiceCalendar> serviceCalendars;
	
	private PackagedFeed packagedFeed;
	
	public Feed(PackagedFeed packagedFeed) {
		this.packagedFeed = packagedFeed;
	}
	
	public void loadCalendar() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		serviceCalendars = new HashMap<String,ServiceCalendar>();
		Table table = packagedFeed.getTable( "calendar" );
		for( String[] record : table ) {
			ServiceCalendar cal = new ServiceCalendar( table.getHeader(), record );
			serviceCalendars.put( cal.service_id, cal );
		}
	}
	
	public void loadCalendarDates() throws Exception {
		loadCalendar();
		
		Table table = packagedFeed.getTable( "calendar_dates" );
		for( String[] record : table ) {
			ServiceCalendarDate scd = new ServiceCalendarDate( table.getHeader(), record );
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
		for( Stop stop : packagedFeed.stopTable ) {
			stops.put( stop.stop_id, stop );
		}
	}
	
	public void loadRoutes() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		routes = new HashMap<String,Route>();
		for( Route route : packagedFeed.routeTable ) {
			routes.put( route.route_id, route );
		}
	}
		
	public void loadTrips() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		
		
		trips = new HashMap<String,Trip>();
		Table table = packagedFeed.getTable( "trips" );
		for( String[] record : table ) {
			Trip trip = new Trip( this, table.getHeader(), record );
			trips.put( trip.trip_id, trip);
		}
	}
	
	public void loadStopTimes(boolean verbose) throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		loadTrips();
		
		for( StopTime stoptime : packagedFeed.stopTimeTable ) {
			Trip trip = trips.get(stoptime.trip_id);
			stoptime.setTrip( trip );
			stoptime.setStop( stops.get(stoptime.stop_id) );
			trip.addStopTime( stoptime );
		}
	}
	
	public void loadStopTimes() throws SecurityException, IllegalArgumentException, IOException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		loadStopTimes(false);
	}
	

	
	public void load() throws IOException, SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException{
		loadStops();
		
		loadRoutes();
		
		loadTrips();
		
		loadStopTimes();
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
	
	public Route getRoute(String routeId) {
		return this.routes.get(routeId);
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

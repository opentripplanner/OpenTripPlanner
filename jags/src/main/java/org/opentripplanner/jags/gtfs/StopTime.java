package org.opentripplanner.jags.gtfs;



import java.lang.reflect.InvocationTargetException;

import org.opentripplanner.jags.gtfs.types.GTFSTime;


public class StopTime extends Record implements Comparable<StopTime> {

	public String trip_id;
	public GTFSTime arrival_time;
	public GTFSTime departure_time;
	public String stop_id;
	public Integer stop_sequence;
	public String stop_headsign;
	public Integer pickup_type;
	public Integer drop_off_type;
	public Double shape_dist_traveled;
	
	Trip trip=null;
	Stop stop=null;
	
	StopTime(Table stops, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		super(stops, record);
	}
	
	public Trip getTrip() {
		if(trip==null) {
		    trip = table.feed.trips.get( trip_id );
		}
		return trip;
	}
	
	public Stop getStop() {
		if(stop==null) {
			stop = table.feed.stops.get( stop_id );
		}
		return stop;
	}

	public String toString() {
		return "<StopTime "+stop_sequence+" "+stop_id+" "+arrival_time+" "+departure_time+">";
	}

	public int compareTo(StopTime other) {
		return this.stop_sequence.compareTo( other.stop_sequence); 
	}
}

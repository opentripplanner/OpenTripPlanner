package org.opentripplanner.jags.gtfs;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;

public class Trip extends Record {

	public String route_id;
	public String service_id;
	public String trip_id;
	public String trip_headsign;
	public String trip_short_name;
	public String direction_id;
	public String block_id;
	public String shape_id;
	
	ArrayList<StopTime> stoptimes;
	
	Trip(PackagedFeed feed, TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		super(feed, header, record);
		stoptimes = new ArrayList<StopTime>();
	}
	
	public ArrayList<StopTime> getStopTimes() {
		//sort every time they're gotten
		Collections.sort( stoptimes );
		
		return stoptimes;
	}
	
	public ServiceCalendar getServiceCalendar() throws Exception {
		return feed.getServiceCalendar( service_id );
	}
	
	public void addStopTime( StopTime st ) {
		stoptimes.add( st );
	}
	
	public String toString() {
		return "<Trip "+trip_id+">";
	}

}

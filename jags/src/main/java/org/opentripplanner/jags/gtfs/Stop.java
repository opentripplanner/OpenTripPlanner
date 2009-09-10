package org.opentripplanner.jags.gtfs;

import java.lang.reflect.InvocationTargetException;

public class Stop extends Record{
	public String stop_id;
	public String stop_code;
	public String stop_name;
	public String stop_desc;
	public Double stop_lat;
	public Double stop_lon;
	public String zone_id;
	public String stop_url;
	public String location_type;
	public String parent_station;
	
	Stop(Table stops, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		super(stops, record);
	}
	
	public String toString() {
		return "<Stop "+stop_id+" ("+stop_lat+","+stop_lon+")>";
	}
}

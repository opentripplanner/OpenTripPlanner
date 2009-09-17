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
	
	// null constructor for Hibernate
	public Stop() {}
	
	Stop(PackagedFeed feed, TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		super(feed, header, record);
	}
	
	public String toString() {
		return "<Stop "+stop_id+" ("+stop_lat+","+stop_lon+")>";
	}
	
	//Getters and Setters for Hibernate
	public String getStop_id() {
		return stop_id;
	}

	public void setStop_id(String stopId) {
		stop_id = stopId;
	}

	public String getStop_name() {
		return stop_name;
	}

	public void setStop_name(String stopName) {
		stop_name = stopName;
	}

	public Double getStop_lat() {
		return stop_lat;
	}

	public void setStop_lat(Double stopLat) {
		stop_lat = stopLat;
	}

	public Double getStop_lon() {
		return stop_lon;
	}

	public void setStop_lon(Double stopLon) {
		stop_lon = stopLon;
	}
	public String getStop_code() {
		return stop_code;
	}

	public void setStop_code(String stopCode) {
		stop_code = stopCode;
	}

	public String getStop_desc() {
		return stop_desc;
	}

	public void setStop_desc(String stopDesc) {
		stop_desc = stopDesc;
	}

	public String getZone_id() {
		return zone_id;
	}

	public void setZone_id(String zoneId) {
		zone_id = zoneId;
	}

	public String getStop_url() {
		return stop_url;
	}

	public void setStop_url(String stopUrl) {
		stop_url = stopUrl;
	}

	public String getLocation_type() {
		return location_type;
	}

	public void setLocation_type(String locationType) {
		location_type = locationType;
	}

	public String getParent_station() {
		return parent_station;
	}

	public void setParent_station(String parentStation) {
		parent_station = parentStation;
	}
}

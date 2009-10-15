package org.opentripplanner.jags.gtfs;



import java.lang.reflect.InvocationTargetException;

import org.opentripplanner.jags.gtfs.types.GTFSTime;


public class StopTime extends Record implements Comparable<StopTime> {
	public Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

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
	
	StopTime(TableHeader header, String[] record) throws SecurityException,
			NoSuchFieldException, IllegalArgumentException,
			IllegalAccessException, InstantiationException,
			InvocationTargetException, NoSuchMethodException {
		super(header, record);
	}
	
	public void setStop(Stop stop) {
		if(stop==null) {
			throw new RuntimeException( "Stops not yet loaded" );
		}
		this.stop = stop;
	}
	
	public void setTrip(Trip trip) {
		this.trip = trip;
	}
	
	public Trip getTrip()  {
		if(trip==null){
			throw new RuntimeException( "Trips not yet loaded" );
		}
		return trip;
	}
	
	public Stop getStop() {
		return stop;
	}

	public String toString() {
		return "<StopTime "+stop_sequence+" "+stop_id+" "+arrival_time+" "+departure_time+">";
	}

	public int compareTo(StopTime other) {
		return this.stop_sequence.compareTo( other.stop_sequence); 
	}
	
	//Big pile of getters and setters for Hibernate
	
	public String getTrip_id() {
		return trip_id;
	}

	public void setTrip_id(String tripId) {
		trip_id = tripId;
	}

	public GTFSTime getArrival_time() {
		return arrival_time;
	}

	public void setArrival_time(GTFSTime arrivalTime) {
		arrival_time = arrivalTime;
	}

	public GTFSTime getDeparture_time() {
		return departure_time;
	}

	public void setDeparture_time(GTFSTime departureTime) {
		departure_time = departureTime;
	}

	public String getStop_id() {
		return stop_id;
	}

	public void setStop_id(String stopId) {
		stop_id = stopId;
	}

	public Integer getStop_sequence() {
		return stop_sequence;
	}

	public void setStop_sequence(Integer stopSequence) {
		stop_sequence = stopSequence;
	}

	public String getStop_headsign() {
		return stop_headsign;
	}

	public void setStop_headsign(String stopHeadsign) {
		stop_headsign = stopHeadsign;
	}

	public Integer getPickup_type() {
		return pickup_type;
	}

	public void setPickup_type(Integer pickupType) {
		pickup_type = pickupType;
	}

	public Integer getDrop_off_type() {
		return drop_off_type;
	}

	public void setDrop_off_type(Integer dropOffType) {
		drop_off_type = dropOffType;
	}

	public Double getShape_dist_traveled() {
		return shape_dist_traveled;
	}

	public void setShape_dist_traveled(Double shapeDistTraveled) {
		shape_dist_traveled = shapeDistTraveled;
	}
}

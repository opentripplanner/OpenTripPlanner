package org.opentripplanner.jags.edgetype;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.GregorianCalendar;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.gtfs.ServiceCalendar;
import org.opentripplanner.jags.gtfs.StopTime;
import org.opentripplanner.jags.gtfs.exception.DateOutOfBoundsException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class Hop extends AbstractPayload implements Comparable<Hop>, Drawable {
	
	public static class HopArrivalTimeComparator implements Comparator<Hop> {

		public int compare(Hop arg0, Hop arg1) {
			Integer v1 = new Integer(arg0.end.arrival_time.getSecondsSinceMidnight());
			Integer v2 = new Integer(arg1.end.arrival_time.getSecondsSinceMidnight());
			return v1.compareTo(v2);
		}
		
	}
	
	private static final long serialVersionUID = -7761092317912812048L;
	private static final int SECS_IN_DAY = 86400;
	private static final int SECS_IN_HOUR = 3600;
	private static final int SECS_IN_MINUTE = 60;
	public StopTime start;
	public StopTime end;
	public ServiceCalendar calendar;
	private int elapsed;

	public Hop( StopTime start, StopTime end ) throws Exception {
		this.start = start;
		this.end = end;
		this.calendar = this.start.getTrip().getServiceCalendar();
		this.elapsed = end.arrival_time.getSecondsSinceMidnight() - start.arrival_time.getSecondsSinceMidnight();
	}
	
    public WalkResult walk( State state0, WalkOptions wo ) {
    	//The basic idea: find the amount of time from state0.time until the hop begins. Weight is that wait+the transit time
    	
    	GregorianCalendar serviceDay = (GregorianCalendar)state0.time.clone();
    	int secondsSinceMidnight = state0.time.get(GregorianCalendar.HOUR_OF_DAY)*SECS_IN_HOUR+
    	                           state0.time.get(GregorianCalendar.MINUTE)*SECS_IN_MINUTE+
    	                           state0.time.get(GregorianCalendar.SECOND);
    	// if the Hop's departure StopTime is more than 24 hours from midnight (say, yesterday) we don't need to check
    	// that the ServiceCalendar runs today - we need to check that it runs yesterday.
    	int overageDays = start.departure_time.getSecondsSinceMidnight()/SECS_IN_DAY;
    	serviceDay.add(GregorianCalendar.DATE, -overageDays );
    	secondsSinceMidnight += SECS_IN_DAY*overageDays;
    	
    	try {
			if(!calendar.runsOn(serviceDay)) {
				return null;
			}
		} catch (DateOutOfBoundsException ex) {
			return null;
		}
    	
    	int wait = start.departure_time.getSecondsSinceMidnight() - secondsSinceMidnight;
    	if( wait < 0 ) {
    		return null;
    	}
    	
    	State state1 = state0.clone();
    	state1.time.add(GregorianCalendar.SECOND, wait+elapsed);
    	return new WalkResult(wait+elapsed, state1);
    }
    
    public WalkResult walkBack( State state0, WalkOptions wo ) {
    	//The basic idea: find the amount of time from the hop arrival to state0.time. Weight is that wait+the transit time

    	GregorianCalendar serviceDay = (GregorianCalendar)state0.time.clone();
    	int secondsSinceMidnight = state0.time.get(GregorianCalendar.HOUR_OF_DAY)*SECS_IN_HOUR+
    	                           state0.time.get(GregorianCalendar.MINUTE)*SECS_IN_MINUTE+
    	                           state0.time.get(GregorianCalendar.SECOND);
    	// if the Hop's arrival StopTime is more than 24 hours from midnight (say, yesterday) we don't need to check
    	// that the ServiceCalendar runs today - we need to check that it runs yesterday.
    	int overageDays = end.arrival_time.getSecondsSinceMidnight()/SECS_IN_DAY;
    	serviceDay.add(GregorianCalendar.DATE, -overageDays );
    	secondsSinceMidnight += SECS_IN_DAY*overageDays;
    	
    	if(!calendar.runsOn(serviceDay)) {
    		return null;
    	}
    	
    	int wait = secondsSinceMidnight - end.arrival_time.getSecondsSinceMidnight();
    	if( wait < 0 ) {
    		return null;
    	}
    	
    	State state1 = state0.clone();
    	state1.time.add(GregorianCalendar.SECOND, -(wait+elapsed));
    	return new WalkResult(wait+elapsed, state1);
    	
    }

	public int compareTo(Hop arg0) {
		return this.end.compareTo( arg0.end ); 
	}
	
	public String toString() {
		return this.start + " " + this.end + " " + this.calendar;
	}

	ArrayList<DrawablePoint> geometryCache = null;
	public ArrayList<DrawablePoint> getDrawableGeometry() {
		if(geometryCache != null) {
			return geometryCache;
		}
		
		ArrayList<DrawablePoint> ret = new ArrayList<DrawablePoint>();
		
		ret.add( new DrawablePoint(this.start.getStop().stop_lon.floatValue(),
				           this.start.getStop().stop_lat.floatValue(),
				           this.start.departure_time.getSecondsSinceMidnight()) );
		ret.add( new DrawablePoint(this.end.getStop().stop_lon.floatValue(),
				           this.end.getStop().stop_lat.floatValue(),
				           this.end.arrival_time.getSecondsSinceMidnight()) );
		
		geometryCache = ret;
		return ret;
	}

	public String getDirection() {
		return start.getTrip().trip_headsign;
	}

	public double getDistanceKm() {
		Point startGeom = (Point) start.getStop().getGeometry();
		Point endGeom = (Point) end.getStop().getGeometry();
		return startGeom.distance(endGeom); //FIXME: units are probably wrong
	}

	public String getEnd() {
		return end.stop_headsign;
	}

	public TransportationMode getMode() {
		return start.getTrip().getRoute().getTransportationMode();
	}

	public String getStart() {
		return start.stop_headsign;
	}

	public String getName() {
		return start.getTrip().getRoute().getName();
	}

	public Geometry getGeometry() {
		//FIXME: use shape if available
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		return factory.createLineString(new Coordinate[] {
				start.getStop().getGeometry().getCoordinate(), 
				end.getStop().getGeometry().getCoordinate()
		}
		);
	}
	
}

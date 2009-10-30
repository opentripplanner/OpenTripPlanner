package org.opentripplanner.jags.edgetype;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class Hop extends AbstractPayload implements Comparable<Hop>, Drawable {
	
	public static class HopArrivalTimeComparator implements Comparator<Hop> {

		public int compare(Hop arg0, Hop arg1) {
		  int v1 = arg0.end.getArrivalTime();
		  int v2 = arg1.end.getArrivalTime();
		  return v1 - v2;
		}
		
	}
	
	private static final long serialVersionUID = -7761092317912812048L;
	
	private static final int SECS_IN_DAY = 86400;
	
	private StopTime start;
	private StopTime end;
	private AgencyAndId _serviceId;
	private int elapsed;

	public Hop( StopTime start, StopTime end ) throws Exception {
		this.start = start;
		this.end = end;
		this._serviceId = start.getTrip().getServiceId();
		this.elapsed = end.getArrivalTime() - start.getDepartureTime();
	}
	
	public StopTime getStartStopTime() {
	  return start;
	}
	
	public StopTime getEndStopTime() {
	  return end;
	}
	
	
	
    public WalkResult walk( State state0, WalkOptions wo ) {
      
      long currentTime = state0.getTime();
      Date serviceDate = getServiceDate(currentTime, false);
      int secondsSinceMidnight = (int) ((currentTime-serviceDate.getTime()) / 1000);
      
    	CalendarService service = wo.getGtfsContext().getCalendarService();
    	Set<Date> serviceDates = service.getServiceDatesForServiceId(_serviceId);
      if( ! serviceDates.contains(serviceDate))
    	  return null;
    	
    	int wait = start.getDepartureTime() - secondsSinceMidnight;
    	if( wait < 0 ) {
    		return null;
    	}
    	
    	State state1 = state0.clone();
    	state1.incrementTimeInSeconds(wait+elapsed);
    	return new WalkResult(wait+elapsed, state1);
    }
    
    public WalkResult walkBack( State state0, WalkOptions wo ) {
      
      long currentTime = state0.getTime();
      Date serviceDate = getServiceDate(currentTime, true);
      int secondsSinceMidnight = (int) ((currentTime-serviceDate.getTime()) / 1000);

      CalendarService service = wo.getGtfsContext().getCalendarService(); 
      if( ! service.getServiceDatesForServiceId(_serviceId).contains(serviceDate) )
        return null;
    	
    	int wait = secondsSinceMidnight - end.getArrivalTime();
    	if( wait < 0 ) {
    		return null;
    	}
    	
    	State state1 = state0.clone();
    	state1.incrementTimeInSeconds(-(wait+elapsed));
    	return new WalkResult(wait+elapsed, state1);
    	
    }

	public int compareTo(Hop arg0) {
		return this.end.compareTo( arg0.end ); 
	}
	
	public String toString() {
		return this.start + " " + this.end + " " + this._serviceId;
	}

	ArrayList<DrawablePoint> geometryCache = null;
	public ArrayList<DrawablePoint> getDrawableGeometry() {
		if(geometryCache != null) {
			return geometryCache;
		}
		
		ArrayList<DrawablePoint> ret = new ArrayList<DrawablePoint>();
		
		ret.add( new DrawablePoint((float)this.start.getStop().getLon(),
				           (float) this.start.getStop().getLat(),
				           this.start.getDepartureTime()) );
		ret.add( new DrawablePoint((float)this.end.getStop().getLon(),
				           (float)this.end.getStop().getLat(),
				           this.end.getArrivalTime()) );
		
		geometryCache = ret;
		return ret;
	}

	public String getDirection() {
		return start.getTrip().getTripHeadsign();
	}

	public double getDistanceKm() {
	  Stop stop1 = start.getStop();
	  Stop stop2 = start.getStop();
	  return GtfsLibrary.distance(stop1.getLat(), stop1.getLon(), stop2.getLat(), stop2.getLon());
	}

	public String getEnd() {
		return end.getStopHeadsign();
	}

	public TransportationMode getMode() {
	  return GtfsLibrary.getTransportationMode(start.getTrip().getRoute());
	}

	public String getStart() {
		return start.getStopHeadsign();
	}

	public String getName() {
		return GtfsLibrary.getRouteName(start.getTrip().getRoute());
	}

	public Geometry getGeometry() {
		//FIXME: use shape if available
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		Stop stop1 = start.getStop();
    Stop stop2 = start.getStop();
		
		Coordinate c1 = new Coordinate(stop1.getLon(),stop1.getLat());
		Coordinate c2 = new Coordinate(stop2.getLon(),stop2.getLat());
		
		return factory.createLineString(new Coordinate[] {c1,c2});
	}	
	
	/****
	 * Private Methods
	 ****/
	
	private Date getServiceDate(long currentTime, boolean useArrival) {
	  int scheduleTime = useArrival ? end.getArrivalTime() : start.getDepartureTime();
	  Calendar c = Calendar.getInstance();
	  c.setTimeInMillis(currentTime);
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    
    int dayOverflow = scheduleTime /SECS_IN_DAY;
    c.add(Calendar.DAY_OF_YEAR,-dayOverflow);
    return c.getTime();
	}
}

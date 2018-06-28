package org.opentripplanner.index.model;

import java.util.List;


import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;

public class TimetableForTrip {
    public AgencyAndId tripId;
    public boolean currentTrip;
    public EncodedPolylineBean geometry;
    public int schoolOnly;
    public long serviceDay;
    public List<TripTimeShort> triptimes; 
    public String routeLongName;
    public String routeShortName;
    public String tripShortName;
    public String tripHeadSign;
    public String tripDirectionId;

    /** This class represent timetable for a trip.
     */
    public TimetableForTrip(Timetable table, TripTimes tt, ServiceDay sd, boolean currentTrip) {
        this.currentTrip = currentTrip;
        tripHeadSign = tt.trip.getTripHeadsign();
        tripId = tt.trip.getId();
        tripShortName = tt.trip.getTripShortName();
        tripDirectionId = tt.trip.getDirectionId();
        geometry = PolylineEncoder.createEncodings(table.pattern.geometry);
        Route r = tt.trip.getRoute();
        routeLongName = r.getLongName();
        routeShortName = r.getShortName();
        schoolOnly = r.getSchoolOnly();
        serviceDay = sd != null ? sd.time(0) : 0;
        triptimes = TripTimeShort.fromTripTimes(table, tt.trip, serviceDay);
    }

    public TimetableForTrip(Timetable table, TripTimes tt, ServiceDay sd) {
        this(table, tt, sd, false);
    }

    public TimetableForTrip(Timetable table, TripTimes tt, boolean currentTrip) {
        this(table, tt, null, currentTrip);
    }
}
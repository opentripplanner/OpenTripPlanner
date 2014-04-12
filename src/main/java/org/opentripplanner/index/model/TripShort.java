package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;

import com.beust.jcommander.internal.Lists;

public class TripShort {

    public String agency;
    public String id;
    public String tripHeadsign;
    public String serviceId;
    public String shapeId;
    public int direction;

    // INCLUDE start and end time, pattern and route in detail version
    
    public TripShort (Trip trip) {
        agency = trip.getId().getAgencyId();
        id = trip.getId().getId();
        tripHeadsign = trip.getTripHeadsign();
        serviceId = trip.getServiceId().getId();
        AgencyAndId shape = trip.getShapeId();
        shapeId = shape == null ? null : shape.getId();
        direction = Integer.parseInt(trip.getDirectionId());
    }

    public static List<TripShort> list (Collection<Trip> in) {
        List<TripShort> out = Lists.newArrayList();
        for (Trip trip : in) out.add(new TripShort(trip));
        return out;
    }    

}

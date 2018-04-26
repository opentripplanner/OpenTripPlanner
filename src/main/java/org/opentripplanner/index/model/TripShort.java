package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.opentripplanner.model.FeedId;
import org.opentripplanner.model.Trip;

import com.beust.jcommander.internal.Lists;

public class TripShort {

    public FeedId id;
    public String tripHeadsign;
    public FeedId serviceId;
    public String shapeId;
    public Integer direction;

    // INCLUDE start and end time, pattern and route in detail version
    
    public TripShort (Trip trip) {
        id = trip.getId();
        tripHeadsign = trip.getTripHeadsign();
        serviceId = trip.getServiceId();
        FeedId shape = trip.getShapeId();
        shapeId = shape == null ? null : shape.getId();
        String directionId = trip.getDirectionId();
        direction = directionId == null ? null : Integer.parseInt(directionId);
    }

    public static List<TripShort> list (Collection<Trip> in) {
        List<TripShort> out = Lists.newArrayList();
        for (Trip trip : in) out.add(new TripShort(trip));
        return out;
    }    

}

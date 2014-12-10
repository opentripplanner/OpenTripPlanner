package org.opentripplanner.profile;

import org.joda.time.LocalDate;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.routing.core.TraverseModeSet;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * All the modifiable paramters for profile routing.
 */
public class ProfileRequest {

    public LatLon from;
    public LatLon to;
    public int    fromTime;
    public int    toTime;

    /* Speeds are in meters per second. */
    public float  walkSpeed;
    public float  bikeSpeed;
    public float  carSpeed;

    /* The following times are all in minutes. */
    public int    streetTime;
    public int    maxWalkTime;
    public int    maxBikeTime;
    public int    maxCarTime;
    public int    minBikeTime;
    public int    minCarTime;

    public LocalDate date;
    public Option.SortOrder orderBy;
    public int limit;
    public TraverseModeSet accessModes, egressModes, directModes, transitModes;
    public boolean analyst = false; // if true, propagate travel times out to street network

    /*
      This parameter compensates for the fact that GTFS does not contain information about schedule deviation (lateness).
      The min-max travel time range for some trains is zero, since the trips are reported to always have the same
      timings in the schedule. Such an option does not overlap (temporally) its alternatives, and is too easily
      eliminated by an alternative that is only marginally better. We want to effectively push the max travel time of
      alternatives out a bit to account for the fact that they don't always run on schedule.
    */
    public int suboptimalMinutes;
    
    public ProfileRequest clone () {
        ProfileRequest ret = new ProfileRequest();
        ret.from = from;
        ret.to = to;
        ret.fromTime = fromTime;
        ret.toTime = toTime;
        
        ret.walkSpeed = walkSpeed;
        ret.bikeSpeed = bikeSpeed;
        ret.carSpeed = carSpeed;
        
        ret.streetTime = streetTime;
        ret.maxWalkTime = maxWalkTime;
        ret.maxBikeTime = maxBikeTime;
        ret.maxCarTime = maxCarTime;
        ret.minBikeTime = minBikeTime;
        ret.minCarTime = minCarTime;
        
        // LocalDate is immutable, no need to copy
        ret.date = date;
        // TODO: deep clone needed? mutable?
        ret.orderBy = orderBy;
        ret.limit = limit;
        ret.accessModes = accessModes != null ? accessModes.clone() : null;
        ret.egressModes = egressModes != null ? egressModes.clone() : null;
        ret.directModes = directModes != null ? directModes.clone() : null;
        ret.transitModes = transitModes != null ? transitModes.clone() : null;
        
        ret.analyst = analyst;
        ret.suboptimalMinutes = suboptimalMinutes;
        
        return ret;
    }
}

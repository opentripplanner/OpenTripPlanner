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
    public TraverseModeSet modes;
    public boolean analyst = false; // if true, propagate travel times out to street network

}

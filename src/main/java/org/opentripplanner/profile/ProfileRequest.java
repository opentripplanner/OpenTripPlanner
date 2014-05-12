package org.opentripplanner.profile;

import org.joda.time.LocalDate;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.routing.core.TraverseModeSet;

/**
 * All the modifiable paramters for profile routing.
 */
public class ProfileRequest {

    public LatLon from;
    public LatLon to;
    public int fromTime;
    public int toTime;
    public float walkSpeed;
    public float bikeSpeed;
    public int streetTime;
    public int accessTime;
    public LocalDate date;
    public Option.SortOrder orderBy;
    public int limit;
    public TraverseModeSet modes;

}

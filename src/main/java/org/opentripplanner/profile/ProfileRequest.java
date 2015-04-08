package org.opentripplanner.profile;

import org.joda.time.LocalDate;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.Serializable;

/**
 * All the modifiable parameters for profile routing.
 */
public class ProfileRequest implements Serializable, Cloneable {

    /** The latitude of the origin. */
    public double fromLat;
    
    /** The longitude of the origin. */
    public double fromLon;
    
    /** The latitude of the destination. Must be set even in Analyst mode. */
    public double toLat;
    
    /** The longitude of the destination. Must be set even in Analyst mode. */
    public double toLon;
    
    /** The beginning of the departure window, in seconds since midnight. */
    public int    fromTime;
    
    /** The end of the departure window, in seconds since midnight. */
    public int    toTime;

    /** The speed of walking, in meters per second */
    public float  walkSpeed;
    
    /** The speed of cycling, in meters per second */
    public float  bikeSpeed;
    
    /** The speed of driving, in meters per second */
    public float  carSpeed;

    /** Maximum time to reach the destination without using transit */
    public int    streetTime;
    
    /** Maximum walk time before and after using transit */
    public int    maxWalkTime;
    
    /** Maximum bike time when using transit */
    public int    maxBikeTime;
    
    /** Maximum car time before when using transit */ 
    public int    maxCarTime;
    
    /** Minimum time to ride a bike (to prevent extremely short bike legs) */
    public int    minBikeTime;
    
    /** Minimum time to drive (to prevent extremely short driving legs) */
    public int    minCarTime;

    /** The date of the search */
    public LocalDate date;
    
    /** The order in which to return multiple options */
    public Option.SortOrder orderBy;
    
    /** the maximum number of options presented PER ACCESS MODE */
    public int limit;
    
    /** The modes used to access transit */
    public QualifiedModeSet accessModes;
    
    /** The modes used to reach the destination after leaving transit */
    public QualifiedModeSet egressModes;
    
    /** The modes used to reach the destination without transit */
    public QualifiedModeSet directModes;
    
    /** The transit modes used */
    public TraverseModeSet transitModes;
    
    /** If true, disable all goal direction and propagate results to the street network */
    public boolean analyst = false;

    /* The relative importance of different factors when biking */
    /** The relative importance of maximizing safety when cycling */
    public int bikeSafe;
    
    /** The relative importance of minimizing hills when cycling */
    public int bikeSlope;
    
    /** The relative importance of minimizing time when cycling */
    public int bikeTime;
    // FIXME change "safe" to "danger" to consistently refer to the things being minimized

    /**
      This parameter compensates for the fact that GTFS does not contain information about schedule deviation (lateness).
      The min-max travel time range for some trains is zero, since the trips are reported to always have the same
      timings in the schedule. Such an option does not overlap (temporally) its alternatives, and is too easily
      eliminated by an alternative that is only marginally better. We want to effectively push the max travel time of
      alternatives out a bit to account for the fact that they don't always run on schedule.
    */
    public int suboptimalMinutes;
    
    public ProfileRequest clone () throws CloneNotSupportedException {
        return (ProfileRequest) super.clone();
    }
}

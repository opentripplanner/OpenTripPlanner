package org.opentripplanner.profile;

import org.joda.time.LocalDate;
import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.routing.core.TraverseModeSet;

import java.io.Serializable;

/**
 * All the modifiable parameters for profile routing.
 */
public class ProfileRequest implements Serializable, Cloneable {
    private static final long serialVersionUID = -6501962907644662303L;

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
    
    /**
     * Maximum walk time before and after using transit, in minutes
     *
     * NB the time to reach the destination after leaving transit is considered separately from the time to reach
     * transit at the start of the search; e.g. if you set maxWalkTime to 600 (ten minutes), you could potentially walk
     * up to ten minutes to reach transit, and up to _another_ ten minutes to reach the destination after leaving transit.
     *
     * This is required because hard resource limiting on non-objective variables is algorithmically invalid. Consider
     * a case where there is a destination 10 minutes from transit and an origin 5 minutes walk from a feeder bus and
     * 15 minutes walk from a fast train, and the walk budget is 20 minutes. If an intermediate point in the search
     * (for example, a transfer station) is reached by the fast train before it is reached by the bus, the route using
     * the bus will be dominated. When we leave transit, though, we have already used up 15 minutes of our walk budget
     * and don't have enough remaining to reach the destination.
     *
     * This is solved by using separate walk budgets at the origin and destination. It could also be solved (although this
     * would slow the algorithm down) by retaining all Pareto-optimal combinations of (travel time, walk distance).
     */
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

    /**
     * What is the minimum proportion of the time for which a destination must be accessible for it to be included in
     * the average?
     *
     * This avoids issues where destinations are reachable for some very small percentage of the time, either because
     * there is a single departure near the start of the time window, or because they take approximately 2 hours
     * (the default maximum cutoff) to reach.

     * Consider a search run with time window 7AM to 9AM, and an origin and destination connected by an express
     * bus that runs once at 7:05. For the first five minutes of the time window, accessibility is very good.
     * For the rest, there is no accessibility; if we didn't have this rule in place, the average would be the average
     * of the time the destination is reachable, and the time it is unreachable would be excluded from the calculation
     * (see issue 2148)

     * There is another issue that this rule does not completely address. Consider a trip that takes 1:45
     * exclusive of wait time and runs every half-hour. Half the time it takes less than two hours and is considered
     * and half the time it takes more than two hours and is excluded, so the average is biased low on very long trips.
     * This rule catches the most egregious cases (say where we average only the best four minutes out of a two-hour
     * span) but does not completely address the issue. However if you're looking at a time cutoff significantly
     * less than two hours, it's not a big problem. Significantly less is half the headway of your least-frequent service, because
     * if there is a trip on your least-frequent service that takes on average the time cutoff plus one minute
     * it will be unbiased and considered unreachable iff the longest trip is less than two hours, which it has
     * to be if the time cutoff plus half the headway is less than two hours, assuming a symmetric travel time
     *
     * The default is 0.5.
     */
    public float reachabilityThreshold = 0.5f;

    /** What assumption should be used when boarding frequency vehicles? */
    public RaptorWorkerTimetable.BoardingAssumption boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.RANDOM;

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

    /** A non-destructive scenario to apply when executing this request */
    public Scenario scenario;
    
    public ProfileRequest clone () throws CloneNotSupportedException {
        return (ProfileRequest) super.clone();
    }
}

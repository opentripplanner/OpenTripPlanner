/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.algorithm.strategies.DefaultExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraverseOptions implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger _log = LoggerFactory.getLogger(TraverseOptions.class);

    /** max speed along streets, in meters per second */
    private double walkSpeed;
    private double bikeSpeed;
    private double carSpeed;

    private TraverseModeSet modes;

    public Calendar calendar;

    private CalendarService calendarService;

    private Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();

    private boolean back = false;

    public boolean wheelchairAccessible = false;

    public OptimizeType optimizeFor = OptimizeType.QUICK;

    /**
     * A maxWalkDistance of Double.MAX_VALUE indicates no limit
     */
    double maxWalkDistance = Double.MAX_VALUE;

    /**
     * When optimizing for few transfers, we don't actually optimize for fewest transfers, as this
     * can lead to absurd results. Consider a trip in New York from Grand Army Plaza (the one in
     * Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to wait until midnight,
     * when the 4 train runs local the whole way. The actual fastest route is the 2/3 to the 4/5 at
     * Nevins to the 6 at Union Square, which takes half an hour. Even someone optimizing for fewest
     * transfers doesn't want to wait until midnight. Maybe they would be willing to walk to 7th Ave
     * and take the Q to Union Square, then transfer to the 6. If this takes less than
     * optimize_transfer_penalty seconds, then that's what we'll return.
     */
    public int transferPenalty = 0;

    public double maxSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.

    /**
     * How much worse walking is than waiting for an equivalent length of time, as a multiplier.
     */
    public double walkReluctance = 2.0;

    /** Used instead of walk reluctance for stairs */
    public double stairsReluctance = 2.0;

    /**
     * How long does it take to get an elevator, on average (actually, it probably should 
     * be a bit *more* than average, to prevent optimistic trips)? Setting it to 
     * "seems like forever," while accurate, will probably prevent OTP from working correctly.
     */
    // TODO: how long does it /really/ take to get an elevator?
    public int elevatorBoardTime = 90;

    /**
     * What is the cost of boarding an elevator?
     */
    public int elevatorBoardCost = 90;

    /**
     * How long does it take to advance one floor on an elevator?
     */
    public int elevatorHopTime = 20;

    /**
     * What is the cost of travelling one floor on an elevator?
     */
    public int elevatorHopCost = 20;

    // it is assumed that getting off an elevator is completely free

    /**
     * Time to rent a bike
     */
    public int bikeRentalPickupTime = 60;

    /**
     * Cost of renting a bike. The cost is a bit more than actual time
     * to model the associated cost and trouble.
     */
    public int bikeRentalPickupCost = 120;

    /**
     * Time to drop-off a rented bike
     */
    public int bikeRentalDropoffTime = 30;

    /**
     * Cost of dropping-off a rented bike
     */
    public int bikeRentalDropoffCost = 30;

    /**
     * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a
     * multiplier. The default value treats wait and on-vehicle time as the same.
     * 
     * It may be tempting to set this as high as or higher than walkReluctance (as studies 
     * often find this kind of preferences among riders) but the planner will take this literally 
     * and walk down a transit line to avoid waiting at a stop.
     */
    public double waitReluctance = 0.95;

    /**
     * How much less bad is waiting at the beginning of the trip
     */
    public double waitAtBeginningFactor = 0.2;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    protected int walkBoardCost = 60 * 5;
    protected int bikeBoardCost = 60 * 10; // cyclists hate loading their bike a second time

    /**
     * Do not use certain named routes
     */
    public HashSet<RouteSpec> bannedRoutes = new HashSet<RouteSpec>();
    
    /**
     * Do not use certain trips
     */
    public HashSet<AgencyAndId> bannedTrips = new HashSet<AgencyAndId>();

    /**
     * Set of preferred routes by user.
     */
    public HashSet<RouteSpec> preferredRoutes = new HashSet<RouteSpec>();
    
    /**
     *  Penalty added for using every route that is not preferred if user set any route as preferred.
     *  We return number of seconds that we are willing to wait for preferred route.
     */
    public int useAnotherThanPreferredRoutesPenalty = 300;
    
    /**
     * Set of unpreferred routes for given user.
     */
    public HashSet<RouteSpec> unpreferredRoutes = new HashSet<RouteSpec>();

    /**
     *  Penalty added for using every unpreferred route. 
     *  We return number of seconds that we are willing to wait for preferred route.
     */
    public int useUnpreferredRoutesPenalty = 300;
    

    /**
     * The worst possible time (latest for depart-by and earliest for arrive-by) that we will accept
     * when planning a trip.
     */
    public long worstTime = Long.MAX_VALUE;

    /**
     * The worst possible weight that we will accept when planning a trip.
     */
    public double maxWeight = Double.MAX_VALUE;

    /**
     * A global minimum transfer time (in seconds) that specifies the minimum amount of time that
     * must pass between exiting one transit vehicle and boarding another. This time is in addition
     * to time it might take to walk between transit stops. This time should also be overridden by
     * specific transfer timing information in transfers.txt
     */
    // initialize to zero so this does not inadvertently affect tests
    // let Planner handle defaults
    public int minTransferTime = 0;

    public int maxTransfers = 2;

    /**
     * Set a hard limit on computation time. Any positive value will be treated as a limit on the
     * computation time for one search instance, in milliseconds relative to search start time. 
     * A zero or negative value implies no limit.
     */
    public long maxComputationTime = 0;

    /**
     * The search will be aborted if it is still running after this time (in milliseconds since the 
     * epoch). A negative or zero value implies no limit. 
     * This provides an absolute timeout, whereas the maxComputationTime is relative to the 
     * beginning of an individual search. While the two might seem equivalent, we trigger search 
     * retries in various places where it is difficult to update relative timeout value. 
     * The earlier of the two timeouts is applied. 
     */
    public long searchAbortTime = 0;
    
    private TraverseOptions walkingOptions;
    
    public GenericAStarFactory aStarSearchFactory = null;
    
    public RemainingWeightHeuristic remainingWeightHeuristic = new DefaultRemainingWeightHeuristic();

    public ExtraEdgesStrategy extraEdgesStrategy = new DefaultExtraEdgesStrategy();

    /**
     * Extensions to the trip planner will require additional traversal options beyond the default
     * set. We provide an extension point for adding arbitrary parameters with an extension-specific
     * key.
     */
    private Map<Object, Object> extensions = new HashMap<Object, Object>();

    private TransferTable transferTable;

    public int nonpreferredTransferPenalty = 120; /* penalty for using a non-preferred transfer */
    

    /**
     * With this flag, you can selectively enable or disable the use of the {@link #serviceDays}
     * cache. It is enabled by default, but you can disable it if you don't need this functionality.
     */
    public boolean useServiceDays = true;

    /*
     * Cache lists of which transit services run on which midnight-to-midnight periods This ties a
     * TraverseOptions to a particular start time for the duration of a search so the same options
     * cannot be used for multiple searches concurrently. To do so this cache would need to be moved
     * into StateData, with all that entails.
     */
    public ArrayList<ServiceDay> serviceDays;
    
    /**
     * This is true when a GraphPath is being traversed in reverse for optimization purposes.
     */
    private boolean reverseOptimizing = false;

    /** 
     * For the bike triangle, how important time is.  
     * triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1 
     */
    private double triangleTimeFactor;
    /** For the bike triangle, how important slope is */
    private double triangleSlopeFactor;
    /** For the bike triangle, how important safety is */
    private double triangleSafetyFactor;

    /** Constructor for options; modes defaults to walk and transit */
    public TraverseOptions() {
        // http://en.wikipedia.org/wiki/Walking
        walkSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        bikeSpeed = 5; // 5 m/s, ~11 mph, a random bicycling speed.
        carSpeed = 15; // 15 m/s, ~35 mph, a random driving speed
        setModes(new TraverseModeSet(new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT }));
        calendar = Calendar.getInstance();
        walkingOptions = this;
    }

    public TraverseOptions(TraverseModeSet modes) {
        this();
        this.setModes(modes);
    }

    public TraverseOptions(GtfsContext context) {
        this();
        setGtfsContext(context);
    }

    public TraverseOptions(TraverseMode mode) {
    	this();
    	this.setModes(new TraverseModeSet(mode));
	}

    public TraverseOptions(TraverseMode mode, OptimizeType optimize) {
    	this(new TraverseModeSet(mode), optimize);
	}

    public TraverseOptions(TraverseModeSet modeSet, OptimizeType optimize) {
    	this();
        this.optimizeFor = optimize;
    	this.setModes(modeSet);
	}

	public void setGtfsContext(GtfsContext context) {
        calendarService = context.getCalendarService();
    }

    public void setCalendarService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public CalendarService getCalendarService() {
        return calendarService;
    }

    public boolean serviceOn(AgencyAndId serviceId, ServiceDate serviceDate) {
        Set<ServiceDate> dates = serviceDatesByServiceId.get(serviceId);
        if (dates == null) {
            dates = calendarService.getServiceDatesForServiceId(serviceId);
            serviceDatesByServiceId.put(serviceId, dates);
        }
        return dates.contains(serviceDate);
    }

    public boolean transitAllowed() {
        return getModes().getTransit();
    }

    @SuppressWarnings("unchecked")
    @Override
    public TraverseOptions clone() {
        try {
            TraverseOptions clone = (TraverseOptions) super.clone();
            clone.bannedRoutes = (HashSet<RouteSpec>) bannedRoutes.clone();
            clone.bannedTrips = (HashSet<AgencyAndId>) bannedTrips.clone();
            if (this.walkingOptions != this)
            	clone.walkingOptions = this.walkingOptions.clone();
            else
            	clone.walkingOptions = clone;
            return clone;
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e);
        }
    }

    public TraverseOptions reversedClone() {
    	TraverseOptions ret = this.clone();
    	ret.setArriveBy( ! ret.isArriveBy());
    	ret.reverseOptimizing = ! ret.reverseOptimizing;
    	return ret;
    }
    
    public boolean equals(Object o) {
        if (o instanceof TraverseOptions) {
            TraverseOptions to = (TraverseOptions) o;
            return walkSpeed == to.walkSpeed && bikeSpeed == to.bikeSpeed && carSpeed == to.carSpeed
                    && maxWeight == to.maxWeight && worstTime == to.worstTime
                    && getModes().equals(to.getModes()) && isArriveBy() == to.isArriveBy()
                    && wheelchairAccessible == to.wheelchairAccessible
                    && optimizeFor == to.optimizeFor && maxWalkDistance == to.maxWalkDistance
                    && transferPenalty == to.transferPenalty
                    && maxSlope == to.maxSlope && walkReluctance == to.walkReluctance
                    && waitReluctance == to.waitReluctance
                    && walkBoardCost == to.walkBoardCost && bikeBoardCost == to.bikeBoardCost
                    && bannedRoutes.equals(to.bannedRoutes)
                    && bannedTrips.equals(to.bannedTrips)
                    && minTransferTime == to.minTransferTime
                    && nonpreferredTransferPenalty == to.nonpreferredTransferPenalty
                    && transferPenalty == to.transferPenalty
                    && triangleSafetyFactor == to.triangleSafetyFactor
                    && triangleSlopeFactor == to.triangleSlopeFactor
                    && triangleTimeFactor == to.triangleTimeFactor
                    && stairsReluctance == to.stairsReluctance;
        }
        return false;
    }

    public int hashCode() {
        return new Double(walkSpeed).hashCode() + new Double(bikeSpeed).hashCode()
                + new Double(carSpeed).hashCode() + new Double(maxWeight).hashCode()
                + (int) (worstTime & 0xffffffff) + getModes().hashCode()
                + (isArriveBy() ? 8966786 : 0) + (wheelchairAccessible ? 731980 : 0)
                + optimizeFor.hashCode() + new Double(maxWalkDistance).hashCode()
                + new Double(transferPenalty).hashCode() + new Double(maxSlope).hashCode()
                + new Double(walkReluctance).hashCode() + new Double(waitReluctance).hashCode()
                + walkBoardCost + bikeBoardCost + bannedRoutes.hashCode()
                + bannedTrips.hashCode() * 1373 + minTransferTime * 20996011
                + (int) nonpreferredTransferPenalty + (int) transferPenalty * 163013803
                + new Double(triangleSafetyFactor).hashCode() * 195233277
                + new Double(triangleSlopeFactor).hashCode() * 136372361
                + new Double(triangleTimeFactor).hashCode() * 790052899
                + new Double(stairsReluctance).hashCode() * 315595321;
    }

    public void setArriveBy(boolean back) {
        this.back = back;
        walkingOptions.back = back;
        if (back) {
            this.worstTime = 0;
        } else {
            this.worstTime = Long.MAX_VALUE;
        }
    }

    public boolean isArriveBy() {
        return back;
    }

    public TraverseOptions getWalkingOptions() {
        return walkingOptions;
    }

    public void setMode(TraverseMode mode) {
        setModes(new TraverseModeSet(mode));
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        if (modes.getBicycle()) {
            walkingOptions = new TraverseOptions();
            walkingOptions.setArriveBy(this.isArriveBy());
            walkingOptions.maxWalkDistance = maxWalkDistance;
            walkingOptions.walkSpeed *= 0.3; //assume walking bikes is slow
            walkingOptions.optimizeFor = optimizeFor;
        } else if (modes.getCar()) {
            walkingOptions = new TraverseOptions();
            walkingOptions.setArriveBy(this.isArriveBy());
            walkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public TraverseModeSet getModes() {
        return modes;
    }

    public void setOptimize(OptimizeType optimize) {
        optimizeFor = optimize;
        walkingOptions.optimizeFor = optimize;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    /**
     * only allow traversal by the specified mode; don't allow walking bikes. This is used during
     * contraction to reduce the number of possible paths.
     */
    public void freezeTraverseMode() {
        walkingOptions = clone();
        walkingOptions.walkingOptions = new TraverseOptions(new TraverseModeSet());
    }

    /**
     * Add an extension parameter with the specified key. Extensions allow you to add arbitrary
     * traversal options.
     * 
     * @param key
     * @param value
     */
    public void putExtension(Object key, Object value) {
        extensions.put(key, value);
    }

    /**
     * Determine if a particular extension parameter is present for the specified key.
     * 
     * @param key
     * @return
     */
    public boolean containsExtension(Object key) {
        return extensions.containsKey(key);
    }

    /**
     * Get the extension parameter with the specified key.
     * 
     * @param <T>
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Object key) {
        return (T) extensions.get(key);
    }

    public TransferTable getTransferTable() {
        return transferTable;
    }

    public void setTransferTable(TransferTable transferTable) {
        this.transferTable = transferTable;
    }

    /* must be called after traverseoptions already has a calendarservice set */
    public void setServiceDays(long time, Collection<String> agencies) {
        if( ! useServiceDays )
            return;
        final long SEC_IN_DAY = 60 * 60 * 24;

        this.serviceDays = new ArrayList<ServiceDay>(3);
        CalendarService cs = this.getCalendarService();
        if (cs == null) {
            _log.warn("TraverseOptions has no CalendarService or GTFSContext. Transit will never be boarded.");
            return;
        }
        // This should be a valid way to find yesterday and tomorrow,
        // since DST changes more than one hour after midnight in US/EU.
        // But is this true everywhere?
        for (String agency : agencies) {
            addIfNotExists(this.serviceDays, new ServiceDay(time - SEC_IN_DAY, cs, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time, cs, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time + SEC_IN_DAY, cs, agency));
        }
    }
    
    private static<T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    public boolean isReverseOptimizing() {
        return reverseOptimizing;
    }

    public double getMaxWalkDistance() {
        return maxWalkDistance;
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        this.maxWalkDistance = maxWalkDistance;
        if (walkingOptions != null) {
            walkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public final static int MIN_SIMILARITY = 1000;

    public int similarity(TraverseOptions options) {
        int s = 0;

        // TODO Check this: perfect equality between non-transit modes.
        // For partial equality, should we return a smaller similarity score?
        if (getModes().getNonTransitSet().equals(options.getModes().getNonTransitSet())) {
            s += 1000;
        }
        if(optimizeFor == options.optimizeFor) {
            s += 700;
        }
        if(wheelchairAccessible == options.wheelchairAccessible) {
            s += 500;
        }

        return s;
    }

    public double getTriangleSafetyFactor() {
        return triangleSafetyFactor;
    }

    public void setTriangleSafetyFactor(double triangleSafetyFactor) {
        this.triangleSafetyFactor = triangleSafetyFactor;
        walkingOptions.triangleSafetyFactor = triangleSafetyFactor;
    }

    public double getTriangleSlopeFactor() {
        return triangleSlopeFactor;
    }

    public void setTriangleSlopeFactor(double triangleSlopeFactor) {
        this.triangleSlopeFactor = triangleSlopeFactor;
        walkingOptions.triangleSlopeFactor = triangleSlopeFactor;
    }

    public double getTriangleTimeFactor() {
        return triangleTimeFactor;
    }

    public void setTriangleTimeFactor(double triangleTimeFactor) {
        this.triangleTimeFactor = triangleTimeFactor;
        walkingOptions.triangleTimeFactor = triangleTimeFactor;
    }

    /**
     * @param mode
     * @return The road speed for a specific traverse mode.
     */
    public double getSpeed(TraverseMode mode) {
        if (mode == TraverseMode.WALK)
            return walkSpeed;
        if (mode == TraverseMode.BICYCLE)
            return bikeSpeed;
        if (mode == TraverseMode.CAR)
            return carSpeed;
        throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    }

    /**
     * @return The highest speed for all possible road-modes.
     */
    public double getSpeedUpperBound() {
        // Assume carSpeed > bikeSpeed > walkSpeed
        if (modes.getCar())
            return carSpeed;
        if (modes.getBicycle())
            return bikeSpeed;
        return walkSpeed;
    }

    public void setWalkSpeed(double walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public void setBikeSpeed(double bikeSpeed) {
        this.bikeSpeed = bikeSpeed;
    }

    public void setCarSpeed(double carSpeed) {
        this.carSpeed = carSpeed;
    }

    /**
     * @param mode
     * @return The board cost for a specific traverse mode.
     */
    public int getBoardCost(TraverseMode mode) {
        if (mode == TraverseMode.BICYCLE)
            return bikeBoardCost;
        // I assume you can't bring your car in the bus
        return walkBoardCost;
    }

    /**
     * @return The lower boarding cost for all possible road-modes.
     */
    public int getBoardCostLowerBound() {
        // Assume walkBoardCost < bikeBoardCost
        if (modes.getWalk())
            return walkBoardCost;
        return bikeBoardCost;
    }

    public void setWalkBoardCost(int walkBoardCost) {
        this.walkBoardCost = walkBoardCost;
    }

    public void setBikeBoardCost(int bikeBoardCost) {
        this.bikeBoardCost = bikeBoardCost;
    }
}

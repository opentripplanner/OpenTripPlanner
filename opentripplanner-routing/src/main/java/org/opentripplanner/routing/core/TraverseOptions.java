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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class TraverseOptions implements Serializable, Cloneable {

    private static final long serialVersionUID = 3836092451659658815L;

    private static final Logger _log = LoggerFactory.getLogger(TraverseOptions.class);

    /** max speed along streets, in meters per second */
    public double speed;

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
    public double maxWalkDistance = Double.MAX_VALUE;

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
    public long optimizeTransferPenalty = 1800;

    public double maxSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.

    /**
     * How much worse walking is than waiting for an equivalent length of time, as a multiplier.
     */
    public double walkReluctance = 2.0;

    /**
     * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a
     * multiplier. The default value treats wait and on-vehicle time as the same.
     * 
     * It may be tempting to set this as high as or higher than walkReluctance (as studies 
     * often find this kind of preferences among riders) but the planner will take this literally 
     * and walk down a transit line to avoid waiting at a stop.
     */
    public double waitReluctance = 1.0;

    /**
     * How much less bad is waiting at the beginning of the trip
     */
    public double waitAtBeginningFactor = 0.1;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    public int boardCost = 60 * 2;

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
    public long useAnotherThanPreferredRoutesPenalty = 300;
    
    /**
     * Set of unpreferred routes for given user.
     */
    public HashSet<RouteSpec> unpreferredRoutes = new HashSet<RouteSpec>();

    /**
     *  Penalty added for using every unpreferred route. 
     *  We return number of seconds that we are willing to wait for preferred route.
     */
    public long useUnpreferredRoutesPenalty = 300;
    

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
    public int minTransferTime = 0;

    public int maxTransfers = 2;

    /**
     * Set a hard limit on computation time. Any positive value will be treated as a limit in the
     * computation time for one search instance, in milliseconds. A zero or negative value implies
     * no limit.
     */
    public long maxComputationTime = 0;

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

    public long baseTransferPenalty = 120; /* penalty for using a non-preferred transfer */
    

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
    public List<ServiceDay> serviceDays;
    
    /**
     * This is true when a GraphPath is being traversed in reverse for optimization purposes.
     */
    private boolean reverseOptimizing = false;

    /** Constructor for options; modes defaults to walk and transit */
    public TraverseOptions() {
        // http://en.wikipedia.org/wiki/Walking
        speed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
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
            return speed == to.speed && maxWeight == to.maxWeight && worstTime == to.worstTime
                    && getModes().equals(to.getModes()) && isArriveBy() == to.isArriveBy()
                    && wheelchairAccessible == to.wheelchairAccessible
                    && optimizeFor == to.optimizeFor && maxWalkDistance == to.maxWalkDistance
                    && optimizeTransferPenalty == to.optimizeTransferPenalty
                    && maxSlope == to.maxSlope && walkReluctance == to.walkReluctance
                    && waitReluctance == to.waitReluctance && boardCost == to.boardCost
                    && bannedRoutes.equals(to.bannedRoutes)
                    && bannedTrips.equals(to.bannedTrips)
                    && minTransferTime == to.minTransferTime
                    && baseTransferPenalty == to.baseTransferPenalty;
        }
        return false;
    }

    public int hashCode() {
        return new Double(speed).hashCode() + new Double(maxWeight).hashCode()
                + (int) (worstTime & 0xffffffff) + getModes().hashCode()
                + (isArriveBy() ? 8966786 : 0) + (wheelchairAccessible ? 731980 : 0)
                + optimizeFor.hashCode() + new Double(maxWalkDistance).hashCode()
                + new Double(optimizeTransferPenalty).hashCode() + new Double(maxSlope).hashCode()
                + new Double(walkReluctance).hashCode() + new Double(waitReluctance).hashCode()
                + boardCost + bannedRoutes.hashCode() + bannedTrips.hashCode() * 1373 + 
                + minTransferTime * 20996011 + (int) baseTransferPenalty;
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

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        if (modes.getBicycle()) {
            speed = 5; // 5 m/s, ~11 mph, a random bicycling speed.
            boardCost = 240; // cyclists hate loading their bike a second time
            walkingOptions = new TraverseOptions();
            walkingOptions.getModes().setTransit(false);
            walkingOptions.setArriveBy(this.isArriveBy());
        } else if (modes.getCar()) {
            speed = 15; // 15 m/s, ~35 mph, a random driving speed
            walkingOptions = new TraverseOptions();
            walkingOptions.getModes().setTransit(false);
            walkingOptions.setArriveBy(this.isArriveBy());
        }
    }

    public TraverseModeSet getModes() {
        return modes;
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

    public void setServiceDays(long time) {
        if( ! useServiceDays )
            return;
        final long MSEC_IN_DAY = 1000 * 60 * 60 * 24;
        this.serviceDays = new ArrayList<ServiceDay>(3);
        CalendarService cs = this.getCalendarService();
        if (cs == null) {
            _log.warn("TraverseOptions has no CalendarService or GTFSContext. Transit will never be boarded.");
            return;
        }
        // This should be a valid way to find yesterday and tomorrow,
        // since DST changes more than one hour after midnight in US/EU.
        // But is this true everywhere?
        this.serviceDays.add(new ServiceDay(time - MSEC_IN_DAY, cs));
        this.serviceDays.add(new ServiceDay(time, cs));
        this.serviceDays.add(new ServiceDay(time + MSEC_IN_DAY, cs));
    }
    
    public boolean isReverseOptimizing() {
        return reverseOptimizing;
    }
}
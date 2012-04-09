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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.algorithm.strategies.DefaultExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries. For example, maxWalkDistance may be relaxed if the alternative is to not provide a
 * route.
 * 
 * NOTE this is the result of merging what used to be called a REQUEST and a TRAVERSEOPTIONS
 */
public class TraverseOptions implements Cloneable, Serializable {
    
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    private static final Logger LOG = LoggerFactory.getLogger(TraverseOptions.class);
    private static final int CLAMP_ITINERARIES = 3;
    private static final int CLAMP_TRANSFERS = 4;

    /* NEW FIELDS */
    
    public Graph graph;
    public Vertex fromVertex;
    public Vertex toVertex;
    boolean initialized = false;
    
    /* EX-REQUEST FIELDS */

    /** The complete list of incoming query parameters. */
    public final HashMap<String, String> parameters = new HashMap<String, String>();
    /** The router ID -- internal ID to switch between router implementation (or graphs) */
    public String routerId;
    /** The start location -- either a Vertex name or latitude, longitude in degrees */
    // TODO change this to Doubles and a Vertex
    public String from;
    /** The start location's user-visible name */
    public String fromName;
    /** The end location (see the from field for format). */
    public String to;
    /** The end location's user-visible name */
    public String toName;
    /** An unordered list of intermediate locations to be visited (see the from field for format). */
    public List<NamedPlace> intermediatePlaces;
    /** The maximum distance (in meters) the user is willing to walk. Defaults to 1/2 mile. */
    public double maxWalkDistance = Double.MAX_VALUE;
    /** The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT. */
    public TraverseModeSet modes = new TraverseModeSet("TRANSIT,WALK"); // defaults in constructor
    /** The set of characteristics that the user wants to optimize for -- defaults to QUICK, or optimize for transit time. */
    public OptimizeType optimize = OptimizeType.QUICK;
    /** The date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    public Date dateTime = new Date();
    /** Whether the trip should depart at dateTime (false, the default), or arrive at dateTime. */
    public boolean arriveBy = false;
    /** Whether the trip must be wheelchair accessible. */
    public boolean wheelchair = false;
    /** The maximum number of possible itineraries to return. */
    public int numItineraries = 3;
    /** The maximum slope of streets for wheelchair trips. */
    public double maxSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.
    /** Whether the planner should return intermediate stops lists for transit legs. */
    public boolean showIntermediateStops = false;
    
    /** max walk/bike speed along streets, in meters per second */
    public double speed; // walkSpeed
    // public double walkSpeed;

    /** max biking speed along streets, in meters per second */
    // public double bikeSpeed;
    
    public boolean intermediatePlacesOrdered;
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

    /* ORIGNAL TRAVERSEOPTIONS FIELDS */
    
    public Calendar calendar;

    public CalendarService calendarService;

    public Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();

    public boolean wheelchairAccessible = false;

    public OptimizeType optimizeFor = OptimizeType.QUICK;

    /** How much worse walking is than waiting for an equivalent length of time, as a multiplier */
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

    /** What is the cost of boarding an elevator? */
    public int elevatorBoardCost = 90;

    /** How long does it take to advance one floor on an elevator? */
    public int elevatorHopTime = 20;

    /** What is the cost of travelling one floor on an elevator? */
    public int elevatorHopCost = 20;

    // it is assumed that getting off an elevator is completely free

    /**
     * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a
     * multiplier. The default value treats wait and on-vehicle time as the same.
     * 
     * It may be tempting to set this as high as or higher than walkReluctance (as studies 
     * often find this kind of preferences among riders) but the planner will take this literally 
     * and walk down a transit line to avoid waiting at a stop.
     */
    public double waitReluctance = 0.95;

    /** How much less bad is waiting at the beginning of the trip (replaces waitReluctance) */
    public double waitAtBeginningFactor = 0.2;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    public int boardCost = 60 * 5;

    /** Do not use certain named routes */
    public HashSet<RouteSpec> bannedRoutes = new HashSet<RouteSpec>();
    
    /** Do not use certain trips */
    public HashSet<AgencyAndId> bannedTrips = new HashSet<AgencyAndId>();

    /** Set of preferred routes by user. */
    public HashSet<RouteSpec> preferredRoutes = new HashSet<RouteSpec>();
    
    /**
     *  Penalty added for using every route that is not preferred if user set any route as preferred.
     *  We return number of seconds that we are willing to wait for preferred route.
     */
    public int useAnotherThanPreferredRoutesPenalty = 300;
    
    /** Set of unpreferred routes for given user. */
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

    /** The worst possible weight that we will accept when planning a trip. */
    public double maxWeight = Double.MAX_VALUE;

    /**
     * A global minimum transfer time (in seconds) that specifies the minimum amount of time that
     * must pass between exiting one transit vehicle and boarding another. This time is in addition
     * to time it might take to walk between transit stops. This time should also be overridden by
     * specific transfer timing information in transfers.txt
     */
    // initialize to zero so this does not inadvertently affect tests, and let Planner handle defaults
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
    
    /**
     * Extensions to the trip planner will require additional traversal options beyond the default
     * set. We provide an extension point for adding arbitrary parameters with an extension-specific
     * key.
     */
    public Map<Object, Object> extensions = new HashMap<Object, Object>();

    /** Penalty for using a non-preferred transfer */
    public int nonpreferredTransferPenalty = 120; 

    /** 
     * For the bike triangle, how important time is.  
     * triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1 
     */
    public double triangleTimeFactor;
    /** For the bike triangle, how important slope is */
    public double triangleSlopeFactor;
    /** For the bike triangle, how important safety is */
    public double triangleSafetyFactor;

    
    /* FIELDS FOR SEARCH CONTEXT AND CACHED VALUES */

    public TraverseOptions walkingOptions;

    public GenericAStarFactory aStarSearchFactory = null;
    
    public RemainingWeightHeuristic remainingWeightHeuristic = new DefaultRemainingWeightHeuristic();
    
    public ExtraEdgesStrategy extraEdgesStrategy = new DefaultExtraEdgesStrategy();

    public TransferTable transferTable;

    /**
     * With this flag, you can selectively enable or disable the use of the {@link #serviceDays}
     * cache. It is enabled by default, but you can disable it if you don't need this functionality.
     */
    public boolean useServiceDays = true;
    
    /**
     * Cache lists of which transit services run on which midnight-to-midnight periods This ties a
     * TraverseOptions to a particular start time for the duration of a search so the same options
     * cannot be used for multiple searches concurrently. To do so this cache would need to be moved
     * into StateData, with all that entails.
     */
    public ArrayList<ServiceDay> serviceDays;
    
    /** This is true when a GraphPath is being traversed in reverse for optimization purposes. */
    public boolean reverseOptimizing = false;
    
    /* CONSTRUCTORS */
    
    /** Constructor for options; modes defaults to walk and transit */
    public TraverseOptions() {
        // http://en.wikipedia.org/wiki/Walking
        speed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        setModes(new TraverseModeSet(new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT }));
        calendar = Calendar.getInstance();
        walkingOptions = this;
        setIntermediatePlaces(new ArrayList<String>());
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

    /* ACCESSOR METHODS */
    
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
        return getModes().isTransit();
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

    public TraverseOptions reversedClone(long finalTime) {
    	TraverseOptions ret = this.clone();
    	ret.setArriveBy( ! ret.isArriveBy());
    	ret.reverseOptimizing = ! ret.reverseOptimizing; // this is not strictly correct
    	ret.dateTime = new Date(finalTime);
    	return ret;
    }
    

    public void setArriveBy(boolean arriveBy) {
        this.arriveBy = arriveBy;
        walkingOptions.arriveBy = arriveBy;
        if (arriveBy) {
            this.worstTime = 0;
        } else {
            this.worstTime = Long.MAX_VALUE;
        }
    }

    public TraverseOptions getWalkingOptions() {
        return walkingOptions;
    }

    public void setMode(TraverseMode mode) {
        setModes(new TraverseModeSet(mode));
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        assert (modes.isValid());
        if (modes.getBicycle()) {
            speed = 5; // 5 m/s, ~11 mph, a random bicycling speed.
            boardCost = 10 * 60; // cyclists hate loading their bike a second time
            walkingOptions = new TraverseOptions();
            walkingOptions.setArriveBy(this.isArriveBy());
            walkingOptions.maxWalkDistance = maxWalkDistance;
            walkingOptions.speed *= 0.3; //assume walking bikes is slow
            walkingOptions.optimizeFor = optimizeFor;
        } else if (modes.getCar()) {
            speed = 15; // 15 m/s, ~35 mph, a random driving speed
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
     */
    public void putExtension(Object key, Object value) {
        extensions.put(key, value);
    }

    /** Determine if a particular extension parameter is present for the specified key. */
    public boolean containsExtension(Object key) {
        return extensions.containsKey(key);
    }

    /** Get the extension parameter with the specified key. */
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

    public boolean isReverseOptimizing() {
        return reverseOptimizing;
    }

    /** @return the (soft) maximum walk distance */
    public double getMaxWalkDistance() {
        return maxWalkDistance;
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        this.maxWalkDistance = maxWalkDistance;
        if (walkingOptions != null) {
            walkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public void setPreferredRoutes(String s) {
        preferredRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }
    
    public void setUnpreferredRoutes(String s) {
        unpreferredRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }

    public void setBannedRoutes(String s) {
        bannedRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }
    
    public final static int MIN_SIMILARITY = 1000;

    public int similarity(TraverseOptions options) {
        int s = 0;

        if(getModes().getNonTransitMode() == options.getModes().getNonTransitMode()) {
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


    
    /**** EX-REQUEST METHODS ****/
    public HashMap<String, String> getParameters() {
        return parameters;
    }

    public String getRouterId() {
        return routerId;
    }

    /** @param routerId the router ID, used to switch between router instances. */
    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getFrom() { return from; }

    // TODO factor out splitting code which appears in 3 places
    public void setFrom(String from) {
        if (from.contains("::")) {
            String[] parts = from.split("::");
            this.fromName = parts[0];
            this.from = parts[1];
        } else {
            this.from = from;
        }
    }

    public String getTo() { return to; }

    public void setTo(String to) {
        if (to.contains("::")) {
            String[] parts = to.split("::");
            this.toName = parts[0];
            this.to = parts[1];
        } else {
            this.to = to;
        }
    }
    
    /** return the vertex where this search will begin, accounting for arriveBy */
    public Vertex getOriginVertex() {
        return arriveBy ? toVertex : fromVertex;
    }
    
    /** return the vertex where this search will end, accounting for arriveBy */
    public Vertex getTargetVertex() {
        return arriveBy ? fromVertex : toVertex;
    }

    /** @param walk - the (soft) maximum walk distance to set */
    public void setMaxWalkDistance(Double walk) { this.maxWalkDistance = walk; }

    // TODO move this into TraverseModeSet
    public String getModesAsStr() {
        String retVal = null;
        for (TraverseMode m : modes.getModes()) {
            if (retVal == null)
                retVal = "";
            else
                retVal += ", ";
            retVal += m;
        }
        return retVal;
    }

    public void addMode(TraverseMode mode) { 
        modes.setMode(mode, true); 
    }

    public void addMode(List<TraverseMode> mList) {
        for (TraverseMode m : mList) {
            addMode(m);
        }
    }

    public OptimizeType getOptimize() { 
        return optimize; 
    }

    public Date getDateTime() { 
        return dateTime; 
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = new Date(dateTime.getTime());
    }

    public void setDateTime(String date, String time) {
        dateTime = DateUtils.toDate(date, time);
        LOG.debug("JVM default timezone is {}", TimeZone.getDefault());
        LOG.debug("Request datetime parsed as {}", dateTime);
    }

    public boolean isArriveBy() { 
        return arriveBy; 
    }

    public Integer getNumItineraries() { 
        return numItineraries; 
    }

    public void setNumItineraries(int numItineraries) {
        if (numItineraries > CLAMP_ITINERARIES) {
            numItineraries = CLAMP_ITINERARIES;
        } else if (numItineraries < 1) {
            numItineraries = 1;
        }
        this.numItineraries = numItineraries;
    }

    public String toHtmlString() {
        return toString("<br/>");
    }

    public String toString() {
        return toString(" ");
    }

    public String toString(String sep) {
        return getFrom() + sep + getTo() + sep + getMaxWalkDistance() + sep + getDateTime() + sep
                + isArriveBy() + sep + getOptimize() + sep + getModesAsStr() + sep
                + getNumItineraries();
    }
    
    public TraverseModeSet getModeSet() { 
        return modes; 
    }

    public void removeMode(TraverseMode mode) { 
        modes.setMode(mode, false); 
    }

    /** Set whether the trip must be wheelchair accessible */
    public void setWheelchair(boolean wheelchair) { this.wheelchair = wheelchair; }

    /** return whether the trip must be wheelchair accessible */
    public boolean getWheelchair() { return wheelchair; }

    public void setIntermediatePlaces(List<String> intermediates) {
        this.intermediatePlaces = new ArrayList<NamedPlace>(intermediates.size());
        for (String place : intermediates) {
            String name = place;
            if (place.contains("::")) {
                String[] parts = place.split("::");
                name = parts[0];
                place = parts[1];
            }
            NamedPlace intermediate = new NamedPlace(name, place);
            intermediatePlaces.add(intermediate);
        }
    }

    /**
     * @return the intermediatePlaces
     */
    public List<NamedPlace> getIntermediatePlaces() {
        return intermediatePlaces;
    }

    /**
     * @return the maximum street slope for wheelchair trips
     */
    public double getMaxSlope() {
        return maxSlope;
    }
    
    /**
     * @param maxSlope the maximum street slope for wheelchair trpis
     */
    public void setMaxSlope(double maxSlope) {
        this.maxSlope = maxSlope;
    }
    
    /** 
     * @param showIntermediateStops
     *          whether the planner should return intermediate stop lists for transit legs 
     */
    public void setShowIntermediateStops(boolean showIntermediateStops) {
        this.showIntermediateStops = showIntermediateStops;
    }

    /** 
     * @return whether the planner should return intermediate stop lists for transit legs 
     */
    public boolean getShowIntermediateStops() {
        return showIntermediateStops;
    }

    public void setMinTransferTime(Integer minTransferTime) {
        this.minTransferTime = minTransferTime;
    }
    
    public Integer getMinTransferTime() {
        return minTransferTime;
    }
    
    public void setTransferPenalty(Integer transferPenalty) {
        this.transferPenalty = transferPenalty;
    }

    public Integer getTransferPenalty() {
        return transferPenalty;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getWalkSpeed() {
        return this.speed;
    }

    public void setTriangleSafetyFactor(double triangleSafetyFactor) {
        this.triangleSafetyFactor = triangleSafetyFactor;
        walkingOptions.triangleSafetyFactor = triangleSafetyFactor;
    }

    public void setTriangleSlopeFactor(double triangleSlopeFactor) {
        this.triangleSlopeFactor = triangleSlopeFactor;
        walkingOptions.triangleSlopeFactor = triangleSlopeFactor;
    }

    public void setTriangleTimeFactor(double triangleTimeFactor) {
        this.triangleTimeFactor = triangleTimeFactor;
        walkingOptions.triangleTimeFactor = triangleTimeFactor;
    }

    public double getTriangleSafetyFactor() {
        return triangleSafetyFactor;
    }

    public double getTriangleSlopeFactor() {
        return triangleSlopeFactor;
    }

    public double getTriangleTimeFactor() {
        return triangleTimeFactor;
    }

    public void setMaxTransfers(int maxTransfers) {
        if (maxTransfers > CLAMP_TRANSFERS) {
            maxTransfers = CLAMP_TRANSFERS;
        }
        this.maxTransfers = maxTransfers;
    }

    public Integer getMaxTransfers() {
        return maxTransfers;
    }

    public void setIntermediatePlacesOrdered(boolean intermediatePlacesOrdered) {
        this.intermediatePlacesOrdered = intermediatePlacesOrdered;
    }

    public boolean isIntermediatePlacesOrdered() {
        return intermediatePlacesOrdered;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public NamedPlace getFromPlace() {
        return new NamedPlace(fromName, from);
    }

    public NamedPlace getToPlace() {
        return new NamedPlace(toName, to);
    }
    
    /* STATIC METHODS */
    
//    /**
//     * Build a request from the parameters in a concrete subclass of SearchResource.
//     */
//    public Request(SearchResource) {
//        
//    }

    /* INSTANCE METHODS */
    
    /**
     * Call when all search request parameters have been set to fill the service days cache,
     * set some other values.
     */
    // could even be setGraph
    public void prepareForSearch() {
        if (graph != null)
            throw new IllegalStateException("Graph must be set before preparing for search.");
        findEndpointVertices();
        setCalendarService(graph.getService(CalendarService.class));
        setTransferTable(graph.getTransferTable());
        setServiceDays();
        if (getModes().isTransit()
            && ! graph.transitFeedCovers(dateTime)) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
        // If transit is not to be used, disable walk limit and only search for one itinerary.
        if (! getModes().isTransit()) {
            numItineraries = 1;
            setMaxWalkDistance(Double.MAX_VALUE);
        }
        this.initialized = true;
    }
    
    public long getSecondsSinceEpoch() {
        return dateTime.getTime() / 1000;
    }
    
    private void findEndpointVertices() {
        if (graph == null)
            return;
        StreetVertexIndexService index = graph.getService(StreetVertexIndexService.class);
        if (index == null)
            return;
        ArrayList<String> notFound = new ArrayList<String>();
        Vertex fromVertex = index.getVertexForPlace(fromPlace, this);
        if (fromVertex == null) {
            notFound.add("from");
        }
        Vertex toVertex = getVertexForPlace(toPlace, options, fromVertex);
        if (toVertex == null) {
            notFound.add("to");
        }
        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }
        Vertex origin = null;
        Vertex target = null;
        if (options.isArriveBy()) {
            origin = toVertex;
            target = fromVertex;
        } else {
            origin = fromVertex;
            target = toVertex;
        }
    }

    /**
     *  Cache ServiceDay objects representing which services are running yesterday, today, and tomorrow relative
     *  to the search time. This information is very heavily used (at every transit boarding) and Date operations were
     *  identified as a performance bottleneck. Must be called after the TraverseOptions already has a CalendarService set. 
     */
    public void setServiceDays() {
        if( ! useServiceDays )
            return;
        final long SEC_IN_DAY = 60 * 60 * 24;

        final long time = this.getSecondsSinceEpoch();
        this.serviceDays = new ArrayList<ServiceDay>(3);
        CalendarService cs = this.getCalendarService();
        if (cs == null) {
            LOG.warn("TraverseOptions has no CalendarService or GTFSContext. Transit will never be boarded.");
            return;
        }
        // This should be a valid way to find yesterday and tomorrow,
        // since DST changes more than one hour after midnight in US/EU.
        // But is this true everywhere?
        for (String agency : graph.getAgencyIds()) {
            addIfNotExists(this.serviceDays, new ServiceDay(time - SEC_IN_DAY, cs, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time, cs, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(time + SEC_IN_DAY, cs, agency));
        }
    }

    /** Builds an initial State for a search based on this set of options. */
    public State getInitialState() {
        return new State(this);
    }
    
    private static<T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof TraverseOptions) {
            TraverseOptions to = (TraverseOptions) o;
            return speed == to.speed && maxWeight == to.maxWeight && worstTime == to.worstTime
                    && getModes().equals(to.getModes()) && isArriveBy() == to.isArriveBy()
                    && wheelchairAccessible == to.wheelchairAccessible
                    && optimizeFor == to.optimizeFor && maxWalkDistance == to.maxWalkDistance
                    && transferPenalty == to.transferPenalty
                    && maxSlope == to.maxSlope && walkReluctance == to.walkReluctance
                    && waitReluctance == to.waitReluctance && boardCost == to.boardCost
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
        return new Double(speed).hashCode() + new Double(maxWeight).hashCode()
                + (int) (worstTime & 0xffffffff) + getModes().hashCode()
                + (isArriveBy() ? 8966786 : 0) + (wheelchairAccessible ? 731980 : 0)
                + optimizeFor.hashCode() + new Double(maxWalkDistance).hashCode()
                + new Double(transferPenalty).hashCode() + new Double(maxSlope).hashCode()
                + new Double(walkReluctance).hashCode() + new Double(waitReluctance).hashCode()
                + boardCost + bannedRoutes.hashCode() + bannedTrips.hashCode() * 1373
                + minTransferTime * 20996011 + (int) nonpreferredTransferPenalty
                + (int) transferPenalty * 163013803 
                + new Double(triangleSafetyFactor).hashCode() * 195233277
                + new Double(triangleSlopeFactor).hashCode() * 136372361
                + new Double(triangleTimeFactor).hashCode() * 790052899
                + new Double(stairsReluctance).hashCode() * 315595321
                ;
    }

}

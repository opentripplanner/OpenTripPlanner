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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
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
public class RoutingRequest implements Cloneable, Serializable {
    
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    private static final Logger LOG = LoggerFactory.getLogger(RoutingRequest.class);
    private static final int CLAMP_ITINERARIES = 3;
    private static final int CLAMP_TRANSFERS = 4;

    
    /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */

    /* TODO no defaults should be set here, they should all be handled in one place (searchresource) */ 
    /** The complete list of incoming query parameters. */
    public final HashMap<String, String> parameters = new HashMap<String, String>();
    /** The router ID -- internal ID to switch between router implementation (or graphs) */
    public String routerId = "";
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
    public boolean intermediatePlacesOrdered;
    /** The maximum distance (in meters) the user is willing to walk. Defaults to 1/2 mile. */
    public double maxWalkDistance = Double.MAX_VALUE;
    /** The worst possible time (latest for depart-by and earliest for arrive-by) to accept */
    public long worstTime = Long.MAX_VALUE;
    /** The worst possible weight that we will accept when planning a trip. */
    public double maxWeight = Double.MAX_VALUE;
    /** The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT. */
    public TraverseModeSet modes = new TraverseModeSet("TRANSIT,WALK"); // defaults in constructor
    /** The set of characteristics that the user wants to optimize for -- defaults to QUICK, or optimize for transit time. */
    public OptimizeType optimize = OptimizeType.QUICK;
    /** The epoch date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    public long dateTime = new Date().getTime() / 1000;
    /** Whether the trip should depart at dateTime (false, the default), or arrive at dateTime. */
    public boolean arriveBy = false;
    /** Whether the trip must be wheelchair accessible. */
    public boolean wheelchairAccessible = false;
    /** The maximum number of possible itineraries to return. */
    public int numItineraries = 3;
    /** The maximum slope of streets for wheelchair trips. */
    public double maxSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.
    /** Whether the planner should return intermediate stops lists for transit legs. */
    public boolean showIntermediateStops = false;
    /** max walk/bike speed along streets, in meters per second */
    private double walkSpeed;
    private double bikeSpeed;
    private double carSpeed;    

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

    /** Time to rent a bike */
    public int bikeRentalPickupTime = 60;

    /** 
     * Cost of renting a bike. The cost is a bit more than actual time
     * to model the associated cost and trouble.
     */
    public int bikeRentalPickupCost = 120;

    /** Time to drop-off a rented bike */
    public int bikeRentalDropoffTime = 30;

    /** Cost of dropping-off a rented bike */
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

    /** How much less bad is waiting at the beginning of the trip (replaces waitReluctance) */
    public double waitAtBeginningFactor = 0.2;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    protected int walkBoardCost = 60 * 5;
    protected int bikeBoardCost = 60 * 10; // cyclists hate loading their bike a second time

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
     * A global minimum transfer time (in seconds) that specifies the minimum amount of time that
     * must pass between exiting one transit vehicle and boarding another. This time is in addition
     * to time it might take to walk between transit stops. This time should also be overridden by
     * specific transfer timing information in transfers.txt
     */
    // initialize to zero so this does not inadvertently affect tests, and let Planner handle defaults
    public int minTransferTime = 0;

    public int maxTransfers = 2;

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

    /** A sub-traverse options for another mode */
    public RoutingRequest walkingOptions;
    
    /** This is true when a GraphPath is being traversed in reverse for optimization purposes. */
    public boolean reverseOptimizing = false;

    /** when true, do not use goal direction or stop at the target, build a full SPT */
    public boolean batch = false;

    /** 
     * Whether or not bike rental availability information will be used
     * to plan bike rental trips
     */
    private boolean useBikeRentalAvailabilityInformation = false;

    /**
     * The routing context used to actually carry out this search. It is important to build States 
     * from TraverseOptions rather than RoutingContexts, and just keep a reference to the context 
     * in the TraverseOptions, rather than using RoutingContexts for everything because in some 
     * testing and graph building situations we need to build a bunch of initial states with 
     * different times and vertices from a single TraverseOptions, without setting all the transit 
     * context or building temporary vertices (with all the exception-throwing checks that entails).
     * 
     * While they are conceptually separate, TraverseOptions does maintain a reference to its 
     * accompanying RoutingContext (and vice versa) so that both do not need to be 
     * passed/injected separately into tight inner loops within routing algorithms. These references
     * should be set to null when the request scope is torn down -- the routing context becomes
     * irrelevant at that point, since temporary graph elements have been removed and the graph may
     * have been reloaded. 
     */
    public RoutingContext rctx;    
    
    
    /* CONSTRUCTORS */
    
    /** Constructor for options; modes defaults to walk and transit */
    public RoutingRequest() {
        // http://en.wikipedia.org/wiki/Walking
        walkSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        bikeSpeed = 5; // 5 m/s, ~11 mph, a random bicycling speed
        carSpeed = 15; // 15 m/s, ~35 mph, a random driving speed        
        setModes(new TraverseModeSet(new TraverseMode[] { TraverseMode.WALK, TraverseMode.TRANSIT }));
        walkingOptions = this;
    }

    public RoutingRequest(TraverseModeSet modes) {
        this();
        this.setModes(modes);
    }

    public RoutingRequest(TraverseMode mode) {
    	this();
    	this.setModes(new TraverseModeSet(mode));
    }

    public RoutingRequest(TraverseMode mode, OptimizeType optimize) {
    	this(new TraverseModeSet(mode), optimize);
    }

    public RoutingRequest(TraverseModeSet modeSet, OptimizeType optimize) {
    	this();
        this.optimize = optimize;
    	this.setModes(modeSet);
    }    

    
    /* ACCESSOR METHODS */

    public boolean transitAllowed() {
        return getModes().isTransit();
    }
    
    public long getSecondsSinceEpoch() {
        return dateTime;
    }

    public void setArriveBy(boolean arriveBy) {
        this.arriveBy = arriveBy;
        walkingOptions.arriveBy = arriveBy;
        if (worstTime == Long.MAX_VALUE || worstTime == 0)
            worstTime = arriveBy ? 0 : Long.MAX_VALUE;
    }

    public RoutingRequest getWalkingOptions() {
        return walkingOptions;
    }

    public void setMode(TraverseMode mode) {
        setModes(new TraverseModeSet(mode));
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        if (modes.getBicycle()) {
            walkingOptions = new RoutingRequest();
            walkingOptions.setArriveBy(this.isArriveBy());
            walkingOptions.maxWalkDistance = maxWalkDistance;
            walkingOptions.walkSpeed *= 0.3; //assume walking bikes is slow
            walkingOptions.optimize = optimize;
        } else if (modes.getCar()) {
            walkingOptions = new RoutingRequest();
            walkingOptions.setArriveBy(this.isArriveBy());
            walkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public TraverseModeSet getModes() {
        return modes;
    }

    public void setOptimize(OptimizeType optimize) {
        this.optimize = optimize;
        walkingOptions.optimize = optimize;
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
        walkingOptions.walkingOptions = new RoutingRequest(new TraverseModeSet());
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

    public boolean isReverseOptimizing() {
        return reverseOptimizing;
    }

    /** @return the (soft) maximum walk distance */
    // If transit is not to be used, disable walk limit.
    public double getMaxWalkDistance() {
        if (getModes().isTransit()) {
            return Double.MAX_VALUE;
        } else {
            return maxWalkDistance;
        }
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        this.maxWalkDistance = maxWalkDistance;
        if (walkingOptions != null) {
            walkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public void setPreferredRoutes(String s) {
        if (s != null && !s.equals(""))
            preferredRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }
    
    public void setUnpreferredRoutes(String s) {
        if (s != null && !s.equals(""))
            unpreferredRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }

    public void setBannedRoutes(String s) {
        if (s != null && !s.equals(""))
            bannedRoutes = new HashSet<RouteSpec>(RouteSpec.listFromString(s));
    }
    
    public final static int MIN_SIMILARITY = 1000;

    public int similarity(RoutingRequest options) {
        int s = 0;

        // TODO Check this: perfect equality between non-transit modes.
        // For partial equality, should we return a smaller similarity score?
        if (getModes().getNonTransitSet().equals(options.getModes().getNonTransitSet())) {        
            s += 1000;
        }
        if(optimize == options.optimize) {
            s += 700;
        }
        if(wheelchairAccessible == options.wheelchairAccessible) {
            s += 500;
        }

        return s;
    }

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
    
    /** @param walk - the (soft) maximum walk distance to set */
    public void setMaxWalkDistance(Double walk) { this.maxWalkDistance = walk; }

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
        return new Date(dateTime * 1000); 
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime.getTime() / 1000;
        LOG.debug("JVM default timezone is {}", TimeZone.getDefault());
        LOG.debug("Request datetime parsed as {}", dateTime);
    }

    public void setDateTime(String date, String time) {
        Date dateObject = DateUtils.toDate(date, time);
        setDateTime(dateObject);
    }

    public boolean isArriveBy() { 
        return arriveBy; 
    }

    public Integer getNumItineraries() { 
        if (getModes().isTransit()) {
            return numItineraries;
        } else {
            // If transit is not to be used, only search for one itinerary.
            return 1;
        }
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
                + isArriveBy() + sep + getOptimize() + sep + modes.getAsStr() + sep
                + getNumItineraries();
    }
    
    public TraverseModeSet getModeSet() { 
        return modes; 
    }

    public void removeMode(TraverseMode mode) { 
        modes.setMode(mode, false); 
    }

    /** Set whether the trip must be wheelchair accessible */
    public void setWheelchair(boolean wheelchair) { 
        this.wheelchairAccessible = wheelchair; 
    }

    /** return whether the trip must be wheelchair accessible */
    public boolean getWheelchairAccessible() { 
        return wheelchairAccessible; 
    }

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

    public void setBatch(boolean batch) {
        this.batch = batch;
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
    
    
    /* INSTANCE METHODS */

    @SuppressWarnings("unchecked")
    @Override
    public RoutingRequest clone() {
        try {
            RoutingRequest clone = (RoutingRequest) super.clone();
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
    
    /** @param finalTime in seconds since the epoch */
    public RoutingRequest reversedClone() {
        RoutingRequest ret = this.clone();
        ret.setArriveBy( ! ret.isArriveBy());
        ret.reverseOptimizing = ! ret.reverseOptimizing; // this is not strictly correct
        return ret;
    }

    public void setRoutingContext (Graph graph) {
        if (rctx == null) {
            this.rctx = new RoutingContext(this, graph); // graphService.getGraph(routerId)
            // check after reference established to allow temp edge cleanup on exception
            this.rctx.check();
        } else {
            if (rctx.graph == graph) {
                LOG.debug("keeping existing routing context");
                return;
            } else {
                LOG.error("attempted to reset routing context using a different graph");
                return;
            }
        }
    }
    
    /** For use in tests. Force RoutingContext to specific vertices rather than making temp edges. */
    public void setRoutingContext (Graph graph, Vertex from, Vertex to) {
        // normally you would want to tear down the routing context...
        // but this method is mostly used in tests, and teardown interferes with testHalfEdges
        // FIXME here, or in test, and/or in other places like TSP that use this method
        // if (rctx != null)
        //    this.rctx.destroy();
        this.rctx = new RoutingContext(this, graph, from, to);
    }

    /** For use in tests. Force RoutingContext to specific vertices rather than making temp edges. */
    public void setRoutingContext (Graph graph, String from, String to) {
        this.setRoutingContext(graph, graph.getVertex(from), graph.getVertex(to));
    }

    public RoutingContext getRoutingContext () {
        return this.rctx;
    }

    /** Equality and hashCode should not consider the routing context, to allow SPT caching. */
    @Override
    public boolean equals(Object o) {
        if (o instanceof RoutingRequest) {
            RoutingRequest other = (RoutingRequest) o;
            return from.equals(other.from) && to.equals(other.to)
                    && walkSpeed == other.walkSpeed && bikeSpeed == other.bikeSpeed 
                    && carSpeed == other.carSpeed
                    && maxWeight == other.maxWeight && worstTime == other.worstTime
                    && getModes().equals(other.getModes()) && isArriveBy() == other.isArriveBy()
                    && wheelchairAccessible == other.wheelchairAccessible
                    && optimize == other.optimize && maxWalkDistance == other.maxWalkDistance
                    && transferPenalty == other.transferPenalty
                    && maxSlope == other.maxSlope && walkReluctance == other.walkReluctance
                    && waitReluctance == other.waitReluctance 
                    && walkBoardCost == other.walkBoardCost && bikeBoardCost == other.bikeBoardCost
                    && bannedRoutes.equals(other.bannedRoutes)
                    && bannedTrips.equals(other.bannedTrips)
                    && minTransferTime == other.minTransferTime
                    && nonpreferredTransferPenalty == other.nonpreferredTransferPenalty
                    && transferPenalty == other.transferPenalty
                    && triangleSafetyFactor == other.triangleSafetyFactor
                    && triangleSlopeFactor == other.triangleSlopeFactor
                    && triangleTimeFactor == other.triangleTimeFactor
                    && stairsReluctance == other.stairsReluctance;
        }
        return false;
    }

    /** Equality and hashCode should not consider the routing context, to allow SPT caching. */
    @Override
    public int hashCode() {
        return from.hashCode() * 524287 + to.hashCode() * 1327144003 
                + new Double(walkSpeed).hashCode() + new Double(bikeSpeed).hashCode() 
                + new Double(carSpeed).hashCode() + new Double(maxWeight).hashCode()
                + (int) (worstTime & 0xffffffff) + getModes().hashCode()
                + (isArriveBy() ? 8966786 : 0) + (wheelchairAccessible ? 731980 : 0)
                + optimize.hashCode() + new Double(maxWalkDistance).hashCode()
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

    /** Tear down any routing context (remove temporary edges from edge lists) */
    public void cleanup() {
        if (this.rctx == null)
            LOG.warn("routing context was not set, cannot destroy it.");
        else {
            int nRemoved = this.rctx.destroy();
            LOG.debug("routing context destroyed ({} temporary edges removed)", nRemoved);
        }        
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

    /** @return The highest speed for all possible road-modes. */
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

    /** @return The lower boarding cost for all possible road-modes. */
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

    public boolean useBikeRentalAvailabilityInformation() {
        return useBikeRentalAvailabilityInformation;
    }

    public void setUseBikeRentalAvailabilityInformation(boolean useBikeRentalAvailabilityInformation) {
        this.useBikeRentalAvailabilityInformation = useBikeRentalAvailabilityInformation;
    }

}

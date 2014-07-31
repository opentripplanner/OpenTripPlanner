package org.opentripplanner.profile;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.impl.StopTimeArray;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.param.LatLon;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class ProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileRouter.class);

    /* Search configuration constants */
    public static final int SLACK = 60; // in seconds, time required to catch a transit vehicle
    private static final int TIMEOUT = 10; // in seconds, maximum computation time
    private static final int MAX_DURATION = 90 * 60; // in seconds, the longest we want to travel
    private static final int MAX_RIDES = 3; // maximum number of boardings in a trip
    private static final List<TraverseMode> ACCESS_MODES =
            Lists.newArrayList(TraverseMode.WALK, TraverseMode.BICYCLE, TraverseMode.CAR);
    private static final List<TraverseMode> EGRESS_MODES =
            Lists.newArrayList(TraverseMode.WALK);

    /* A stop which represents the destination */
    private static final Stop fakeTargetStop = new Stop();
    static {
        fakeTargetStop.setId(new AgencyAndId("FAKE", "TARGET"));
    }

    private final Graph graph;
    private final ProfileRequest request;
    public ProfileRouter(Graph graph, ProfileRequest request) {
        this.graph = graph;
        this.request = request;
    }

    /* Search state */
    // while finding direct paths:
    // if dist < M meters OR we don't yet have N stations: record station
    List<Option> directOptions = Lists.newArrayList(); // ways to reach the destination without transit
    Multimap<Stop, Ride> rides = ArrayListMultimap.create(); // at each destination stop, the rides that are worth continuing to explore.
    BinHeap<Ride> queue = new BinHeap<Ride>(); // rides to be explored, prioritized by minumum travel time
    Map<TripPattern, StopAtDistance> fromStops, toStops;
    Set<Ride> targetRides = Sets.newHashSet(); // transit rides that reach the destination
    TimeWindow window; // filters trips used by time of day and service schedule

    /** @return true if the given stop has at least one transfer coming from the given pattern. */
    private boolean hasTransfers(Stop stop, TripPattern pattern) {
        for (ProfileTransfer tr : graph.index.transfersForStop.get(stop)) {
            if (tr.tp1 == pattern) return true;
        }
        return false;
    }

    /** Add a ride to its target stop if it is deemed worthwhile to explore. TODO complete */
    private boolean addRide(Ride newRide) {
        for (Ride ride : rides.get(newRide.to)) {
            if (newRide.durationLowerBound() < ride.durationUpperBound()) {
                // The new ride is sometimes better than an existing ride. Explore it.
                rides.put(newRide.to, newRide);
                queue.insert(newRide, newRide.durationLowerBound());
                return true;
            }
        }
        return false;
    }

    /* Maybe don't even try to accumulate stats and weights on the fly, just enumerate options. */
    /* TODO Or actually use a priority queue based on the min time. */

    public ProfileResponse route () {

        // Establish search timeouts
        long searchBeginTime = System.currentTimeMillis();
        long abortTime = searchBeginTime + TIMEOUT * 1000;

        // TimeWindow could constructed in the caller, which does have access to the graph index.
        this.window = new TimeWindow(request.fromTime, request.toTime, graph.index.servicesRunning(request.date));

        // Lazy-initialize profile transfers
        if (graph.index.transfersForStop == null) {
            graph.index.initializeProfileTransfers();
        }

        findStreetOptions();
        findClosestPatterns();
        LOG.info("from stops: {}", fromStops);
        LOG.info("to stops: {}", toStops);

        /* Enqueue an unfinished PatternRide for each pattern near the origin, grouped by Stop into unfinished Rides. */
        Map<Stop, Ride> initialRides = Maps.newHashMap();
        for (Entry<TripPattern, StopAtDistance> entry : fromStops.entrySet()) {
            TripPattern pattern = entry.getKey();
            StopAtDistance sd = entry.getValue();
            /* Loop over stops in case stop appears more than once in the same pattern. */
            for (int i = 0; i < pattern.getStops().size(); ++i) {
                if (pattern.getStops().get(i) == sd.stop) {
                    if (request.modes.contains(pattern.mode)) {
                        /* Pseudo-transfer from null indicates first leg. */
                        ProfileTransfer xfer = new ProfileTransfer(null, pattern, null, sd.stop, sd.distance);
                        Ride ride = initialRides.get(sd.stop);
                        if (ride == null) {
                            ride = new Ride(sd.stop, null);
                            initialRides.put(sd.stop, ride);
                        }
                        ride.patternRides.add(new PatternRide(pattern, i, null, xfer));
                    }
                }
            }
        }
        for (Ride ride : initialRides.values()) {
            queue.insert(ride, 0);
        }
        /* Explore incomplete rides as long as there are any in the queue. */
        while ( ! queue.empty()) {
            /* Get the minimum-time unfinished ride off the queue. */
            Ride ride = queue.extract_min();
            //ride.dump();
            // maybe when ride is complete, then find transfers here, but that makes for more queue operations.
            if (ride.to != null) throw new AssertionError("Ride should be unfinished.");
            /* Track finished rides by their destination stop, so we can add PatternRides to them. */
            Map<Stop, Ride> rides = Maps.newHashMap();
            /* Complete partial PatternRides (with only a pattern and a beginning stop) which were enqueued in this
             * partial ride. This is done by scanning through the Pattern, creating rides to all downstream stops that
             * are near the destination or have relevant transfers. */
            PR: for (PatternRide pr : ride.patternRides) {
                // LOG.info(" {}", pr);
                List<Stop> stops = pr.pattern.getStops();
                Stop targetStop = null;
                if (toStops.containsKey(pr.pattern)) {
                    /* This pattern has a stop near the destination. Retrieve it. */
                    targetStop = toStops.get(pr.pattern).stop;
                }
                for (int s = pr.fromIndex + 1; s < stops.size(); ++s) {
                    Stop stop = stops.get(s);
                    boolean isTarget = (targetStop != null && targetStop == stop);
                    /* If this destination stop is useful in the search, extend the PatternRide to it. */
                    if (isTarget || hasTransfers(stop, pr.pattern)){
                        PatternRide pr2 = pr.extendToIndex(s, window);
                        // PatternRide may be empty because there are no trips in time window.
                        if (pr2 == null) continue PR;
                        // LOG.info("   {}", pr2);
                        // Get or create the completed Ride to this destination stop.
                        Ride ride2 = rides.get(stop);
                        if (ride2 == null) {
                            ride2 = ride.extendTo(stop);
                            rides.put(stop, ride2);
                        }
                        // Add the completed PatternRide to the completed Ride.
                        ride2.patternRides.add(pr2);
                        // Record this ride as a way to reach the destination.
                        if (isTarget) targetRides.add(ride2);
                    }
                }
            }
            /* Build new downstream Rides by transferring from patterns in current Rides. */
            Map<Stop, Ride> xferRides = Maps.newHashMap(); // A map of incomplete rides after transfers, one for each stop.
            for (Ride r1 : rides.values()) {
                r1.calcStats(window, request.walkSpeed);
                if (r1.waitStats == null) {
                    // This is a sign of a questionable algorithm: we're only seeing if it was possible
                    // to board these trips after the fact, and removing the ride late.
                    // It might make more sense to keep bags of arrival times per ride+stop.
                    targetRides.remove(r1);
                    continue;
                }
                // Do not transfer too many times. Continue after calculating stats since they are still needed!
                if (r1.pathLength() >= MAX_RIDES) continue;
                // r1.to should be the same as r1's key in rides
                // TODO benchmark, this is so not efficient
                for (ProfileTransfer tr : graph.index.transfersForStop.get(r1.to)) {
                    if (r1.containsPattern(tr.tp1)) {
                        // Prune loopy or repetitive paths.
                        if (r1.pathContainsRoute(tr.tp2.route)) continue;
                        if (tr.s1 != tr.s2 && r1.pathContainsStop(tr.s2)) continue;
                        // Scan through stops because a stop might appear more than once in a pattern.
                        for (int i = 0; i < tr.tp2.getStops().size(); ++i) {
                            if (tr.tp2.getStops().get(i) == tr.s2) {
                                if (request.modes.contains(tr.tp2.mode)) {
                                    // Save transfer result for later exploration.
                                    Ride r2 = xferRides.get(tr.s2);
                                    if (r2 == null) {
                                        r2 = new Ride(tr.s2, r1);
                                        xferRides.put(tr.s2, r2);
                                    }
                                    r2.patternRides.add(new PatternRide(tr.tp2, i, r1, tr));
                                }
                            }
                        }
                    }
                }
            }
            /* Enqueue new incomplete Rides with non-excessive durations. */
            for (Ride xr : xferRides.values()) {
                int dlb = xr.previous.durationLowerBound(); // this ride is not finished so it has no times...
                if (dlb > MAX_DURATION) continue;
                queue.insert(xr, dlb);
            }
            if (System.currentTimeMillis() > abortTime) throw new RuntimeException("TIMEOUT");
        }
        /* Build the list of Options by following the back-pointers in Rides. */
        List<Option> options = Lists.newArrayList();
        // for (Ride ride : targetRides) ride.dump();
        for (Ride ride : targetRides) {
            /* We alight from all patterns in a ride at the same stop. */
            int dist = toStops.get(ride.patternRides.get(0).pattern).distance;
            // we could also make a fake ride to the destination, and add an egress segment to the option.
            Option option = new Option(ride, dist);
            if ( ! option.hasEmptyRides()) options.add(option); 
        }
        /* Include the direct (no-transit) biking, driving, and walking options. */
        for (Option option : directOptions) {
            options.add(option);
        }
        LOG.info("Profile routing request finished in {} sec.", (System.currentTimeMillis() - searchBeginTime) / 1000.0);
        return new ProfileResponse(options, request.orderBy, request.limit);
    }

    public void findStreetOptions() {
        for (TraverseMode mode : ACCESS_MODES) {
            if (request.modes.contains(mode)) {
                findStreetOption(mode);
            }
        }
    }

    // We don't need to retain a routing context or any temp vertices.
    public void findClosestPatterns() {
        LOG.info("Finding nearby routes...");
        // First search forward
        fromStops  = findClosestPatterns(false);
        // Then search backward
        toStops    = findClosestPatterns(true);
        LOG.info("Done.");
    }

    /**
     * Find all patterns that stop close to a given vertex.
     * Return a map from each pattern to the closest stop on that pattern.
     * We want a single stop object rather than an index within the pattern because we want to consider
     * stops that appear more than once in a pattern at every index where they occur.
     * Therefore this is not the right place to convert stop objects to stop indexes.
     */
    public Map<TripPattern, StopAtDistance> findClosestPatterns(boolean back) {
        SimpleIsochrone.MinMap<TripPattern, StopAtDistance> closest = new SimpleIsochrone.MinMap<TripPattern, StopAtDistance>();
        for (StopAtDistance stopDist : findClosestStops(back)) {
            for (TripPattern pattern : graph.index.patternsForStop.get(stopDist.stop)) {
                closest.putMin(pattern, stopDist);
            }
        }

        if (closest.size() > 50) {
            // Truncate the list to include a mix of nearby bus and train patterns
            Multimap<StopAtDistance, TripPattern> busPatterns = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
            Multimap<StopAtDistance, TripPattern> otherPatterns = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
            Multimap<StopAtDistance, TripPattern> patterns;
            for (TripPattern pattern : closest.keySet()) {
                patterns = (pattern.mode == TraverseMode.BUS) ? busPatterns : otherPatterns;
                patterns.put(closest.get(pattern), pattern);
            }
            closest.clear();
            Iterator<StopAtDistance> iterBus = busPatterns.keySet().iterator();
            Iterator<StopAtDistance> iterOther = otherPatterns.keySet().iterator();
            // Alternately add one of each kind of pattern until we reach the max
            while (closest.size() < 50) {
                StopAtDistance sd;
                if (iterBus.hasNext()) {
                    sd = iterBus.next();
                    for (TripPattern tp : busPatterns.get(sd)) closest.put(tp, sd);
                }
                if (iterOther.hasNext()) {
                    sd = iterOther.next();
                    for (TripPattern tp: otherPatterns.get(sd)) closest.put(tp, sd);
                }
            }
        }
        return closest;
    }

    /**
     * Perform an on-street search at the origin and destination points
     * to find nearby stops, as well as a path to the destination if possible.
     */
    private List<StopAtDistance> findClosestStops(boolean back) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        // TODO set walk speed!
        rr.setFrom(new GenericLocation(request.from.lat, request.from.lon));
        rr.setTo(new GenericLocation(request.to.lat, request.to.lon));
        rr.setArriveBy(back);
        rr.setRoutingContext(graph);
        // Set batch after context, so both origin and dest vertices will be found.
        rr.setBatch(true);
        rr.setWalkSpeed(request.walkSpeed);
        // If this is not set, searches are very slow.
        int worstElapsedTime = request.accessTime * 60; // from minutes to seconds
        if (back) worstElapsedTime *= -1;
        rr.setWorstTime(rr.dateTime + worstElapsedTime);
        // Note that the (forward) search is intentionally unlimited so it will reach the destination
        // on-street, even though only transit boarding locations closer than req.streetDist will be used.
        GenericAStar astar = new GenericAStar();
        astar.setNPaths(1);
        final List<StopAtDistance> ret = Lists.newArrayList();
        astar.setTraverseVisitor(new TraverseVisitor() {
            @Override public void visitEdge(Edge edge, State state) { }
            @Override public void visitEnqueue(State state) { }
            /* 1. Accumulate stops into ret as the search runs. */
            @Override public void visitVertex(State state) {
                Vertex vertex = state.getVertex();
                if (vertex instanceof TransitStop) {
                    TransitStop tstop = (TransitStop) vertex;
                    LOG.debug("found stop: {} ({}m)", tstop.getStopId(), state.getWalkDistance());
                    LOG.debug("  elapsed time {}", state.getElapsedTimeSeconds());
                    ret.add(new StopAtDistance(tstop.getStop(), (int) state.getWalkDistance()));
                }
            }
        });
        ShortestPathTree spt = astar.getShortestPathTree(rr, System.currentTimeMillis() + 5000);
        rr.rctx.destroy();
        return ret;
    }

    /** Look for an option connecting origin to destination with no transit. */
    private void findStreetOption(TraverseMode mode) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(mode);
        rr.setFrom(new GenericLocation(request.from.lat, request.from.lon));
        rr.setTo(new GenericLocation(request.to.lat, request.to.lon));
        rr.setArriveBy(false);
        rr.setRoutingContext(graph);
        // This is not a batch search, it is a point-to-point search with goal direction.
        // Impose a max time to protect against very slow searches.
        int worstElapsedTime = request.streetTime * 60;
        rr.setWorstTime(rr.dateTime + worstElapsedTime);
        rr.setWalkSpeed(request.walkSpeed);
        rr.setBikeSpeed(request.bikeSpeed);
        GenericAStar astar = new GenericAStar();
        astar.setNPaths(1);
        ShortestPathTree spt = astar.getShortestPathTree(rr, System.currentTimeMillis() + 5000);
        State state = spt.getState(rr.rctx.target);
        if (state != null) {
            LOG.info("Found non-transit option for mode {}", mode);
            directOptions.add(new Option(state));
        }
        rr.rctx.destroy();
    }

    /** Make two time surfaces, one for the minimum and one for the maximum. */
    public P2<TimeSurface> makeSurfaces(List<Ride> rides) {
        // Iterate over the Multimap<Stop, Ride> entries.
        // In a one-to-one search that included only stops near the destination.
        // For making analyst surfaces, that will include ALL reached stops.

        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        // TODO set mode and speed.
        rr.setRoutingContext(graph); // TODO set origin vertex with second parameter
        // Set batch after context, so both origin and dest vertices will be found.
        rr.setBatch(true);
        rr.setWalkSpeed(request.walkSpeed);
        // If time is not limited, searches are very slow.
        int worstElapsedTime = request.accessTime * 60; // convert from minutes to seconds
        rr.setWorstTime(rr.dateTime + worstElapsedTime);
        GenericAStar astar = new GenericAStar();
        final int[] mins = new int[Vertex.getMaxIndex()];
        final int[] maxs = new int[Vertex.getMaxIndex()];
        Arrays.fill(mins, Integer.MAX_VALUE);
        Arrays.fill(maxs, Integer.MIN_VALUE);
        int minmin = Integer.MAX_VALUE;
        int minmax = Integer.MAX_VALUE;
        for (Ride ride : rides) {
            int min = ride.durationLowerBound();
            int max = ride.durationUpperBound();
            if (min < minmin) minmin = min;
            if (max < minmax) minmax = max;
        }
        // aaargh Java
        final int finalminmin = minmin;
        final int finalminmax = minmax;
        astar.setNPaths(1);
        astar.setTraverseVisitor(new TraverseVisitor() {
            @Override public void visitEdge(Edge edge, State state) { }
            @Override public void visitEnqueue(State state) { }
            @Override public void visitVertex(State state) {
                int min = finalminmin + (int) state.getElapsedTimeSeconds();
                int max = finalminmax + (int) state.getElapsedTimeSeconds();
                Vertex vertex = state.getVertex();
                int index = vertex.getIndex();
                if (mins[index] > min)
                    mins[index] = min;
                if (maxs[index] < max)
                    maxs[index] = max;
            }
        });
        ShortestPathTree spt = astar.getShortestPathTree(rr, System.currentTimeMillis() + 5000);
        rr.rctx.destroy();
        return null;
    }

}

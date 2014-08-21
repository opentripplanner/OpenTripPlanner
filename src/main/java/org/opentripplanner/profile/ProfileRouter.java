package org.opentripplanner.profile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileRouter.class);

    /* Search configuration constants */
    public static final int SLACK = 60; // in seconds, time required to catch a transit vehicle
    private static final int TIMEOUT = 10; // in seconds, maximum computation time
    public static final int MAX_DURATION = 90 * 60; // in seconds, the longest we want to travel
    private static final int MAX_RIDES = 3; // maximum number of boardings in a trip
    private static final int MIN_DRIVE_TIME = 10 * 60; // in seconds
    private static final List<TraverseMode> ACCESS_MODES =
            Lists.newArrayList(TraverseMode.WALK, TraverseMode.BICYCLE, TraverseMode.CAR);
    private static final List<TraverseMode> EGRESS_MODES =
            Lists.newArrayList(TraverseMode.WALK);

    public final Graph graph;
    public final ProfileRequest request;

    public ProfileRouter(Graph graph, ProfileRequest request) {
        this.graph = graph;
        this.request = request;
    }

    /* Search state */
    Multimap<StopCluster, StopAtDistance> fromStopPaths, toStopPaths; // ways to reach each origin or dest stop cluster
    List<RoutingContext> routingContexts = Lists.newArrayList();

    /* Analyst: time bounds for each vertex */
    public int[] mins, maxs;
    public TimeSurface minSurface, maxSurface;

    // while finding direct paths:
    // if dist < M meters OR we don't yet have N stations: record station
    Collection<StopAtDistance> directPaths = Lists.newArrayList(); // ways to reach the destination without transit
    Multimap<StopCluster, Ride> retainedRides = ArrayListMultimap.create(); // the rides arriving at each stop that are worth continuing to explore.
    BinHeap<Ride> queue = new BinHeap<Ride>(); // rides to be explored, prioritized by minumum travel time
    // TODO rename fromStopsByPattern
    Map<TripPattern, StopAtDistance> fromStops, toStops;
    Set<Ride> targetRides = Sets.newHashSet(); // transit rides that reach the destination
    TimeWindow window; // filters trips used by time of day and service schedule

    /** @return true if the given stop cluster has at least one transfer coming from the given pattern. */
    private boolean hasTransfers(StopCluster stopCluster, TripPattern pattern) {
        for (ProfileTransfer tr : graph.index.transfersFromStopCluster.get(stopCluster)) {
            if (tr.tp1 == pattern) return true;
        }
        return false;
    }

    /* Maybe don't even try to accumulate stats and weights on the fly, just enumerate options. */
    /* TODO Or actually use a priority queue based on the min time. */

    public ProfileResponse route () {

        // Lazy-initialize profile transfers (before setting timeouts, since this is slow)
        if (graph.index.transfersFromStopCluster == null) {
            graph.index.initializeProfileTransfers();
        }
        // Analyst
        if (request.analyst) {
            mins = new int[Vertex.getMaxIndex()];
            maxs = new int[Vertex.getMaxIndex()];
            Arrays.fill(mins, TimeSurface.UNREACHABLE);
            Arrays.fill(maxs, TimeSurface.UNREACHABLE);
        }
        LOG.info("modes: {}", request.modes);

        // Establish search timeouts
        long searchBeginTime = System.currentTimeMillis();
        long abortTime = searchBeginTime + TIMEOUT * 1000;

        // TimeWindow could constructed in the caller, which does have access to the graph index.
        this.window = new TimeWindow(request.fromTime, request.toTime, graph.index.servicesRunning(request.date));

        LOG.info("Finding access/egress paths.");
        // Look for stops that are within a given time threshold of the origin and destination
        // Find the closest stop on each pattern near the origin and destination
        // TODO consider that some stops may be closer by one mode than another
        // and that some stops may be accessible by one mode but not another
        fromStopPaths = findClosestStops(false);
        fromStops = findClosestPatterns(fromStopPaths);
        if ( ! request.analyst) {
            toStopPaths = findClosestStops(true);
            toStops = findClosestPatterns(toStopPaths);
            // Also look for options connecting origin to destination with no transit.
            for (TraverseMode mode : ACCESS_MODES) {
                if (request.modes.contains(mode)) findStreetOption(mode);
            }
        }
        LOG.info("Done finding access/egress paths.");
        LOG.info("From patterns/stops: {}", fromStops);
        LOG.info("To patterns/stops: {}", toStops);

        /* Enqueue an unfinished PatternRide for each pattern near the origin, grouped by Stop into unfinished Rides. */
        Map<StopCluster, Ride> initialRides = Maps.newHashMap(); // One ride per stop cluster
        for (Entry<TripPattern, StopAtDistance> entry : fromStops.entrySet()) {
            TripPattern pattern = entry.getKey();
            StopAtDistance sd = entry.getValue();
            if ( ! request.modes.contains(pattern.mode)) continue; // FIXME why are we even storing these patterns?
            /* Loop over stop clusters in case stop cluster appears more than once in the same pattern. */
            for (int i = 0; i < pattern.getStops().size(); ++i) {
                // FIXME using String identity equality for stop clusters on purpose
                if (sd.stop == graph.index.stopClusterForStop.get(pattern.getStops().get(i))) {
                    Ride ride = initialRides.get(sd.stop);
                    if (ride == null) {
                        ride = new Ride(sd.stop, null); // null previous ride because this is the first ride
                        ride.accessTime = sd.etime;
                        ride.accessDist = 0; // FIXME
                        initialRides.put(sd.stop, ride);
                    }
                    ride.patternRides.add(new PatternRide(pattern, i));
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
            //LOG.info("dequeued ride {}", ride);
            //ride.dump();
            // maybe when ride is complete, then find transfers here, but that makes for more queue operations.
            if (ride.to != null) throw new AssertionError("Ride should be unfinished.");
            /* Track finished rides by their destination stop, so we can add PatternRides to them. */
            Map<StopCluster, Ride> rides = Maps.newHashMap();
            /* Complete partial PatternRides (with only a pattern and a beginning stop) which were enqueued in this
             * partial ride. This is done by scanning through the Pattern, creating rides to all downstream stops that
             * are near the destination or have relevant transfers. */
            PR: for (PatternRide pr : ride.patternRides) {
                // LOG.info(" {}", pr);
                List<Stop> stops = pr.pattern.getStops();
                StopCluster targetCluster = null;
                if (toStops != null && toStops.containsKey(pr.pattern)) {
                    /* This pattern has a stop near the destination. Retrieve it. */
                    targetCluster = toStops.get(pr.pattern).stop;
                }
                for (int s = pr.fromIndex + 1; s < stops.size(); ++s) {
                    StopCluster cluster = graph.index.stopClusterForStop.get(stops.get(s));
                    boolean isTarget = (targetCluster != null && targetCluster == cluster);
                    /* If this destination stop is useful in the search, extend the PatternRide to it. */
                    if (isTarget || hasTransfers(cluster, pr.pattern)) {
// FIXME this was commented out for analyst, which needs every stop ^
                        PatternRide pr2 = pr.extendToIndex(s, window);
                        // PatternRide may be empty because there are no trips in time window.
                        if (pr2 == null) continue PR;
                        // LOG.info("   {}", pr2);
                        // Get or create the completed Ride to this destination stop.
                        Ride ride2 = rides.get(cluster);
                        if (ride2 == null) {
                            ride2 = ride.extendTo(cluster);
                            rides.put(cluster, ride2);
                        }
                        // Add the completed PatternRide to the completed Ride.
                        ride2.patternRides.add(pr2);
                        // Record this ride as a way to reach the destination.
                        if (isTarget) targetRides.add(ride2);
                    }
                }
            }
            /* Build new downstream Rides by transferring from patterns in current Rides. */
            Map<StopCluster, Ride> xferRides = Maps.newHashMap(); // A map of incomplete rides after transfers, one for each stop.
            for (Ride r1 : rides.values()) {
                r1.calcStats(window, request.walkSpeed);
                if (r1.waitStats == null) {
                    // This is a sign of a questionable algorithm: we're only seeing if it was possible
                    // to board these trips after the fact, and removing the ride late.
                    // It might make more sense to keep bags of arrival times per ride+stop.
                    targetRides.remove(r1);
                    continue;
                }
                // We now have a new completed ride. Record its lower and upper bounds at the arrival stop.
                if (request.analyst) {
                    TransitStop tstop = graph.index.stopVertexForStop.get(r1.to);
                    int tsidx = tstop.getIndex();
                    int lb = r1.durationLowerBound();
                    int ub = r1.durationUpperBound();
                    if (mins[tsidx] == TimeSurface.UNREACHABLE || mins[tsidx] > lb)
                        mins[tsidx] = lb;
                    if (maxs[tsidx] == TimeSurface.UNREACHABLE || maxs[tsidx] > ub) // Yes, we want the _minimum_ upper bound.
                        maxs[tsidx] = ub;

                }
                // Do not transfer too many times. Continue after calculating stats since they are still needed!
                int nRides = r1.pathLength();
                if (nRides >= MAX_RIDES) continue;
                boolean penultimateRide = (nRides == MAX_RIDES - 1);
                // r1.to should be the same as r1's key in rides
                // TODO benchmark, this is so not efficient
                for (ProfileTransfer tr : graph.index.transfersFromStopCluster.get(r1.to)) {
                    if ( ! request.modes.contains(tr.tp2.mode)) continue;
                    if (r1.containsPattern(tr.tp1)) {
                        // Prune loopy or repetitive paths.
                        if (r1.pathContainsRoute(tr.tp2.route)) continue;
                        if (tr.sc1 != tr.sc2 && r1.pathContainsStop(tr.sc2)) continue;
                        // Optimization: on the last ride of point-to-point searches,
                        // only transfer to patterns that pass near the destination.
                        if ( ! request.analyst && penultimateRide && ! toStops.containsKey(tr.tp2)) continue;
                        // Scan through stops looking for transfer target: stop might appear more than once in a pattern.
                        TARGET_STOP : for (int i = 0; i < tr.tp2.getStops().size(); ++i) {
                            StopCluster cluster = graph.index.stopClusterForStop.get(tr.tp2.getStops().get(i));
                            if (cluster == tr.sc2) {
                                // Save transfer result for later exploration.
                                Ride r2 = xferRides.get(tr.sc2);
                                if (r2 == null) {
                                    r2 = new Ride(tr.sc2, r1);
                                    r2.accessDist = tr.distance;
                                    r2.accessTime = (int) (tr.distance / request.walkSpeed);
                                    xferRides.put(tr.sc2, r2);
                                }
                                for (PatternRide pr : r2.patternRides) {
                                    // Multiple patterns can have transfers to the same target pattern,
                                    // but Rides should not have duplicate PatternRides.
                                    // TODO refactor with equals function and contains().
                                    if (pr.pattern == tr.tp2 && pr.fromIndex == i) continue TARGET_STOP;
                                }
                                r2.patternRides.add(new PatternRide(tr.tp2, i));
                            }
                        }
                    }
                }
            }
            /* Enqueue new incomplete Rides with non-excessive durations. */
            // TODO maybe check excessive time before transferring (above, where we prune loopy paths)
            for (Ride r : xferRides.values()) maybeAddRide(r);
            if (System.currentTimeMillis() > abortTime) throw new RuntimeException("TIMEOUT");
        }
        LOG.info("Profile routing request finished in {} sec.", (System.currentTimeMillis() - searchBeginTime) / 1000.0);
        if (request.analyst) {
            makeSurfaces();
            return null;
        }
        /* Non-analyst. Build the list of Options by following the back-pointers in Rides. */
        List<Option> options = Lists.newArrayList();
        // for (Ride ride : targetRides) ride.dump();
        for (Ride ride : targetRides) {
            /* We alight from all patterns in a ride at the same stop. */
            int dist = toStops.get(ride.patternRides.get(0).pattern).etime; // TODO time vs dist
            Collection<StopAtDistance> accessPaths = fromStopPaths.get(ride.getAccessStopCluster());
            Collection<StopAtDistance> egressPaths = toStopPaths.get(ride.getEgressStopCluster());
            Option option = new Option(ride, accessPaths, egressPaths);
            StopCluster s0 = ride.getAccessStopCluster();
            StopCluster s1 = ride.getEgressStopCluster();
            if ( ! option.hasEmptyRides()) options.add(option);
        }
        /* Include the direct (no-transit) biking, driving, and walking options. */
        options.add(new Option(null, directPaths, null));
/*
        for (Stop stop : graph.index.stopVertexForStop.keySet()) {
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            int min = mins[tstop.getIndex()];
            int max = maxs[tstop.getIndex()];
            if (min == Integer.MAX_VALUE)
                LOG.info("{} unreachable", tstop.getName());
            else
                LOG.info("{} min {} max {}", tstop.getName(), min, max);
        }
*/
        return new ProfileResponse(options, request.orderBy, request.limit);
    }

    public boolean maybeAddRide(Ride newRide) {
        int dlb = newRide.previous.durationLowerBound(); // this ride is unfinished so it has no times yet
        if (dlb > MAX_DURATION) return false;
        // Check whether any existing rides at the same location (stop) dominate the new one
        for (Ride oldRide : retainedRides.get(newRide.from)) { // fromv because this is an unfinished ride
            if (dlb > oldRide.previous.durationUpperBound()) { // TODO re-verify logic
                return false; // minimum duration of new ride is longer than maximum duration of some existing ride
            }
        }
        // No existing ride is strictly better than the new ride.
        retainedRides.put(newRide.from, newRide);
        queue.insert(newRide, dlb);
        return true;
    }

    /**
     * @param stopClusters a multimap from stop clusters to one or more StopAtDistance objects at the corresponding cluster.
     * @return for each TripPattern that passes through any of the supplied stop clusters, the stop cluster that is
     * closest to the origin or destination point according to the distances in the StopAtDistance objects.
     *
     * In short, take a bunch of stop clusters near the origin or destination and return the quickest way to reach each
     * pattern that passes through them.
     * We want stop cluster references rather than indexes within the patterns because when a stop cluster appears more
     * than once in a pattern, we want to consider boarding or alighting from that pattern at every index where the
     * cluster occurs.
     */
    public Map<TripPattern, StopAtDistance> findClosestPatterns(Multimap<StopCluster, StopAtDistance> stopClusters) {
        SimpleIsochrone.MinMap<TripPattern, StopAtDistance> closest = new SimpleIsochrone.MinMap<TripPattern, StopAtDistance>();
        // Iterate over all StopAtDistance for all Stops. The fastest mode will win at each stop.
        for (StopAtDistance stopDist : stopClusters.values()) {
            for (Stop stop : stopDist.stop.children) {
                for (TripPattern pattern : graph.index.patternsForStop.get(stop)) {
                    closest.putMin(pattern, stopDist);
                }
            }
        }
        // Truncate long lists to include a mix of nearby bus and train patterns
        if (closest.size() > 500) {
            LOG.warn("Truncating long list of patterns.");
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
     * Perform an on-street search around a point with each of several modes to find nearby stops.
     * @return one or more paths to each reachable stop using the various modes.
     */
    private Multimap<StopCluster, StopAtDistance> findClosestStops(boolean dest) {
        Multimap<StopCluster, StopAtDistance> pathsByStop = ArrayListMultimap.create();
        for (TraverseMode mode: (dest ? EGRESS_MODES : ACCESS_MODES)) {
            if (request.modes.contains(mode)) {
                LOG.info("{} mode {}", dest ? "egress" : "access", mode);
                for (StopAtDistance sd : findClosestStops(mode, dest)) {
                    pathsByStop.put(sd.stop, sd);
                }
            }
        }
        return pathsByStop;
    }

    /**
     * Perform an on-street search around a point with a specific mode to find nearby stops.
     * @param dest : whether to search at the destination instead of the origin.
     */
    private Collection<StopAtDistance> findClosestStops(final TraverseMode mode, boolean dest) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.setFrom(new GenericLocation(request.from.lat, request.from.lon));
        // FIXME requires destination to be set, not necesary for analyst
        rr.setTo(new GenericLocation(request.to.lat, request.to.lon));
        rr.setArriveBy(dest);
        rr.setMode(mode);
        // TODO CAR does not seem to work. rr.setModes(new TraverseModeSet(TraverseMode.WALK, mode));
        rr.setRoutingContext(graph);
        // Set batch after context, so both origin and dest vertices will be found.
        rr.setBatch(true);
        rr.setWalkSpeed(request.walkSpeed);
        // RR dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        long worstElapsedTime = request.accessTime * 60; // convert from minutes to seconds
        if (dest) worstElapsedTime *= -1;
        rr.setWorstTime(rr.dateTime + worstElapsedTime);
        // Note that the (forward) search is intentionally unlimited so it will reach the destination
        // on-street, even though only transit boarding locations closer than req.streetDist will be used.
        GenericAStar astar = new GenericAStar();
        astar.setNPaths(1);
        StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(mode);
        astar.setTraverseVisitor(visitor);
        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // seconds timeout
        // Save the routing context for later cleanup. We need its temporary edges to render street segments at the end.
        routingContexts.add(rr.rctx);
        return visitor.stopClustersFound.values();
    }

    static class StopFinderTraverseVisitor implements TraverseVisitor {
        TraverseMode mode;
        Map<StopCluster, StopAtDistance> stopClustersFound = Maps.newHashMap();
        public StopFinderTraverseVisitor(TraverseMode mode) { this.mode = mode; }
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        // Accumulate stops into ret as the search runs.
        @Override public void visitVertex(State state) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof TransitStop) {
                StopAtDistance sd = new StopAtDistance(state);
                sd.mode = mode;
                if (sd.mode == TraverseMode.CAR && sd.etime < MIN_DRIVE_TIME) return;
                if (stopClustersFound.containsKey(sd.stop)) return; // record only the closest stop in each cluster
                LOG.debug("found stop cluster: {}", sd);
                stopClustersFound.put(sd.stop, sd);
            }
        }
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
            directPaths.add(new StopAtDistance(state));
        }
        routingContexts.add(rr.rctx); // save context for later cleanup so temp edges remain available
    }

    // Major change: This needs to include all stops, not just those where transfers occur or those near the destination.
    /** Make two time surfaces, one for the minimum and one for the maximum. */
    public P2<TimeSurface> makeSurfaces() {
        LOG.info("Propagating profile router result to street vertices.");
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.setMode(TraverseMode.WALK);
        rr.setWalkSpeed(request.walkSpeed);
        // If max trip duration is not limited, searches are of course much slower.
        int worstElapsedTime = request.accessTime * 60; // convert from minutes to seconds
        rr.setWorstTime(rr.dateTime + worstElapsedTime);
        rr.setBatch(true);
        GenericAStar astar = new GenericAStar();
        astar.setNPaths(1);
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            int index = tstop.getIndex();
            // Generate a tree outward from all stops that have been touched in the basic profile search
            if (mins[index] == TimeSurface.UNREACHABLE || maxs[index] == TimeSurface.UNREACHABLE) continue;
            rr.setRoutingContext(graph, tstop, null); // Set origin vertex directly instead of generating link edges
            astar.setTraverseVisitor(new ExtremaPropagationTraverseVisitor(mins[index], maxs[index]));
            ShortestPathTree spt = astar.getShortestPathTree(rr, 5);
            rr.rctx.destroy();
        }
        minSurface = new TimeSurface(this, false);
        maxSurface = new TimeSurface(this, true);
        LOG.info("done making timesurfaces.");
        return new P2<TimeSurface>(minSurface, maxSurface);
    }

    /** Given a minimum and maximum at a starting vertex, build an on-street SPT and propagate those values outward. */
    class ExtremaPropagationTraverseVisitor implements TraverseVisitor {
        final int min0;
        final int max0;
        ExtremaPropagationTraverseVisitor(int min0, int max0) {
            this.min0 = min0;
            this.max0 = max0;
        }
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        @Override public void visitVertex(State state) {
            int min = min0 + (int) state.getElapsedTimeSeconds();
            int max = max0 + (int) state.getElapsedTimeSeconds();
            Vertex vertex = state.getVertex();
            int index = vertex.getIndex();
            if (index >= mins.length) return; // New temp vertices may have been created since the array was dimensioned.
            if (mins[index] == TimeSurface.UNREACHABLE || mins[index] > min)
                mins[index] = min;
            if (maxs[index] == TimeSurface.UNREACHABLE || maxs[index] > max) // Yes we want the minimum upper bound (minimum maximum)
                maxs[index] = max;
        }
    }

    /** Destroy all routing contexts created during this search. */
    public int cleanup() {
        int n = 0;
        for (RoutingContext rctx : routingContexts) {
            rctx.destroy();
            n += 1;
        }
        routingContexts.clear();
        LOG.info("destroyed {} routing contexts.", n);
        return n;
    }

}

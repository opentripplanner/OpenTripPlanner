package org.opentripplanner.profile;

import com.google.common.collect.*;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.resource.SimpleIsochrone;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
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

import java.util.*;
import java.util.Map.Entry;

/**
 * Rather than finding a single optimal route, ProfileRouter aims to find all reasonable ways to go from an origin
 * to a destination over a given time window, and expresses those results in terms of route combinations and time ranges.
 * For example:
 * riding Train A followed by Bus B between 9AM and 11AM takes between 1.2 and 1.6 hours;
 * riding Train A followed by Bus C takes between 1.4 and 1.8 hours.
 *
 * Create one instance of ProfileRouter per profile search. It is not intended to be threadsafe or reusable.
 * You MUST call the cleanup method on all ProfileRouter instances that have been used for routing before
 * they are released for garbage collection.
 */
public class ProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileRouter.class);

    /* Search configuration constants */
    public static final int SLACK = 60; // in seconds, time required to catch a transit vehicle
    private static final int TIMEOUT = 10; // in seconds, maximum computation time
    public static final int MAX_DURATION = 90 * 60; // in seconds, the longest we want to travel
    private static final int MAX_RIDES = 3; // maximum number of boardings in a trip
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
            synchronized (graph.index) {
                // why another if statement? so that if another thread initialized this in the meantime
                // we don't initialize it again.
                if (graph.index.transfersFromStopCluster == null) {
                    graph.index.initializeProfileTransfers();
                }
            }
        }
        // Analyst
        if (request.analyst) {
            mins = new int[Vertex.getMaxIndex()];
            maxs = new int[Vertex.getMaxIndex()];
            Arrays.fill(mins, TimeSurface.UNREACHABLE);
            Arrays.fill(maxs, TimeSurface.UNREACHABLE);
        }
        LOG.info("access modes: {}", request.accessModes);
        LOG.info("egress modes: {}", request.egressModes);
        LOG.info("direct modes: {}", request.directModes);

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
                LOG.info("Finding non-transit path for mode {}", mode);
                if (request.directModes.contains(mode)) findStreetOption(mode); // TODO rename function
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
            if ( ! request.transitModes.contains(pattern.mode)) {
                continue; // FIXME why are we even storing these patterns?
            }
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
                for (int s = pr.fromIndex + 1; s < stops.size(); ++s) {
                    StopCluster cluster = graph.index.stopClusterForStop.get(stops.get(s));
                    /* Originally we only extended rides to destination stops considered useful in the search, i.e.
                     * those that had transfers leading out of them or were known to be near the destination.
                     * However, analyst needs to know the times we can reach every stop, and pruning is more effective
                     * if we know when rides pass through all stops.*/
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
                    continue;
                }
                if ( ! addIfNondominated(r1)) continue; // abandon this ride if it is dominated by some existing ride at the same location
                // We have a new, nondominated, completed ride. Record its lower and upper bounds at the arrival stop.
                if (request.analyst) {
                    for (Stop s : r1.to.children) {
                        // TODO This could be done at the end now that we are retaining all rides.
                        TransitStop tstop = graph.index.stopVertexForStop.get(s);
                        int tsidx = tstop.getIndex();
                        int lb = r1.durationLowerBound();
                        int ub = r1.durationUpperBound();
                        if (mins[tsidx] == TimeSurface.UNREACHABLE || mins[tsidx] > lb)
                            mins[tsidx] = lb;
                        if (maxs[tsidx] == TimeSurface.UNREACHABLE || maxs[tsidx] > ub) // Yes, we want the _minimum_ upper bound.
                            maxs[tsidx] = ub;
                    }
                }
                /* Find transfers out of this new ride. */
                // Do not transfer too many times. Check after calculating stats since stats are needed in any case.
                int nRides = r1.pathLength();
                if (nRides >= MAX_RIDES) continue;
                boolean penultimateRide = (nRides == MAX_RIDES - 1);
                // Invariant: r1.to should be the same as r1's key in rides
                // TODO benchmark, this is so not efficient
                for (ProfileTransfer tr : graph.index.transfersFromStopCluster.get(r1.to)) {
                    if ( ! request.transitModes.contains(tr.tp2.mode)) continue;
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
                                // Save transfer result in an unfinished ride for later exploration.
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
            for (Ride r : xferRides.values()) {
                // ride is unfinished, use previous ride's time as key
                if (addIfNondominated(r)) queue.insert(r, r.previous.durationLowerBound());
            }
            if (System.currentTimeMillis() > abortTime) throw new RuntimeException("TIMEOUT");
        }
        LOG.info("Profile routing request finished in {} sec.", (System.currentTimeMillis() - searchBeginTime) / 1000.0);
        if (request.analyst) {
            makeSurfaces();
            return null;
        }
        /* Non-analyst: Determine which rides are good ways to reach the destination. */
        // FIXME determine why there are multiple copies of the same ride then maybe use a list
        Set<Ride> targetRides = Sets.newHashSet();
        for (StopCluster cluster : toStopPaths.keySet()) {
            for (Ride ride : retainedRides.get(cluster)) {
                PATTERN: for (PatternRide pr : ride.patternRides) {
                    StopAtDistance clusterForPattern = toStops.get(pr.pattern);
                    if (clusterForPattern != null && clusterForPattern.stop == cluster) {
                        targetRides.add(ride);
                        break PATTERN;
                    }
                }
            }
        }
        LOG.info("{} nondominated rides stop near the destination.", targetRides.size());
        /* Non-analyst: Build the list of Options by following the back-pointers in Rides. */
        List<Option> options = Lists.newArrayList();
        for (Ride ride : targetRides) {
            /* We alight from all patterns in a ride at the same stop. */
            int dist = toStops.get(ride.patternRides.get(0).pattern).etime; // TODO time vs dist
            Collection<StopAtDistance> accessPaths = fromStopPaths.get(ride.getAccessStopCluster());
            Collection<StopAtDistance> egressPaths = toStopPaths.get(ride.getEgressStopCluster());
            Option option = new Option(ride, accessPaths, egressPaths);
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

    /** Check whether a new ride has too long a duration relative to existing rides at the same location or global time limit. */
    public boolean addIfNondominated(Ride newRide) {
        StopCluster cluster = newRide.to;
        if (cluster == null) { // if ride is unfinished, calculate time to its from-cluster based on previous ride
            cluster = newRide.from;
            newRide = newRide.previous;
        }
        if (newRide.durationLowerBound() > MAX_DURATION) return false;
        // Check whether any existing rides at the same location (stop cluster) dominate the new one.
        for (Ride oldRide : retainedRides.get(cluster)) {
            if (oldRide.to == null) oldRide = oldRide.previous; // rides may be unfinished
            // New rides must be strictly better (min and max) than any existing one with less transfers.
            // This avoids alternatives formed by simply inserting extra unnecessary rides.
            if (oldRide.pathLength() < newRide.pathLength() &&
                oldRide.durationLowerBound() < newRide.durationLowerBound() &&
                oldRide.durationUpperBound() < newRide.durationUpperBound()) {
                return false;
            }
            // State is not strictly dominated. Perhaps it has the same number of transfers.
            // In this case we want to keep it as long as it's sometimes better than all the others (time ranges overlap).
            if (newRide.durationLowerBound() > oldRide.durationUpperBound() + request.suboptimalMinutes) {
                return false;
            }
        }
        retainedRides.put(cluster, newRide);
        return true; // No existing ride is strictly better than the new ride.
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
                    //LOG.info("trip pattern {}", pattern);
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
            if ((dest ? request.egressModes : request.accessModes).contains(mode)) {
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
        RoutingRequest rr = new RoutingRequest(mode);
        if (mode == TraverseMode.CAR) {
            //rr.kissAndRide = true; // allow car->walk transition. we are assuming that someone will drop you off.
            rr.parkAndRide = true; // allow car->walk transition only at tagged park and ride facilities.
            rr.modes.setWalk(true); // need to walk after dropping the car off
        }
        rr.from = (new GenericLocation(request.from.lat, request.from.lon));
        // FIXME requires destination to be set, not necesary for analyst
        rr.to = new GenericLocation(request.to.lat, request.to.lon);
        rr.setArriveBy(dest);
        rr.setRoutingContext(graph);
        // Set batch after context, so both origin and dest vertices will be found.
        rr.batch = (true);
        rr.walkSpeed = request.walkSpeed;
        // RR dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        int minAccessTime = 0;
        int maxAccessTime = request.maxWalkTime;
        if (mode == TraverseMode.BICYCLE) {
            rr.bikeSpeed = request.bikeSpeed;
            minAccessTime = request.minBikeTime;
            maxAccessTime = request.maxBikeTime;
        } else if (mode == TraverseMode.CAR) {
            rr.carSpeed = request.carSpeed;
            minAccessTime = request.minCarTime;
            maxAccessTime = request.maxCarTime;
        } else {
            LOG.warn("No modes matched when setting min/max travel times.");
        }
        long worstElapsedTimeSeconds = maxAccessTime * 60; // convert from minutes to seconds
        if (dest) worstElapsedTimeSeconds *= -1;
        rr.worstTime = (rr.dateTime + worstElapsedTimeSeconds);
        // Note that the (forward) search is intentionally unlimited so it will reach the destination
        // on-street, even though only transit boarding locations closer than req.streetDist will be used.
        GenericAStar astar = new GenericAStar();
        rr.setNumItineraries(1);
        StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(mode, minAccessTime * 60);
        astar.setTraverseVisitor(visitor);
        astar.getShortestPathTree(rr, 5); // timeout in seconds
        // Save the routing context for later cleanup. We need its temporary edges to render street segments at the end.
        routingContexts.add(rr.rctx);
        return visitor.stopClustersFound.values();
    }

    static class StopFinderTraverseVisitor implements TraverseVisitor {
        TraverseMode mode;
        int minTravelTimeSeconds = 0;
        Map<StopCluster, StopAtDistance> stopClustersFound = Maps.newHashMap();
        public StopFinderTraverseVisitor(TraverseMode mode, int minTravelTimeSeconds) {
            this.mode = mode;
            this.minTravelTimeSeconds = minTravelTimeSeconds;
        }
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        // Accumulate stops into ret as the search runs.
        @Override public void visitVertex(State state) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof TransitStop) {
                StopAtDistance sd = new StopAtDistance(state);
                sd.mode = mode; // Override final mode from State, since driving to transit ends in walking.
                if (sd.mode == TraverseMode.CAR && sd.etime < minTravelTimeSeconds) return;
                if (stopClustersFound.containsKey(sd.stop)) return; // record only the closest stop in each cluster
                LOG.debug("found stop cluster: {}", sd);
                stopClustersFound.put(sd.stop, sd);
            }
        }
    }

    /** Look for an option connecting origin to destination without using transit. */
    private void findStreetOption(TraverseMode mode) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        RoutingRequest rr = new RoutingRequest(mode);
        rr.from = (new GenericLocation(request.from.lat, request.from.lon));
        rr.to = new GenericLocation(request.to.lat, request.to.lon);
        rr.setArriveBy(false);
        rr.setRoutingContext(graph);
        // This is not a batch search, it is a point-to-point search with goal direction.
        // Impose a max time to protect against very slow searches.
        int worstElapsedTime = request.streetTime * 60;
        rr.worstTime = (rr.dateTime + worstElapsedTime);
        rr.walkSpeed = request.walkSpeed;
        rr.bikeSpeed = request.bikeSpeed;
        GenericAStar astar = new GenericAStar();
        rr.setNumItineraries(1);
        ShortestPathTree spt = astar.getShortestPathTree(rr, System.currentTimeMillis() + 5000); // FIXME timeout is absolute and in seconds ?
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
        rr.walkSpeed = request.walkSpeed;
        // If max trip duration is not limited, searches are of course much slower.
        int worstElapsedTime = request.maxWalkTime * 60; // convert from minutes to seconds, assume walking at egress
        rr.worstTime = (rr.dateTime + worstElapsedTime);
        rr.batch = (true);
        GenericAStar astar = new GenericAStar();
        rr.setNumItineraries(1);
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

    /**
     * Destroy all routing contexts created during this search. This method must be called manually on any
     * ProfileRouter instance before it is released for garbage collection, because RoutingContexts remain linked into
     * the graph by temporary edges if they are not cleaned up.
     */
    public int cleanup() {
        int n = 0;
        for (RoutingContext rctx : routingContexts) {
            rctx.destroy();
            n += 1;
        }
        routingContexts.clear();
        LOG.debug("destroyed {} routing contexts.", n);
        return n;
    }

    /**
     * This finalizer is intended as a failsafe to prevent memory leakage in case someone does
     * not clean up routing contexts. It should be considered an error if this method does any work.
     */
    @Override
    public void finalize() {
        if (routingContexts.size() > 0) {
            LOG.error("RoutingContexts were observed in the ProfileRouter finalizer: this is a memory leak.");
            cleanup();
        }
    }

}

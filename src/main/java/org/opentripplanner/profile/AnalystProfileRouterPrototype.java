package org.opentripplanner.profile;

import com.google.common.collect.*;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * More optimized ProfileRouter targeting one-to-many searches on entirely frequency-based transit networks.
 *
 * This requires simpleTransfers to exist in the graph, so it needs to be built in longDistance mode.
 */
public class AnalystProfileRouterPrototype {

    private static final Logger LOG = LoggerFactory.getLogger(AnalystProfileRouterPrototype.class);

    /* Search configuration constants */
    public static final int SLACK = 60; // in seconds, time required to catch a transit vehicle
    private static final int TIMEOUT = 10; // in seconds, maximum computation time
    public static final int MAX_DURATION = 90 * 60; // in seconds, the longest we want to travel
    private static final int MAX_RIDES = 4; // maximum number of boardings in a trip
    private static final List<TraverseMode> ACCESS_MODES =
            Lists.newArrayList(TraverseMode.WALK, TraverseMode.BICYCLE, TraverseMode.CAR);
    private static final List<TraverseMode> EGRESS_MODES =
            Lists.newArrayList(TraverseMode.WALK);

    public final Graph graph;
    public final ProfileRequest request;
    public final Map<Vertex, TimeRange> propagatedTimes = Maps.newHashMap(); // the travel times propagated onto the street network
    // TODO propagate times from the origin point without transit.

    public AnalystProfileRouterPrototype(Graph graph, ProfileRequest request) {
        this.graph = graph;
        this.request = request;
    }

    /* Search state */
    Multimap<StopCluster, StopAtDistance> fromStopPaths, toStopPaths; // ways to reach each origin or dest stop cluster
    List<RoutingContext> routingContexts = Lists.newArrayList();

    TObjectIntMap<Stop> fromStops;
    TimeWindow window; // filters trips used by time of day and service schedule

    /** Return a set of all patterns that pass through the stops that are present in the given Tracker. */
    public Set<TripPattern> uniquePatternsVisiting(Set<Stop> stops) {
        Set<TripPattern> patterns = Sets.newHashSet();
        for (Stop stop : stops) {
            for (TripPattern pattern : graph.index.patternsForStop.get(stop)) {
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    long searchBeginTime;
    long abortTime;

    private void checkTimeout() {
        if (System.currentTimeMillis() > abortTime) {
            throw new RuntimeException("TIMEOUT");
        }
    }

    public TimeSurface.RangeSet route () {

        // NOT USED here, however FIXME this is not threadsafe, needs lock graph.index.clusterStopsAsNeeded();

        LOG.info("access modes: {}", request.accessModes);
        LOG.info("egress modes: {}", request.egressModes);
        LOG.info("direct modes: {}", request.directModes);

        // Establish search timeouts
        searchBeginTime = System.currentTimeMillis();
        abortTime = searchBeginTime + TIMEOUT * 1000;

        // TimeWindow could constructed in the caller, which does have access to the graph index.
        this.window = new TimeWindow(request.fromTime, request.toTime, graph.index.servicesRunning(request.date));

        fromStops = findClosestStops(TraverseMode.WALK);
        LOG.info("From patterns/stops: {}", fromStops);

        /* Initialize time range tracker to begin the search. */
        TimeRange.Tracker times = new TimeRange.Tracker();
        for (Stop stop : fromStops.keySet()) {
            times.set(stop, fromStops.get(stop));
        }

        Set<Stop> stopsUpdated = fromStops.keySet();

        for (int round = 0; round < MAX_RIDES; round++) {

            // TODO maybe even loop until no updates happen? That should happen automatically if MAX_RIDES is high enough.

            /* Get all patterns passing through stops updated in the last round, then reinitialize the updated stops set. */
            Set<TripPattern> patternsUpdated = uniquePatternsVisiting(stopsUpdated);
            LOG.info("ROUND {} : {} stops and {} patterns to explore.", round, stopsUpdated.size(), patternsUpdated.size());
            stopsUpdated = Sets.newHashSet();

            /* RAPTOR style: iterate over each pattern once. */
            for (TripPattern pattern : patternsUpdated) {
                //checkTimeout();
                TimeRange rangeBeingPropagated = null;
                List<Stop> stops = pattern.getStops();
                FrequencyEntry freq = pattern.getSingleFrequencyEntry();
                if (freq == null) continue;
                TripTimes tt = freq.tripTimes;
                int headway = freq.headway;
                for (int sidx = 0; sidx < stops.size(); sidx++) {
                    Stop stop = stops.get(sidx);
                    TimeRange existingRange = times.get(stop);
                    TimeRange reBoardRange = (existingRange != null) ? existingRange.wait(headway) : null;
                    if (rangeBeingPropagated == null) {
                        // We do not yet have a range worth propagating
                        if (reBoardRange != null) {
                            rangeBeingPropagated = reBoardRange; // this is a fresh protective copy
                        }
                    } else {
                        // We already have a range that is being propagated along the pattern.
                        // We are certain sidx >= 1 here because we have already boarded in a previous iteration.
                        TimeRange arrivalRange = rangeBeingPropagated.shift(tt.getRunningTime(sidx - 1));
                        if (times.add(stop, arrivalRange)) {
                            // The propagated time improved the best known time in some way.
                            stopsUpdated.add(stop);
                        }
                        // TODO handle case where arrival and departure are different
                        rangeBeingPropagated = arrivalRange.shift(tt.getDwellTime(sidx));
                        if (reBoardRange != null) {
                            rangeBeingPropagated.mergeIn(reBoardRange);
                        }
                    }
                }
            }
            /* Transfer from updated stops to adjacent stops before beginning the next round.
               Iterate over a protective copy because we add more stops to the updated list during iteration. */
            if ( ! graph.hasDirectTransfers) {
                throw new RuntimeException("Requires the SimpleTransfers generated in long distance mode.");
            }
            for (Stop stop : Lists.newArrayList(stopsUpdated)) {
                Collection<Edge> outgoingEdges = graph.index.stopVertexForStop.get(stop).getOutgoing();
                for (SimpleTransfer transfer : Iterables.filter(outgoingEdges, SimpleTransfer.class)) {
                    Stop targetStop = ((TransitStop) transfer.getToVertex()).getStop();
                    double walkTime = transfer.getDistance() / request.walkSpeed;
                    TimeRange rangeAfterTransfer = times.get(stop).shift((int)walkTime);
                    if (times.add(targetStop, rangeAfterTransfer)) {
                        stopsUpdated.add(targetStop);
                    }
                }
            }
        }
        LOG.info("Done with transit.");
        LOG.info("Propagating from transit stops to the street network...");
        // Grab a cached map of distances to street intersections from each transit stop
        StopTreeCache stopTreeCache = graph.index.getStopTreeCache();
        // Iterate over all stops that were reached in the transit part of the search
        for (Stop stop : times) {
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            // Iterate over street intersections in the vicinity of this particular transit stop.
            // Shift the time range at this transit stop, merging it into that for all reachable street intersections.
            TimeRange rangeAtTransitStop = times.get(stop);
            TObjectIntMap<Vertex> distanceToVertex = null; // FIXME stopTreeCache.getDistancesForStop(tstop);
            for (TObjectIntIterator<Vertex> iter = distanceToVertex.iterator(); iter.hasNext(); ) {
                iter.advance();
                Vertex vertex = iter.key();
                // distance in meters over walkspeed in meters per second --> seconds
                int egressWalkTimeSeconds = (int) (iter.value() / request.walkSpeed);
                if (egressWalkTimeSeconds > request.maxWalkTime * 60) {
                    continue;
                }
                TimeRange propagatedRange = rangeAtTransitStop.shift(egressWalkTimeSeconds);
                TimeRange existingTimeRange = propagatedTimes.get(vertex);
                if (existingTimeRange == null) {
                    propagatedTimes.put(vertex, propagatedRange);
                } else {
                    existingTimeRange.mergeIn(propagatedRange);
                }
            }
        }
        LOG.info("Done with propagation.");
        TimeSurface.RangeSet result = TimeSurface.makeSurfaces(this);
        LOG.info("Done making time surfaces.");
        return result;
    }

    /**
     * Perform an on-street search around a point with a specific mode to find nearby stops.
     * TODO merge with NearbyStopFinder
     */
    private TObjectIntMap<Stop> findClosestStops(final TraverseMode mode) {
        RoutingRequest rr = new RoutingRequest(mode);
        GenericLocation gl = new GenericLocation(request.fromLat, request.fromLon);
        rr.from = gl;
        // FIXME destination must be set, even though this is meaningless for one-to-many
        rr.to = gl;
        rr.setRoutingContext(graph);
        // Set batch after context, so both origin and dest vertices will be found.
        rr.batch = (true);
        rr.walkSpeed = request.walkSpeed;
        // RR dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        int minAccessTime = 0;
        int maxAccessTime = request.maxWalkTime;
        rr.worstTime = (rr.dateTime + maxAccessTime * 60);
        AStar astar = new AStar();
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.setNumItineraries(1);
        StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(mode, minAccessTime * 60);
        astar.setTraverseVisitor(visitor);
        astar.getShortestPathTree(rr, 5); // timeout in seconds
        rr.cleanup();
        return visitor.stopsFound;
    }

    // TODO merge with NearbyStopFinder
    static class StopFinderTraverseVisitor implements TraverseVisitor {
        TraverseMode mode;
        int minTravelTimeSeconds = 0;
        TObjectIntMap<Stop> stopsFound = new TObjectIntHashMap();
        public StopFinderTraverseVisitor(TraverseMode mode, int minTravelTimeSeconds) {
            this.mode = mode;
            this.minTravelTimeSeconds = minTravelTimeSeconds;
        }
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        // Accumulate stops as the search runs.
        @Override public void visitVertex(State state) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof TransitStop) {
                Stop stop = ((TransitStop)vertex).getStop();
                if (stopsFound.containsKey(stop)) return; // record only the closest stop in each cluster
                stopsFound.put(stop, (int) state.getElapsedTimeSeconds());
            }
        }
    }

    /** Given a minimum and maximum at a starting vertex, build an on-street SPT and propagate those values outward. */
    class ExtremaPropagationTraverseVisitor implements TraverseVisitor {
        final TimeRange range0;
        ExtremaPropagationTraverseVisitor(TimeRange range0) {
            this.range0 = range0;
        }
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        @Override public void visitVertex(State state) {
            TimeRange propagatedRange = range0.shift((int) state.getElapsedTimeSeconds());
            Vertex vertex = state.getVertex();
            TimeRange existing = propagatedTimes.get(vertex);
            if (existing == null) {
                propagatedTimes.put(vertex, propagatedRange);
            } else {
                existing.mergeIn(propagatedRange);
            }
        }
    }

    public void cleanup() { /* DO NOTHING */ }

}

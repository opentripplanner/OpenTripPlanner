package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.collect.Sets;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.geotools.xml.xsi.XSISimpleTypes.Int;
import org.mapdb.Fun.Tuple2;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.TimeSurface.RangeSet;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.profile.ProfileState.Type;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Profile routing using a round-based approach, more or less like RAPTOR (http://research.microsoft.com/pubs/156567/raptor_alenex.pdf)
 * @author mattwigway
 *
 */
public class RoundBasedProfileRouter {
    public final Graph graph;
    public final ProfileRequest request;
    public TimeWindow window;
    
    public final int TIMEOUT = 60;
    /** the maximum number of rounds. this is the same as the maximum number of boarding */
    public final int MAX_ROUNDS = 3;
    
    public static final int CUTOFF_SECONDS = 180 * 60;
    
    public static boolean RETAIN_PATTERNS = false;
    
    private static final Logger LOG = LoggerFactory.getLogger(RoundBasedProfileRouter.class);
    
    public Multimap<TransitStop, ProfileState> retainedStates = HashMultimap.create();
    
    /** the routing results */
    public RangeSet timeSurfaceRangeSet;  
    
    public RoundBasedProfileRouter (Graph graph, ProfileRequest request) {
        this.graph = graph;
        this.request = request;
    }
    
    public void route () {
        LOG.info("access modes: {}", request.accessModes);
        LOG.info("egress modes: {}", request.egressModes);
        LOG.info("direct modes: {}", request.directModes);
        
        // TimeWindow could constructed in the caller, which does have access to the graph index.
        this.window = new TimeWindow(request.fromTime, request.toTime, graph.index.servicesRunning(request.date));
            
        // Establish search timeouts
        long searchBeginTime = System.currentTimeMillis();
        long abortTime = searchBeginTime + TIMEOUT * 1000;
        
        LOG.info("Finding access/egress paths.");
        // Look for stops that are within a given time threshold of the origin and destination
        // Find the closest stop on each pattern near the origin and destination
        // TODO consider that some stops may be closer by one mode than another
        // and that some stops may be accessible by one mode but not another
        
        ProfileStateStore store = RETAIN_PATTERNS ? new MultiProfileStateStore() : new SingleProfileStateStore();
        
        for (ProfileState ps : findInitialStops(false)) {
            store.put(ps);
        }
        
        LOG.info("Found {} initial stops", store.size());
       
        // note: we do not add the found stops to retainedStates, because, if you are making a zero-transfer trip,
        // we don't want to generate trips that are artificially forced to go past a transit stop.
        ROUNDS: for (int round = 0; round < MAX_ROUNDS; round++) {
            long roundStart = System.currentTimeMillis();
            LOG.info("Begin round {}; {} stops to explore", round, store.size());
            
            ProfileStateStore previousStore = store;
            store = RETAIN_PATTERNS ? new MultiProfileStateStore((MultiProfileStateStore) store) : new SingleProfileStateStore((SingleProfileStateStore) store);
        
            Set<TripPattern> patternsToExplore = Sets.newHashSet();
            
            // explore all of the patterns at the stops visited on the previous round
            for (TransitStop tstop : previousStore.keys()) {
                Collection<TripPattern> patterns = graph.index.patternsForStop.get(tstop.getStop());
                patternsToExplore.addAll(patterns);
            }
            
            LOG.info("Exploring {} patterns", patternsToExplore.size());
            
            // propagate all of the bounds down each pattern
            PATTERNS: for (final TripPattern pattern : patternsToExplore) {
                STOPS: for (int i = 0; i < pattern.stopVertices.length; i++) {
                    if (!previousStore.containsKey(pattern.stopVertices[i]))
                        continue STOPS;
                    
                    Collection<ProfileState> statesToPropagate;                    
                    // only propagate nondominated states
                    statesToPropagate = previousStore.get(pattern.stopVertices[i]);
                    
                    // don't propagate states that use the same pattern
                    statesToPropagate = Collections2.filter(statesToPropagate, new Predicate<ProfileState> () {

                        @Override
                        public boolean apply(ProfileState input) {
                            // don't reboard same pattern, and don't board patterns that are better boarded elsewhere
                            return !input.containsPattern(pattern) &&
                                    (input.targetPatterns == null || input.targetPatterns.contains(pattern));
                        }
                        
                    });
                    
                    if (statesToPropagate.isEmpty())
                        continue STOPS;         
                    
                    int minWaitTime = Integer.MAX_VALUE;
                    int maxWaitTime = Integer.MIN_VALUE;
                    
                    // TODO: scheduled transfers. For scheduled transfers, recall that it depends on from whence you have come
                    // (i.e. the transfer time is different for the initial boarding than transfers)
                    for (FrequencyEntry freq : pattern.scheduledTimetable.frequencyEntries) {
                        if (freq.exactTimes) {
                            throw new IllegalStateException("Exact times not yet supported in profile routing.");
                        }
                        int overlap = window.overlap(freq.startTime, freq.endTime, freq.tripTimes.serviceCode);
                        if (overlap > 0) {
                            if (freq.headway > maxWaitTime) maxWaitTime = freq.headway;
                            // if any frequency-based trips are running a wait of 0 is always possible, because it could come
                            // just as you show up at the stop.
                            minWaitTime = 0;
                        }
                    }
                    
                    DESTSTOPS: for (int j = i + 1; j < pattern.stopVertices.length; j++) {
                        // how long does it take to ride this trip from i to j?
                        int minRideTime = Integer.MAX_VALUE;
                        int maxRideTime = Integer.MIN_VALUE;
                        
                        // how long does it take to get to stop j from stop i?
                        for (TripTimes tripTimes : pattern.scheduledTimetable.tripTimes) {
                            int depart = tripTimes.getDepartureTime(i);
                            int arrive = tripTimes.getArrivalTime(j);
                            if (window.includes (depart) && 
                                window.includes (arrive) && 
                                window.servicesRunning.get(tripTimes.serviceCode)) {
                                int t = arrive - depart;
                                if (t < minRideTime) minRideTime = t;
                                if (t > maxRideTime) maxRideTime = t;
                            }
                        }
                        /* Do the same thing for any frequency-based trips. */
                        for (FrequencyEntry freq : pattern.scheduledTimetable.frequencyEntries) {
                            TripTimes tt = freq.tripTimes;
                            int overlap = window.overlap(freq.startTime, freq.endTime, tt.serviceCode);
                            if (overlap == 0) continue;
                            int depart = tt.getDepartureTime(i);
                            int arrive = tt.getArrivalTime(j);
                            int t = arrive - depart;
                            if (t < minRideTime) minRideTime = t;
                            if (t > maxRideTime) maxRideTime = t;
                        }
                        
                        if (minWaitTime == Integer.MAX_VALUE || maxWaitTime == Integer.MIN_VALUE ||
                                minRideTime == Integer.MAX_VALUE || maxRideTime == Integer.MIN_VALUE)
                            // no trips in window that arrive at stop
                            continue DESTSTOPS;
                        
                        if (minRideTime < 0 || maxRideTime < 0) {
                            LOG.error("Pattern {} travels backwards in time between stop {} and {}",
                                    pattern, pattern.stopVertices[i].getStop(), pattern.stopVertices[j].getStop());
                            continue DESTSTOPS;
                        }
                        
                        // note: unnecessary variance in the scheduled case. It is entirely possible that the max wait and the max ride time
                        // cannot occur simultaneously.
                        
                        // propagate every profile state that we picked up at stop i to stop j
                        // we've already checked to ensure we're not reboarding the same pattern
                        for (ProfileState ps : statesToPropagate) {
                            ProfileState ps2 = ps.propagate(minWaitTime + minRideTime, maxWaitTime + maxRideTime);
                            
                            if (ps2.upperBound > CUTOFF_SECONDS)
                                continue;
                            
                            ps2.stop = pattern.stopVertices[j];
                            ps2.accessType = Type.TRANSIT;
                            
                            if (RETAIN_PATTERNS)
                                ps2.patterns = new TripPattern[] { pattern };
                            
                            store.put(ps2);  
                        }
                    }
                }
            }
            
            // merge states that came from the same stop.
            if (RETAIN_PATTERNS) {
                LOG.info("Round completed, merging similar states");
                ((MultiProfileStateStore) store).mergeStates();
            }
            
            for (ProfileState ps : store.getAll()) {
                retainedStates.put(ps.stop, ps);
            }
            
            if (round == MAX_ROUNDS - 1) {
                LOG.info("Finished round {} in {} seconds", round, (System.currentTimeMillis() - roundStart) / 1000);
                break ROUNDS;
            }
            
            // propagate states to nearby stops (transfers)
            LOG.info("Finding transfers . . .");               
            // avoid concurrent modification
            Set<TransitStop> touchedStopKeys = new HashSet<TransitStop>(store.keys());
            for (TransitStop tstop : touchedStopKeys) {
                List<Tuple2<TransitStop, Integer>> accessTimes = Lists.newArrayList();
                
                // find transfers for the stop
                for (Edge e : tstop.getOutgoing()) {
                    if (e instanceof SimpleTransfer) {
                        SimpleTransfer t = (SimpleTransfer) e;
                        int time = (int) (t.getDistance() / request.walkSpeed);
                        accessTimes.add(new Tuple2((TransitStop) e.getToVertex(), time));
                    }
                }
                
                // only transfer from nondominated states. only transfer to each pattern once
                Collection<ProfileState> statesAtStop = store.get(tstop);
                
                TObjectIntHashMap<TripPattern> minBoardTime = new TObjectIntHashMap<TripPattern>(1000, .75f, Integer.MAX_VALUE);
                Map<TripPattern, ProfileState> optimalBoardState = Maps.newHashMap();
                
                List<ProfileState> xferStates = Lists.newArrayList();
                
                
                // make a hashset of the patterns that stop here, because we don't want to transfer to them at another stop
                HashSet<TripPattern> patternsAtSource = new HashSet<TripPattern>(graph.index.patternsForStop.get(tstop.getStop()));
                
                for (ProfileState ps : statesAtStop) {
                    for (Tuple2<TransitStop, Integer> atime : accessTimes) {
                        ProfileState ps2 = ps.propagate(atime.b);
                        ps2.accessType = Type.TRANSFER;
                        ps2.stop = atime.a;
                        // note that we do not reset pattern, as we still don't want to transfer from a pattern to itself.
                        // (TODO: is this true? loop routes?)
                        
                        for (TripPattern patt : graph.index.patternsForStop.get(atime.a.getStop())) {
                            // don't transfer to patterns that we can board at this stop.
                            if (patternsAtSource.contains(patt))
                                continue;
                            
                            if (atime.b < minBoardTime.get(patt)) {
                                minBoardTime.put(patt, atime.b);
                                optimalBoardState.put(patt, ps2);
                            }
                        }

                        xferStates.add(ps2);
                    }
                }
                
                for (Entry<TripPattern, ProfileState> e : optimalBoardState.entrySet()) {
                    ProfileState ps = e.getValue();
                    if (ps.targetPatterns == null)
                        ps.targetPatterns = Sets.newHashSet();
                    
                    ps.targetPatterns.add(e.getKey());
                }
                
                for (ProfileState ps : xferStates) {
                    if (ps.targetPatterns != null && !ps.targetPatterns.isEmpty()) {
                        store.put(ps);
                    }
                }
            }
            
            LOG.info("Finished round {} in {} seconds", round, (System.currentTimeMillis() - roundStart) / 1000);
        }
        
        LOG.info("Finished profile routing in {} seconds", (System.currentTimeMillis() - searchBeginTime) / 1000);
        
        makeSurfaces();
        
        LOG.info("Finished analyst request in {} seconds total", (System.currentTimeMillis() - searchBeginTime) / 1000);
    }
    
    /** from a collection of profile states at a transit stop, return a collection of all the nondominated states */
    public Collection<ProfileState> nondominated(Collection<ProfileState> original, TransitStop tstop) {
        // find the min upper bound
        int minUpperBound = Integer.MAX_VALUE;
        
        // TODO optimization: retain min upper bound as states are added.
        for (ProfileState ps : original) {
            if (ps.upperBound < minUpperBound)
                minUpperBound = ps.upperBound;
        }
        
        // we also check against states that were found in previous rounds and have already been propagated;
        // no reason to propagate again.
        for (ProfileState ps : retainedStates.get(tstop)) {
            if (ps.upperBound < minUpperBound)
                minUpperBound = ps.upperBound;
        }
        
        for (Iterator<ProfileState> it = original.iterator(); it.hasNext();) {
            ProfileState ps = it.next();
            if (ps.lowerBound > minUpperBound || ps.lowerBound > CUTOFF_SECONDS)
                it.remove();
        }
        
        return original;
    }
    
    /** find the boarding stops */
    private Collection<ProfileState> findInitialStops(boolean dest) {
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.accessModes : request.egressModes;
        
        List<ProfileState> stops = Lists.newArrayList();
        
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        rr.walkSpeed = request.walkSpeed;
        rr.to = rr.from;
        rr.setRoutingContext(graph);
        
        // RoutingRequest dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + request.maxWalkTime * 60);
        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);
        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                ProfileState ps = new ProfileState();
                ps.lowerBound = ps.upperBound = (int) s.getElapsedTimeSeconds();
                ps.stop = tstop;
                ps.accessType = Type.STREET;
                stops.add(ps);
            }
        }
        
        Map<TripPattern, ProfileState> optimalBoardingLocation = Maps.newHashMap();
        TObjectIntMap<TripPattern> minBoardTime = new TObjectIntHashMap<TripPattern>(100, 0.75f, Integer.MAX_VALUE);
        
        // Only board patterns at the closest possible stop
        for (ProfileState ps : stops) {
            for (TripPattern pattern : graph.index.patternsForStop.get(ps.stop.getStop())) {
                if (ps.lowerBound < minBoardTime.get(pattern)) {
                    optimalBoardingLocation.put(pattern, ps);
                    minBoardTime.put(pattern, ps.lowerBound);
                }                    
            }
            
            ps.targetPatterns = Sets.newHashSet();
        }
        
        LOG.info("Found {} reachable stops, filtering to only board at closest stops", stops.size());
        
        for (Entry<TripPattern, ProfileState> e : optimalBoardingLocation.entrySet()) {
            e.getValue().targetPatterns.add(e.getKey());
        }
        
        for (Iterator<ProfileState> it = stops.iterator(); it.hasNext();) {
            if (it.next().targetPatterns.isEmpty())
                it.remove();
        }
        
        rr.cleanup();
        
        return stops;
    }
    
    /** analyst mode: propagate to street network */
    private void makeSurfaces() {
        LOG.info("Propagating from transit stops to the street network...");
        List<State> lower = Lists.newArrayList();
        List<State> upper = Lists.newArrayList();
        List<State> avg = Lists.newArrayList();
        
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.batch = (true);
        rr.from = new GenericLocation(request.fromLat, request.fromLon);
        rr.setRoutingContext(graph);
        rr.longDistance = true;
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.setNumItineraries(1);
        rr.worstTime = rr.dateTime + CUTOFF_SECONDS;
       
        long startTime = rr.dateTime;
        
        State origin = new State(rr);
        
        // Iterate over all rides at all clusters
        // Note that some may be dominated, but it doesn't matter
        // Multi-origin Dijkstra search; preinitialize the queue with states at each transit stop
        for (Collection<ProfileState> pss : retainedStates.asMap().values()) {
            TransitStop tstop = null;
            int lowerBound = Integer.MAX_VALUE;
            int upperBound = Integer.MAX_VALUE;
            
            for (ProfileState ps : pss) {
                if (tstop == null) tstop = ps.stop;
                if (ps.lowerBound < lowerBound) lowerBound = ps.lowerBound;
                if (ps.upperBound < upperBound) upperBound = ps.upperBound;
            }
            
            if (lowerBound == Integer.MAX_VALUE || upperBound == Integer.MAX_VALUE)
                throw new IllegalStateException("Invalid bound!");
               
            lower.add(new State(tstop, null, lowerBound + startTime, startTime, rr));
            upper.add(new State(tstop, null, upperBound + startTime, startTime, rr));
            
            // TODO extremely incorrect hack!
            avg.add(new State(tstop, null, (upperBound + lowerBound) / 2 + startTime, startTime, rr));
        }
        
        // get direct trips as well
        lower.add(origin);
        upper.add(origin);
        avg.add(origin);
        
        // create timesurfaces
        timeSurfaceRangeSet = new TimeSurface.RangeSet();

        AStar astar = new AStar();
        timeSurfaceRangeSet.min = new TimeSurface(astar.getShortestPathTree(rr, 20, null, lower), false);
        astar = new AStar();
        timeSurfaceRangeSet.max = new TimeSurface(astar.getShortestPathTree(rr, 20, null, upper), false);
        astar = new AStar();
        timeSurfaceRangeSet.avg = new TimeSurface(astar.getShortestPathTree(rr, 20, null, avg), false);
        
        rr.cleanup();
        
        LOG.info("Done with propagation.");
        /* Store the results in a field in the router object. */

    }
    
    public void cleanup () {
        // TODO
    }
}

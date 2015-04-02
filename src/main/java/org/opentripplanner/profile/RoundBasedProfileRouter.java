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
    
    public static final int CUTOFF_SECONDS = 90 * 60;
    
    public static final boolean RETAIN_PATTERNS = false;
    
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
        
        Multimap<TransitStop, ProfileState> touchedStops = findInitialStops(false);
        
        LOG.info("Found {} initial stops", touchedStops.size());
        
        // note: we do not add the found stops to retainedStates, because, if you are making a zero-transfer trip,
        // we don't want to generate trips that are artificially forced to go past a transit stop.
        ROUNDS: for (int round = 0; round < MAX_ROUNDS; round++) {
            long roundStart = System.currentTimeMillis();
            LOG.info("Begin round {}; {} stops to explore", round, touchedStops.size());
            
            Multimap<TransitStop, ProfileState> previousStops = touchedStops;
            touchedStops = ArrayListMultimap.create();
        
            Set<TripPattern> patternsToExplore = Sets.newHashSet();
            
            // explore all of the patterns at the stops visited on the previous round
            // optimization: on the first round, board each pattern only once, at the nearest stop
            Map<TripPattern, TransitStop> optimalBoardingLocation = null;
            TObjectIntMap<TripPattern> minBoardTime = null;
            
            if (round == 0) {
                minBoardTime = new TObjectIntHashMap<TripPattern>(100, .75f, Integer.MAX_VALUE);
                optimalBoardingLocation = Maps.newHashMap();
            }
                
            
            for (TransitStop tstop : previousStops.keySet()) {
                Collection<TripPattern> patterns = graph.index.patternsForStop.get(tstop.getStop());
                patternsToExplore.addAll(patterns);
                
                if (round == 0) {
                    for (TripPattern tp : patterns) {
                        ProfileState exemplar = previousStops.get(tstop).iterator().next();
                        if (exemplar.lowerBound < minBoardTime.get(tp)) {
                            minBoardTime.put(tp, exemplar.lowerBound);
                            optimalBoardingLocation.put(tp, tstop);
                        }
                    }
                }
            }
            
            LOG.info("Exploring {} patterns", patternsToExplore.size());
            
            // propagate all of the bounds down each pattern
            PATTERNS: for (final TripPattern pattern : patternsToExplore) {
                STOPS: for (int i = 0; i < pattern.stopVertices.length; i++) {
                    if (!previousStops.containsKey(pattern.stopVertices[i]))
                        continue STOPS;
                    
                    // optimization: don't board the same patterns many times at the origin
                    if (round == 0 && optimalBoardingLocation.get(pattern) != pattern.stopVertices[i])
                        continue STOPS;
                    
                    // only propagate nondominated states
                    Collection<ProfileState> statesToPropagate = nondominated(previousStops.get(pattern.stopVertices[i]), pattern.stopVertices[i]);
                    
                    // don't 
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
                        // how long does it take to ride this trip from i to j
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
                        
                        // note: unnecessary variance in the scheduled case. It is entirely possible that the max wait and the max ride time
                        // cannot occur simultaneously.
                        
                        // propagate every profile state that we picked up at stop i to stop j
                        // we've already checked to ensure we're not reboarding the same pattern
                        for (ProfileState ps : statesToPropagate) {
                            ProfileState ps2 = ps.propagate(minWaitTime + minRideTime, maxWaitTime + maxRideTime);
                            ps2.stop = pattern.stopVertices[j];
                            ps2.accessType = Type.TRANSIT;
                            ps2.patterns = new TripPattern[] { pattern };
                            touchedStops.put(ps2.stop, ps2);
                        }
                    }
                }
            }
            
            // merge single path common trunks right here, to avoid propagating too many bounds
            LOG.info("Round completed, merging similar states");
            
            Set<TransitStop> touchedStopVertices = new HashSet<TransitStop>(touchedStops.keySet());
            for (TransitStop tstop : touchedStopVertices) {
                Collection<ProfileState> pss = nondominated(touchedStops.get(tstop), tstop);
                
                if (pss.isEmpty())
                    continue;
                
                if (!RETAIN_PATTERNS) {
                    ProfileState st = ProfileState.merge(pss, false);
                    pss.clear();
                    pss.add(st);
                    continue;
                }
                
                // find states that have come from the same place
                Multimap<ProfileState, ProfileState> foundStates = ArrayListMultimap.create();
                
                for (Iterator<ProfileState> it = pss.iterator(); it.hasNext();) {
                    ProfileState ps = it.next();
                    foundStates.put(ps.previous, ps);
                }
                
                pss.clear();
                
                // merge them now
                for (Collection<ProfileState> states : foundStates.asMap().values()) {                   
                    if (states.size() == 1)
                        pss.addAll(states);
                    else
                        pss.add(ProfileState.merge(states, true));
                }
            }
            
            // retain the states found here. we do this before finding transfers because you wouldn't transfer and then
            // not board a vehicle, so states immediately after a transfer are not final.
            for (Entry<TransitStop, ProfileState> e : touchedStops.entries()) {
                retainedStates.put(e.getKey(), e.getValue());
            }
            
            if (round == MAX_ROUNDS - 1) {
                LOG.info("Finished round {} in {} seconds", round, (System.currentTimeMillis() - roundStart) / 1000);
                break ROUNDS;
            }
            
            // propagate states to nearby stops (transfers)
            LOG.info("Finding transfers . . .");
            // avoid concurrent modification
            Set<TransitStop> touchedStopKeys = new HashSet<TransitStop>(touchedStops.keySet());
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
                Collection<ProfileState> statesAtStop = nondominated(touchedStops.get(tstop), tstop);
                
                minBoardTime = new TObjectIntHashMap<TripPattern>(1000, .75f, Integer.MAX_VALUE);
                Map<TripPattern, ProfileState> optimalBoardState = Maps.newHashMap();
                
                List<ProfileState> xferStates = Lists.newArrayList();
                
                for (ProfileState ps : statesAtStop) {
                    for (Tuple2<TransitStop, Integer> atime : accessTimes) {
                        ProfileState ps2 = ps.propagate(atime.b);
                        ps2.accessType = Type.TRANSFER;
                        ps2.stop = atime.a;
                        // note that we do not reset pattern, as we still don't want to transfer from a pattern to itself.
                        // (TODO: is this true? loop routes?)

                        for (TripPattern patt : graph.index.patternsForStop.get(tstop.getStop())) {
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
                        touchedStops.put(ps.stop, ps);
                    }
                }
            }
            
            LOG.info("Finished round {} in {} seconds", round, (System.currentTimeMillis() - roundStart) / 1000);
        }
        
        LOG.info("Finished profile routing in {} seconds", (System.currentTimeMillis() - searchBeginTime) / 1000);
        
        makeSurfaces();
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
    private Multimap<TransitStop, ProfileState> findInitialStops(boolean dest) {
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.accessModes : request.egressModes;
        
        Multimap<TransitStop, ProfileState> stops = ArrayListMultimap.create();
        
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
                stops.put(tstop, ps);
            }
        }
        
        return stops;       
    }
    
    /** analyst mode: propagate to street network */
    private void makeSurfaces() {
        LOG.info("Propagating from transit stops to the street network...");
        // A map to store the travel time to each vertex
        TimeSurface minSurface = new TimeSurface(this);
        TimeSurface avgSurface = new TimeSurface(this);
        TimeSurface maxSurface = new TimeSurface(this);
        // Grab a cached map of distances to street intersections from each transit stop
        StopTreeCache stopTreeCache = graph.index.getStopTreeCache();
        // Iterate over all rides at all clusters
        // Note that some may be dominated, but it doesn't matter
        for (Entry<TransitStop, ProfileState> entry : retainedStates.entries()) {
            ProfileState ps = entry.getValue();
            int lb0 = ps.lowerBound;
            int ub0 = ps.upperBound;

            TransitStop tstop = entry.getKey();
            // Iterate over street intersections in the vicinity of this particular transit stop.
            // Shift the time range at this transit stop, merging it into that for all reachable street intersections.
            TObjectIntMap<Vertex> distanceToVertex = stopTreeCache.getDistancesForStop(tstop);
            for (TObjectIntIterator<Vertex> iter = distanceToVertex.iterator(); iter.hasNext(); ) {
                iter.advance();
                Vertex vertex = iter.key();
                // distance in meters over walkspeed in meters per second --> seconds
                int egressWalkTimeSeconds = (int) (iter.value() / request.walkSpeed);
                if (egressWalkTimeSeconds > request.maxWalkTime * 60) {
                    continue;
                }
                int propagated_min = lb0 + egressWalkTimeSeconds;
                int propagated_max = ub0 + egressWalkTimeSeconds;
                int propagated_avg = (int)(((long) propagated_min + propagated_max) / 2); // FIXME HACK
                int existing_min = minSurface.times.get(vertex);
                int existing_max = maxSurface.times.get(vertex);
                int existing_avg = avgSurface.times.get(vertex);
                // FIXME this is taking the least lower bound and the least upper bound
                // which is not necessarily wrong but it's a crude way to perform the combination
                if (existing_min == TimeSurface.UNREACHABLE || existing_min > propagated_min) {
                    minSurface.times.put(vertex, propagated_min);
                }
                if (existing_max == TimeSurface.UNREACHABLE || existing_max > propagated_max) {
                    maxSurface.times.put(vertex, propagated_max);
                }
                if (existing_avg == TimeSurface.UNREACHABLE || existing_avg > propagated_avg) {
                    avgSurface.times.put(vertex, propagated_avg);
                }
            }
        }
        LOG.info("Done with propagation.");
        /* Store the results in a field in the router object. */
        timeSurfaceRangeSet = new TimeSurface.RangeSet();
        timeSurfaceRangeSet.min = minSurface;
        timeSurfaceRangeSet.max = maxSurface;
        timeSurfaceRangeSet.avg = avgSurface;
    }
    
    public void cleanup () {
        // TODO
    }
}

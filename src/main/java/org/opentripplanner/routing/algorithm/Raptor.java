package org.opentripplanner.routing.algorithm;

import gnu.trove.iterator.TObjectIntIterator;
import jersey.repackaged.com.google.common.collect.Sets;
import org.joda.time.LocalDate;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimeSubset;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Implements the RAPTOR algorithm; see http://research.microsoft.com/pubs/156567/raptor_alenex.pdf */
public class Raptor {
    public final Graph graph;
    public final int maxTransfers;
    public final float walkSpeed;
    
    private RaptorStateStore store;
    
    private static final Logger LOG = LoggerFactory.getLogger(Raptor.class);
    
    private HashSet<TripPattern> markedPatterns = Sets.newHashSet();
    private HashSet<TransitStop> markedStops = Sets.newHashSet();
    
    private Map<TripPattern, TripTimeSubset> times;
    
    /**
     * the date of the search
     * We could infer this from the routing request but we want to eventually get rid of the routing request
     * in profile mode.
     */
    private LocalDate date;
    
    /** Initialize a RAPTOR router from an existing RaptorStateStore, a routing request, and the timetables of active trips */
    public Raptor(Graph graph, int maxTransfers, float walkSpeed, RaptorStateStore store, int startTime, LocalDate date, Map<TripPattern, TripTimeSubset> timetables) {
    	this.graph = graph;    	
    	this.maxTransfers = maxTransfers;
    	this.times = timetables;
    	this.walkSpeed = walkSpeed;
    	this.date = date;
    	this.store = store;
    	
    	for (TransitStop tstop : store.getTouchedStopsIncludingTransfers()) {
    		markedPatterns.addAll(graph.index.patternsForStop.get(tstop.getStop()));    		
    	}
    }
    
    /** Initialize a RAPTOR router with only a routing request */
    /*
    public Raptor(RoutingRequest options) {
    	this.options = options;
    	this.store = new StatePreservingRaptorStateStore();
    	
    	findInitialStops();
    }
    */
    
    /** Get a state at the destination, for a point-to-point search */
    /*
    public ShortestPathTree getShortestPathTree () {
    	run();
    	return finishSearch();
    }
    */
    
    public void run () {
    	// the initial walk is a "round"
    	store.proceed();
        for (int round = 0; round <= maxTransfers; round++) {
			// LOG.info("round {}", round);
            if (!doRound(round == maxTransfers))
            	break;
        }
    }
    
    /**
     * perform one round of a RAPTOR search.
     * @return whether this round relaxed any bounds.
     */
    private boolean doRound (boolean isLast) {    	
    	//LOG.info("Begin RAPTOR round");
        // TODO: filter to routes that are running
        // TODO: implement the rest of the optimizations in the paper, in particular not going past the destination
    	Set<TripPattern> oldMarkedPatterns = markedPatterns;
    	markedPatterns = Sets.newHashSet();
    	markedStops.clear();
    	
    	//LOG.info("Exploring {} patterns", oldMarkedPatterns.size());
    	
    	// Loop over the patterns we marked in the previous iteration
        PATTERNS: for (TripPattern tp : oldMarkedPatterns) {
            STOPS: for (int i = 0; i < tp.stopVertices.length; i++) {
                int time = store.getPrev(tp.stopVertices[i]);
                if (time == Integer.MAX_VALUE)
                    continue STOPS;
                
                propagate(time, tp, i);
            }
        }
        
        // Find all possible transfers. No need to do this on the last round though.
    	if (!isLast) {
			// we proceed both before and after finding transfers, because finding transfers is effectively
			// another "round." We used to use bestStops in the profile store to track this, but that doesn't
			// work with dynamic programming/range-RAPTOR mode because bestStops may contain optimal trips found in
			// future rounds,
			store.proceed();
    		findTransfers();
    		store.proceed();
    	}
        
        // check if it changed
        return !markedPatterns.isEmpty();
    }
    
    /** Find all the transfers from the last round to this round */
    public void findTransfers () {
        // only find transfers from stops that were touched in this round.
        for (TransitStop tstop : markedStops) {  
        	
        	// we know that the best time for this stop was updated on the last round because we mark stops,
			// so we can't be transferring from a transfer that was copied forward.
			// We are using getPrev rather than getTime because bestStops may contain information from future rounds
			// in dynamic programming/range-RAPTOR mode.
        	int timeAtOriginStop = store.getPrev(tstop);
        	
            for (Edge e : tstop.getOutgoing()) {
                if (e instanceof SimpleTransfer) {
                	TransitStop to = (TransitStop) e.getToVertex();
                	int timeAtDestStop = (int) (timeAtOriginStop + e.getDistance() / walkSpeed);
                	
                	if (store.put(to, timeAtDestStop, true)) {
                		markedPatterns.addAll(graph.index.patternsForStop.get(to.getStop()));
                	}
                }
            }
        }
    }
    
    /** Propagate a time down a trip pattern, using frequencies if available, otherwise schedules. */
    public void propagate (int time, TripPattern tripPattern, int stopIndex) {
    	
    	if (!propagateFrequencies(time, tripPattern, stopIndex))
    		propagateSchedules(time, tripPattern, stopIndex);
    }
    
    /** Propagate times using frequency entries */
    public boolean propagateFrequencies (int time, TripPattern tripPattern, int stopIndex) {    	
    	// first check for frequency trips
    	boolean foundFrequencyEntry = false;
    	
    	// Figure out what the best frequency entry is. Note that it may be worthwhile to wait for a frequency based trip
    	// if we're near but before the start of the window.
    	int bestFreqBoardTime = Integer.MAX_VALUE;
    	FrequencyEntry bestFreq = null;
    	
    	for (FrequencyEntry freq : tripPattern.scheduledTimetable.frequencyEntries) {
    		if (!graph.index.servicesRunning(date).get(freq.tripTimes.serviceCode))
    			continue;
    		
    		// we set this here rather than below the time check, because in Analyst we run
    		// RAPTOR many times and we don't want the same patterns switching back and forth
    		// from frequency to scheduled. (or do we?)
    		foundFrequencyEntry = true;
    		
    		// the first stop on the trip must occur between start_time and end_time,
    		// so offset start_time and end_time the appropriate amount to see if it applies.
    		int offsetSecs = freq.tripTimes.getScheduledDepartureTime(stopIndex);
    		
    		// there are no more trips on this frequency entry
    		if (freq.endTime + offsetSecs < time)
    			continue;
    		
    		int boardTime = time + freq.headway;
    		
    		// we have to wait until the service starts running
    		// note that we still figure in the wait; unless we are using exact times there
    		// is no guarantee that first trip left at exactly the start time of the frequency entry,
    		// only within headway_seconds.
    		if (freq.startTime + offsetSecs > time) {
    			boardTime = freq.startTime + offsetSecs + freq.headway;
    		}
    		
    		if (boardTime < bestFreqBoardTime) {
    			bestFreqBoardTime = boardTime;
    			bestFreq = freq;
    		}
    	}
    	
    	if (bestFreq == null)
    		return foundFrequencyEntry;
    	
    	int boardOffset = bestFreq.tripTimes.getScheduledDepartureTime(stopIndex);
    	
    	for (int reachedIdx = stopIndex + 1; reachedIdx < bestFreq.tripTimes.getNumStops(); reachedIdx++) {
    		TransitStop v = tripPattern.stopVertices[reachedIdx];
    		// board time already includes headway, if we are computing the worst-case
    		int arrTime = bestFreqBoardTime + bestFreq.tripTimes.getScheduledArrivalTime(reachedIdx) - boardOffset;
    		
    		if (store.put(v, arrTime, false)) {    			
    			markedStops.add(v);
    			
    			for (TripPattern tp : graph.index.patternsForStop.get(v.getStop())) {
    				if (tp != tripPattern)
    					markedPatterns.add(tp);
    			}
       		}
    	}
    	
    	return foundFrequencyEntry;
    }
    	
    /** Propagate times using schedules. */
    public void propagateSchedules(int time, TripPattern tripPattern, int stopIndex) {
    	TripTimeSubset tts = times.get(tripPattern);
    	
    	if (tts == null)
    		return;
    	
    	// find the appropriate trip, quickly (uses a binary search)
    	int tripIndex = tts.findTripAfter(stopIndex, time);
    	
    	if (tripIndex == -1)
    		return;
    	
    	// Propagate the times
    	for (int reachedIdx = stopIndex + 1; reachedIdx < tripPattern.stopVertices.length; reachedIdx++) {
    		TransitStop v = tripPattern.stopVertices[reachedIdx];
    		int arrTime = tts.getArrivalTime(tripIndex, reachedIdx);
    		
    		if (store.put(v, arrTime, false)) {
    			for (TripPattern tp : graph.index.patternsForStop.get(v.getStop())) {
    				if (tp != tripPattern)
    					markedPatterns.add(tp);
    			}
    			
    			markedStops.add(v);
    		}
    	}
    }
    
    /** Get an iterator over all the nondominated target states of this RAPTOR search */
    public TObjectIntIterator<TransitStop> iterator () {
        return store.iterator();
    }
    
    /*
    private void findInitialStops () {
    	TraverseModeSet oldModes = options.modes;
    	long oldWorstTime = options.worstTime;
    	boolean oldBatch = options.batch;
    	options.modes = options.modes.clone();
    	options.modes.setTransit(false);
    	options.worstTime = options.dateTime + (long) (options.maxWalkDistance / options.walkSpeed);
    	options.batch = true;
    	
    	// routing context already set
    	
    	AStar astar = new AStar();    	
    	ShortestPathTree spt = astar.getShortestPathTree(options);
    	
    	for (TransitStop tstop : options.rctx.graph.index.stopVertexForStop.values()) {
    		State s = spt.getState(tstop);
    		if (s != null) {
    			if (store.put(s))
    	            markedPatterns.addAll(options.rctx.graph.index.patternsForStop.get(((TransitStop) s.getVertex()).getStop()));
    			
    		}
    	}
    	
    	options.modes = oldModes;
    	options.batch = oldBatch;
    	options.worstTime = oldWorstTime;
    }
    
    private ShortestPathTree finishSearch () {
    	Collection<State> states = Lists.newArrayList();
    	    	
    	for (Iterator<State> it = store.currentIterator(); it.hasNext();) {
    		State s = it.next();
    		states.add(s);
     	}
    	
    	TraverseModeSet oldModes = options.modes;
    	options.modes.clone();
    	options.modes.setTransit(false);
    	
    	// add the origin as well
    	states.add(new State(options));
    	
    	AStar astar = new AStar();
    	// TODO this is not efficient
    	ShortestPathTree spt = astar.getShortestPathTree(options, 10, null, states);
    	options.modes = oldModes;
    	return spt;
    }
    */
}

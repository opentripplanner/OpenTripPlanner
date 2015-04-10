package org.opentripplanner.routing.algorithm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jersey.repackaged.com.google.common.collect.Sets;

import com.google.common.collect.Lists;

import flexjson.PathExpression;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the RAPTOR algorithm; see http://research.microsoft.com/pubs/156567/raptor_alenex.pdf */
public class Raptor {
    public RaptorStateStore store;
    public RoutingRequest options;
    
    private static final Logger LOG = LoggerFactory.getLogger(Raptor.class);
    
    private HashSet<TripPattern> markedPatterns = Sets.newHashSet();
        
    /** Initialize a RAPTOR router from states at transit stops */
    public Raptor(Collection<State> states, RoutingRequest options) {
        // TODO: don't hardwire store type
        store = new PathDiscardingRaptorStateStore(options);
        this.options = options;
        
        for (State state : states) {
            store.put(state);
            markedPatterns.addAll(options.rctx.graph.index.patternsForStop.get(((TransitStop) state.getVertex()).getStop()));
        }
    }
    
    /** Initialize a RAPTOR router with only a routing request */
    public Raptor(RoutingRequest options) {
    	this.options = options;
    	this.store = new StatePreservingRaptorStateStore();
    	
    	findInitialStops();
    }
    
    /** Get a state at the destination, for a point-to-point search */
    public ShortestPathTree getShortestPathTree () {
    	run();
    	return finishSearch();
    }
    
    public void run () {
        // slightly hacky, but proceed here so that prev is fixed and we can modify current.
        store.proceed();
        for (int round = 0; round <= options.maxTransfers; round++) {
            if (!doRound())
            	break;
        }
    }
    
    /**
     * perform one round of a RAPTOR search.
     * @return whether this round relaxed any bounds.
     */
    private boolean doRound () {
    	//LOG.info("Begin RAPTOR round");
        // TODO: mark routes
        // TODO: filter to routes that are running
        // TODO: implement the rest of the optimizations in the paper, in particular not going past the destination
    	Set<TripPattern> oldMarkedPatterns = markedPatterns;
    	markedPatterns = Sets.newHashSet();
    	
    	LOG.info("Exploring {} patterns", oldMarkedPatterns.size());
    	
        PATTERNS: for (TripPattern tp : oldMarkedPatterns) {
            STOPS: for (int i = 0; i < tp.stopVertices.length; i++) {
                State s = store.getPrev(tp.stopVertices[i]);
                if (s == null)
                    continue STOPS;
                
                propagate(s, tp, i);
            }
        }
        
        // Find all possible transfers
        store.proceed();
        findTransfers();
        
        // check if it changed
        return !markedPatterns.isEmpty();
    }
    
    /** Find all the transfers from the last round to this round */
    public void findTransfers () {
        // TODO: don't transfer from things that were not updated this round
        for (Iterator<State> stateIt = store.prevIterator(); stateIt.hasNext();) {
            State s0 = stateIt.next();
            
            for (Edge e : s0.getVertex().getOutgoing()) {
                if (e instanceof SimpleTransfer) {
                    State s1 = e.traverse(s0);
                    
                    if (s1 != null) {
                        if (store.put(s1)) {
                            markedPatterns.addAll(options.rctx.graph.index.patternsForStop.get(((TransitStop) s1.getVertex()).getStop()));	
                        }
                    }
                }
            }
        }
    }
    
    /** Propagate a state down a trip pattern */
    public void propagate (State state, TripPattern tripPattern, int stopIndex) {
        PreBoardEdge pbe = null;
        TransitBoardAlight tbe = null;
        
        // find the proper PatternBoard
        TSTOP_OUTGOING: for (Edge e : state.getVertex().getOutgoing()) {
            if (e instanceof PreBoardEdge) {
                DEPART_OUTGOING: for (Edge e2 : e.getToVertex().getOutgoing()) {
                    // TODO: handle StopIndex
                	// (this is not giving incorrect results, but we explore loop routes twice)
                    if (e2 instanceof TransitBoardAlight && ((TransitBoardAlight) e2).getPattern() == tripPattern) {
                        pbe = (PreBoardEdge) e;
                        tbe = (TransitBoardAlight) e2;
                        break TSTOP_OUTGOING;
                    }
                }
            }
        }
        
        if (tbe == null)
        	return;
        
        state = pbe.traverse(state);
        
        if (state == null)
        	return;
        
        state = tbe.traverse(state);
        
        if (state == null)
        	return;
        
        // we now have a state on board a vehicle on this pattern
        // propagate it to the end of the pattern
        List<State> states = Lists.newArrayList();
        states.add(state);
        
        while (!states.isEmpty()) {
            State s0 = states.remove(states.size() - 1);
            for (Edge e : s0.getVertex().getOutgoing()) {            	
                State s1 = e.traverse(s0);
                
                if (s1 == null)
                    continue;
                
                if (s1.getVertex() instanceof TransitStop) {
                    if (store.put(s1)) {
                        markedPatterns.addAll(options.rctx.graph.index.patternsForStop.get(((TransitStop) s1.getVertex()).getStop()));
                    };
                }
                else {
                    states.add(s1);
                }
            }
        }
    }
    
    /** Get an iterator over all the nondominated target states of this RAPTOR search */
    public Iterator<State> iterator () {
        return store.currentIterator();
    }
    
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
}

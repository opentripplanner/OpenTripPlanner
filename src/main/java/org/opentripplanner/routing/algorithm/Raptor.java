package org.opentripplanner.routing.algorithm;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implements the RAPTOR algorithm; see http://research.microsoft.com/pubs/156567/raptor_alenex.pdf */
public class Raptor {
    public RaptorStateStore store;
    public RoutingRequest options;
    
    private static final Logger LOG = LoggerFactory.getLogger(Raptor.class);
        
    /** Initialize a RAPTOR router from states at transit stops */
    public Raptor(Collection<State> states, RoutingRequest options) {
        // TODO: don't hardwire store type
        store = new PathDiscardingRaptorStateStore(options);
        this.options = options;
        
        for (State state : states) {
            store.put(state);
        }
    }
    
    public void run () {
        // slightly hacky, but proceed here so that prev is fixed and we can modify current.
        store.proceed();
        for (int round = 0; round < options.maxTransfers; round++) {
            if (!doRound()) break;
        }
    }
    
    /**
     * perform one round of a RAPTOR search.
     * @return whether this round relaxed any bounds.
     */
    public boolean doRound () {
    	//LOG.info("Begin RAPTOR round");
        // TODO: mark routes
        // TODO: filter to routes that are running
        // TODO: implement the rest of the optimizations in the paper, in particular not going past the destination
        PATTERNS: for (TripPattern tp : options.rctx.graph.index.patternForId.values()) {
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
        
        // TODO check if it changed
        return true;
    }
    
    /** Find all the transfers from the last round to this round */
    public void findTransfers () {
        // TODO: don't transfer from things that were not updated this round
        for (Iterator<State> stateIt = store.prevIterator(); stateIt.hasNext();) {
            State s0 = stateIt.next();
            
            for (Edge e : s0.getVertex().getOutgoing()) {
                if (e instanceof SimpleTransfer) {
                    State s1 = e.traverse(s0);
                    
                    if (s1 != null)
                        store.put(s1);
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
                    store.put(s1);
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
}

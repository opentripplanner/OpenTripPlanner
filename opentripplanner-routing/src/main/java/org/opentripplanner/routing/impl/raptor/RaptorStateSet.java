package org.opentripplanner.routing.impl.raptor;

import java.util.HashMap;
import java.util.List;

import org.opentripplanner.routing.graph.Vertex;

public class RaptorStateSet {
    List<RaptorState>[] statesByStop;

    public List<RaptorState> getStates(RaptorStop stop) {
        return statesByStop[stop.index];
    }
    
    public HashMap<Vertex, List<RaptorState>> getStates() {
        HashMap<Vertex, List<RaptorState>> out = new HashMap<Vertex, List<RaptorState>>();
        for (int stopNo = 0; stopNo < statesByStop.length; ++stopNo) {
            List<RaptorState> states = statesByStop[stopNo];
            if (states == null || states.size() == 0) continue;
            out.put(states.get(0).stop.stopVertex, states);
        }
        
        return out;
    }
}

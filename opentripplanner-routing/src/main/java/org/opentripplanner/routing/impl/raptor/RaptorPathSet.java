/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl.raptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class RaptorPathSet {
    List<RaptorState>[] statesByStop;
    private List<RaptorState> targetStates = new ArrayList<RaptorState>();

    Set<RaptorStop> visitedEver = new HashSet<RaptorStop>();
    
    Set<RaptorStop> visitedLastRound = Collections.emptySet();
    HashMap<RaptorStop, StopNearTarget> stopsNearTarget = new HashMap<RaptorStop, StopNearTarget>();
    public List<RaptorState> boundingStates = new ArrayList<RaptorState>();
    public List<State> dijkstraBoundingStates;
    public ShortestPathTree spt;
    
    @SuppressWarnings("unchecked")
    RaptorPathSet(int nStops) {
        statesByStop = new List[nStops];
    }
    
    public List<RaptorState>[] getStates() {
        return statesByStop;
    }

    public void addStates(int stop, List<RaptorState> list) {
        assert(statesByStop[stop] == null);
        statesByStop[stop] = list; 
    }

    public void setStates(int stop, List<RaptorState> list) {
        statesByStop[stop] = list; 
    }
    
    public int getNStops() {
        return statesByStop.length;
    }

    public void addTargetState(RaptorState state) {
        targetStates.add(state);
    }

    public List<RaptorState> getTargetStates() {
        return targetStates;
    }

    public void addStopNearTarget(RaptorStop stop, double walkDistance, int time) {
        stopsNearTarget.put(stop, new StopNearTarget(stop, walkDistance, time));
    }

}

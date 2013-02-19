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

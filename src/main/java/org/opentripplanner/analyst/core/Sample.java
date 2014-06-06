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

package org.opentripplanner.analyst.core;

import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class Sample {

    public final int t0, t1; // TODO change from times to distances.
    public final Vertex v0, v1;
    
    public Sample (Vertex v0, int t0, Vertex v1, int t1) {
        this.v0 = v0;
        this.t0 = t0;
        this.v1 = v1;
        this.t1 = t1;
    }

    public byte evalBoardings(ShortestPathTree spt) {
        State s0 = spt.getState(v0);
        State s1 = spt.getState(v1);
        int m0 = 255;
        int m1 = 255;
        if (s0 != null)
            m0 = (s0.getNumBoardings()); 
        if (s1 != null)
            m1 = (s1.getNumBoardings()); 
        return (byte) ((m0 < m1) ? m0 : m1); 
    }
    
    public long eval(ShortestPathTree spt) {
        State s0 = spt.getState(v0);
        State s1 = spt.getState(v1);
        long m0 = Long.MAX_VALUE;
        long m1 = Long.MAX_VALUE;
        if (s0 != null)
            m0 = (s0.getActiveTime() + t0); 
        if (s1 != null)
            m1 = (s1.getActiveTime() + t1); 
        return (m0 < m1) ? m0 : m1; 
    }

    public String toString() {
        return String.format("Sample: %s in %d sec or %s in %d sec\n", v0, t0, v1, t1);
    }
    
}


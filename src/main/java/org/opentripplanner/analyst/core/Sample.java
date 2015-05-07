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

    public final int d0, d1; // TODO change from times to distances.
    public final Vertex v0, v1;
    
    public Sample (Vertex v0, int d0, Vertex v1, int d1) {
        this.v0 = v0;
        this.d0 = d0;
        this.v1 = v1;
        this.d1 = d1;
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

    /**
     * @param spt the ShortestPathTree with respect to which this sample will be evaluated
     * @return the travel time to reach this Sample point from the SPT's origin
     */
    public long eval(ShortestPathTree spt) {
        State s0 = spt.getState(v0);
        State s1 = spt.getState(v1);
        long m0 = Long.MAX_VALUE;
        long m1 = Long.MAX_VALUE;
        double walkSpeed = spt.getOptions().walkSpeed;
        if (s0 != null)
            m0 = (int)(s0.getActiveTime() + d0 / walkSpeed);
        if (s1 != null)
            m1 = (int)(s1.getActiveTime() + d1 / walkSpeed);
        return (m0 < m1) ? m0 : m1; 
    }

    public double evalWalkDistance(ShortestPathTree spt) {
        State s0 = spt.getState(v0);
        State s1 = spt.getState(v1);
        double m0 = Double.NaN;
        double m1 = Double.NaN;
        if (s0 != null)
            m0 = (s0.getWalkDistance() + d0);
        if (s1 != null)
            m1 = (s1.getWalkDistance() + d1);
        return (m0 < m1) ? m0 : m1;
    }

    /* DUPLICATES code in sampleSet.eval(). should be deduplicated using a common function of vertices/dists. */
    public long eval(TimeSurface surf) {
        int m0 = Integer.MAX_VALUE;
        int m1 = Integer.MAX_VALUE;
        if (v0 != null) {
            int s0 = surf.getTime(v0);
            if (s0 != TimeSurface.UNREACHABLE) {
                m0 = (int) (s0 + d0 / surf.walkSpeed);
            }
        }
        if (v1 != null) {
            int s1 = surf.getTime(v1);
            if (s1 != TimeSurface.UNREACHABLE) {
                m1 = (int) (s1 + d1 / surf.walkSpeed);
            }
        }
        return (m0 < m1) ? m0 : m1;
    }

    public String toString() {
        return String.format("Sample: %s at %d meters or %s at %d meters\n", v0, d0, v1, d1);
    }
    
}


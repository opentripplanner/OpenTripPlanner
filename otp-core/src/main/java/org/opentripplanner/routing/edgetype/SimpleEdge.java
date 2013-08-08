/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;

/** Appears to be used only in tests. */
public class SimpleEdge extends FreeEdge {
    private static final long serialVersionUID = 1L;
    private double weight;
    private int seconds;

    public SimpleEdge (Vertex v1, Vertex v2, double weight, int seconds) {
        super(v1, v2);
        this.weight = weight;
        this.seconds = seconds;
    }
    
    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(seconds);
        s1.incrementWeight(weight);
        // SimpleEdges don't concern themselves with mode
        return s1.makeState();
    }

}
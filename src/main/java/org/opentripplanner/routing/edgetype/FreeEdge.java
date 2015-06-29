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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * An edge that costs nothing to traverse. Used for connecting intersection vertices to the main
 * edge-based graph.
 * 
 * @author novalis
 * 
 */
public class FreeEdge extends Edge {

    private static final long serialVersionUID = 3925814840369402222L;

    public FreeEdge(Vertex from, Vertex to) {
        super(from, to);
    }

    @Override
    public State traverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        // do not change mode, which means it may be null at the start of a trip
        return s1.makeState();
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return this.getName();
    }

    public String toString() {
        return "FreeEdge(" + fromv + " -> " + tov + ")";
    }
}

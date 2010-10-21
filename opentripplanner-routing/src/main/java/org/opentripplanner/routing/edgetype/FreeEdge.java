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

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge that costs nothing to traverse.  Used for connecting intersection vertices to the main
 * edge-based graph.
 * @author novalis
 *
 */
public class FreeEdge extends AbstractEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    public FreeEdge(Vertex from, Vertex to) {
        super(from, to);
    }
    
    @Override
    public TraverseResult traverse(State s0, TraverseOptions options)
            throws NegativeWeightException {
        State s1 = s0.clone();
        return new TraverseResult(0.000001, s1);
    }

    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions options)
            throws NegativeWeightException {
        State s1 = s0.clone();
        return new TraverseResult(0.000001, s1);
    }

    @Override
    public String getDirection() {
        return null;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Geometry getGeometry() {
        return null;
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return null;
    }
    
    public boolean equals(Object o) {
        if (o instanceof FreeEdge) {
            FreeEdge other = (FreeEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }
}

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

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

/**
 * A superclass for general trip pattern related edges
 * @author novalis
 *
 */
public abstract class TablePatternEdge extends Edge implements PatternEdge {

    private static final long serialVersionUID = 1L;

    public TablePatternEdge(TransitVertex fromv, TransitVertex tov) {
        super(fromv, tov);
    }

    public TripPattern getPattern() {
        return ((OnboardVertex)fromv).getTripPattern();
    }

    public abstract int getStopIndex();

}

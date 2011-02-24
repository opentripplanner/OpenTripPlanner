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

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.Vertex;

/**
 * A superclass for general trip pattern related edges
 * @author novalis
 *
 */
public abstract class PatternEdge extends AbstractEdge {

    protected TripPattern pattern;

    private static final long serialVersionUID = 1L;

    public PatternEdge(Vertex fromv, Vertex tov, TripPattern pattern) {
        super(fromv, tov);
        this.pattern = pattern;
    }

    public TripPattern getPattern() {
        return pattern;
    }

    public void setPattern(TripPattern pattern) {
        this.pattern = pattern;
    }
}

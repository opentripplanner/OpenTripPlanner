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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Represents an ordinary location in space, typically an intersection.
 */
public class IntersectionVertex extends StreetVertex {

    private static final long serialVersionUID = 1L;

    /**
     * Does this intersection have a traffic light?
     */
    public boolean trafficLight;

    /**
     * Is this a free-flowing intersection, i.e. should it have no delay at all? e.g., freeway ramps, &c.
     */
    public boolean freeFlowing;

    /** Returns true if this.freeFlowing or if it appears that this vertex is free-flowing */
    public boolean inferredFreeFlowing() {
        if (this.freeFlowing) {
            return true;
        }
        
        return getDegreeIn() == 1 && getDegreeOut() == 1 && !this.trafficLight;
    }

    //For testing only
    public IntersectionVertex(Graph g, String label, double x, double y, String name) {
        this(g, label, x, y, new NonLocalizedString(name));
    }

    public IntersectionVertex(Graph g, String label, double x, double y, I18NString name) {
        super(g, label, x, y, name);
        freeFlowing = false;
        trafficLight = false;
    }

    public IntersectionVertex(Graph g, String label, double x, double y) {
        this(g, label, x, y, new NonLocalizedString(label));
    }

}

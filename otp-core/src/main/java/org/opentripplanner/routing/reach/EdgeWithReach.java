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

package org.opentripplanner.routing.reach;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public abstract class EdgeWithReach extends Edge {
    public EdgeWithReach(Vertex v1, Vertex v2) {
        super(v1, v2);
    }
    
    private static final long serialVersionUID = 2515505277419161021L;
    
    public abstract void setReach(double reach);
    //public void setTransitReach(double reach);

    public abstract double getReach();
    //public double getTransitReach();
}

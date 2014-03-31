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

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;

public abstract class OnboardVertex extends TransitVertex {

    private static final long serialVersionUID = 1L;

    private final TripPattern tripPattern; // set to null for non-pattern vertices
    // (or just use patterns for everything, eliminating simple hops)
    
    public OnboardVertex(Graph g, String label, TripPattern tripPattern, Stop stop) {
        super(g, label, stop);
        this.tripPattern = tripPattern;
    }

    public TripPattern getTripPattern() {
        return tripPattern;
    }
    
}

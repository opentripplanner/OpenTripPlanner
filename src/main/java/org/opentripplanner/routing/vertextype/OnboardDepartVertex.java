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

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A vertex acting as a starting point for planning a trip while onboard an existing trip.
 * 
 * @author laurent
 */
public class OnboardDepartVertex extends Vertex {

    private static final long serialVersionUID = -6721280275560962711L;

    public OnboardDepartVertex(String label, double lon, double lat) {
        // This vertex is *alway* temporary, so graph is always null.
        super(null, label, lon, lat, label);
    }

    @Override
    public int removeTemporaryEdges(Graph graph) {
        // We can remove all
        int nRemoved = 0;
        for (Edge e : getOutgoing()) {
            if (e.detach(graph) != 0)
                nRemoved += 1;
        }
        if (!getIncoming().isEmpty())
            throw new AssertionError("Can't have incoming edge on a OnboardDepartVertex");
        return nRemoved;
    }
}

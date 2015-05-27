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

import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex acting as a starting point for planning a trip while onboard an existing trip.
 * 
 * @author laurent
 */
public class OnboardDepartVertex extends Vertex implements TemporaryVertex {
    private static final long serialVersionUID = -6721280275560962711L;

    public OnboardDepartVertex(String label, double lon, double lat) {
        super(null, label, lon, lat, new NonLocalizedString(label));
    }

    @Override
    public void addIncoming(Edge edge) {
        throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof TemporaryEdge) {
            super.addOutgoing(edge);
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }

    @Override
    public void dispose() {
        for (Object temp : getOutgoing()) {
            ((TemporaryEdge) temp).dispose();
        }
    }
}

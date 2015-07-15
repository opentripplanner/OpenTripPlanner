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

package org.opentripplanner.routing.location;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.I18NString;

final public class TemporaryStreetLocation extends StreetLocation implements TemporaryVertex {
    final private boolean endVertex;

    public TemporaryStreetLocation(String id, Coordinate nearestPoint, I18NString name,
                                   boolean endVertex) {
        super(id, nearestPoint, name);
        this.endVertex = endVertex;
    }

    @Override
    public void addIncoming(Edge edge) {
        if (edge instanceof TemporaryEdge) {
            if (endVertex) {
                super.addIncoming(edge);
            } else {
                throw new UnsupportedOperationException("Can't add incoming edge to start vertex");
            }
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public void addOutgoing(Edge edge) {
        if (edge instanceof TemporaryEdge) {
            if (endVertex) {
                throw new UnsupportedOperationException("Can't add outgoing edge to end vertex");
            } else {
                super.addOutgoing(edge);
            }
        } else {
            throw new UnsupportedOperationException("Can't add permanent edge to temporary vertex");
        }
    }

    @Override
    public boolean isEndVertex() {
        return endVertex;
    }

    @Override
    public void dispose() {
        for (Object temp : endVertex ? getIncoming() : getOutgoing()) {
            ((TemporaryEdge) temp).dispose();
        }
    }
}

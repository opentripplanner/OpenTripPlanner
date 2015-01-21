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

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge {
    final private boolean endEdge;

    public TemporaryFreeEdge(TemporaryVertex from, Vertex to) {
        super((Vertex) from, to);

        if (from.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else {
            endEdge = false;
        }
    }

    public TemporaryFreeEdge(Vertex from, TemporaryVertex to) {
        super(from, (Vertex) to);

        if (to.isEndVertex()) {
            endEdge = true;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public void dispose() {
        if (endEdge) {
            fromv.removeOutgoing(this);
        } else {
            tov.removeIncoming(this);
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}

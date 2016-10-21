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

import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.util.I18NString;

final public class TemporaryPartialStreetEdge extends PartialStreetEdge implements TemporaryEdge {
    final private Boolean endEdge;  // A null value means that the vertices are temporary themselves

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporaryStreetLocation v1,
            TemporaryStreetLocation v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else if (v2.isEndVertex()) {
            endEdge = null;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporaryStreetLocation v1,
            StreetVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else {
            endEdge = false;
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, TemporarySplitterVertex v1,
        StreetVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else {
            endEdge = false;
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1,
            TemporaryStreetLocation v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v2.isEndVertex()) {
            endEdge = true;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    public TemporaryPartialStreetEdge(StreetEdge parentEdge, StreetVertex v1,
        TemporarySplitterVertex v2, LineString geometry, I18NString name, double length) {
        super(parentEdge, v1, v2, geometry, name, length);

        if (v2.isEndVertex()) {
            endEdge = true;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public void dispose() {
        if (endEdge != null) {
            if (endEdge) {
                fromv.removeOutgoing(this);
            } else {
                tov.removeIncoming(this);
            }
        }
    }

    @Override
    public String toString() {
        return "Temporary" + super.toString();
    }
}

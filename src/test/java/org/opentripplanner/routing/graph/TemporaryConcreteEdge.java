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

package org.opentripplanner.routing.graph;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryConcreteEdge extends Edge implements TemporaryEdge {
    final private boolean endEdge;

    public TemporaryConcreteEdge(TemporaryVertex v1, Vertex v2) {
        super((Vertex) v1, v2);

        if (v1.isEndVertex()) {
            throw new IllegalStateException("A temporary edge is directed away from an end vertex");
        } else {
            endEdge = false;
        }
    }

    public TemporaryConcreteEdge(Vertex v1, TemporaryVertex v2) {
        super(v1, (Vertex) v2);

        if (v2.isEndVertex()) {
            endEdge = true;
        } else {
            throw new IllegalStateException("A temporary edge is directed towards a start vertex");
        }
    }

    @Override
    public State traverse(State s0) {
        double d = getDistance();
        TraverseMode mode = s0.getNonTransitMode();
        int t = (int) (d / s0.getOptions().getSpeed(mode));
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(t);
        s1.incrementWeight(d);
        return s1.makeState();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getName(Locale locale) {
        return null;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public double getDistance() {
        return SphericalDistanceLibrary.distance(getFromVertex().getCoordinate(), getToVertex().getCoordinate());
    }

    @Override
    public void dispose() {
        if (endEdge) {
            fromv.removeOutgoing(this);
        } else {
            tov.removeIncoming(this);
        }
    }
}

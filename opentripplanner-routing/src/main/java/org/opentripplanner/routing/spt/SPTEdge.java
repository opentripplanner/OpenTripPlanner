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

package org.opentripplanner.routing.spt;

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class SPTEdge implements Edge {

    private static final long serialVersionUID = 1L;

    public SPTVertex fromv;

    public SPTVertex tov;

    public Edge payload;

    SPTEdge(SPTVertex fromv, SPTVertex tov, Edge ep) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = ep;
    }

    @Override
    public String getDirection() {
        return payload.getDirection();
    }

    @Override
    public double getDistance() {
        return payload.getDistance();
    }

    @Override
    public String getEnd() {
        return payload.getEnd();
    }

    @Override
    public Geometry getGeometry() {
        return payload.getGeometry();
    }

    @Override
    public TraverseMode getMode() {
        return payload.getMode();
    }

    @Override
    public String getName() {
        return payload.getName();
    }

    @Override
    public String getStart() {
        return payload.getStart();
    }

    @Override
    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverse(s0, wo);
    }

    @Override
    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverseBack(s0, wo);
    }

    public SPTVertex getToSPTVertex() {
        return tov;
    }

    public SPTVertex getFromSPTVertex() {
        return fromv;
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }
}
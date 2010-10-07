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

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternEdge;
import org.opentripplanner.routing.edgetype.TripPattern;

import com.vividsolutions.jts.geom.Geometry;

/** An edge in the ShortestPathTree */
public class SPTEdge {

    private static final long serialVersionUID = 1L;

    public SPTVertex fromv;

    public SPTVertex tov;

    public Edge payload;

    public SPTEdge(SPTVertex fromv, SPTVertex tov, Edge ep) {
        this.fromv = fromv;
        this.tov = tov;
        this.payload = ep;
    }

    public String getDirection() {
        return payload.getDirection();
    }

    public double getDistance() {
        return payload.getDistance();
    }

    public Geometry getGeometry() {
        return payload.getGeometry();
    }

    public TraverseMode getMode() {
        return payload.getMode();
    }

    public String getName() {
        if (payload instanceof PatternBoard) {
            return payload.getName(tov.state);
        } else {
            return payload.getName(fromv.state);
        }
    }

    public Trip getTrip() {

        if (payload instanceof Board || payload instanceof Hop || payload instanceof Alight) {
            return payload.getTrip();
        }
        int patternIndex = -1;
        if (payload instanceof PatternAlight) {
            patternIndex = fromv.state.getTrip();
        } else if (payload instanceof PatternEdge) {
            patternIndex = tov.state.getTrip();
        } else {
            return null;
        }

        TripPattern pattern = ((PatternEdge)payload).getPattern();
        return pattern.getTrip(patternIndex);
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverse(s0, wo);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) throws NegativeWeightException {
        return payload.traverseBack(s0, wo);
    }

    public SPTVertex getFromVertex() {
        return fromv;
    }

    public SPTVertex getToVertex() {
        return tov;
    }
    
    public String toString() {
        return "SPTEdge(" + payload.toString() + ")";
    }

    public String getName(State state) {
        return payload.getName(state);
    }
    
    public boolean equals(Object o) {
        if (o instanceof SPTEdge) {
            SPTEdge e = (SPTEdge) o;
            return e.payload.equals(payload);
        }
        return false;
    }
    
    public int hashCode() {
        return 37 * payload.hashCode();
    }
}
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

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class Transfer extends AbstractEdge {

    private static final long serialVersionUID = 1L;
    
    double distance = 0;

    private Geometry geometry = null;

    public Transfer(Vertex fromv, Vertex tov, double distance) {
        super(fromv, tov);
        this.distance = distance;
    }

    public String getDirection() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public String getEnd() {
        // TODO Auto-generated method stub
        return null;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        return TraverseMode.TRANSFER;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "transfer";
    }

    public String getStart() {
        // TODO Auto-generated method stub
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        if (!s0.getTransferAllowed()) {
            return null;
        }
        State s1 = s0.clone();
        s1.incrementTimeInSeconds((int)(distance / wo.speed));
        return new TraverseResult(distance / wo.speed, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (!s0.getTransferAllowed()) {
            return null;
        }
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(-(int)(distance / wo.speed));
        return new TraverseResult(distance / wo.speed, s1);
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Transfer)) {
            return false;
        }
        Transfer t = (Transfer) o;
        return t.getToVertex().equals(getToVertex()) && t.getFromVertex().equals(getFromVertex());
    }
    
    public int hashCode() {
        return getToVertex().hashCode() ^ getFromVertex().hashCode();
    }

    public void setGeometry(Geometry geometry) {
        this.geometry  = geometry;
    }
}

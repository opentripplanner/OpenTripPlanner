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

import java.util.Vector;

import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;

public class SPTVertex extends GenericVertex {
    
    private static final long serialVersionUID = -4422788581123655293L;

    public SPTEdge incoming;

    public Vector<SPTEdge> outgoing;

    public Vertex mirror;

    public State state;
    
    public TraverseOptions options;

    public double weightSum;

    SPTVertex(Vertex mirror, State state, double weightSum, TraverseOptions options) {
        super(mirror.getLabel(), mirror.getX(), mirror.getY());
        this.mirror = mirror;
        this.state = state;
        this.weightSum = weightSum;
        this.options = options;
        this.outgoing = new Vector<SPTEdge>();
    }

    public void addOutgoing(SPTEdge ee) {
        this.outgoing.add(ee);
    }

    public SPTEdge setParent(SPTVertex parent, Edge ep) {
        // remove this edge from outgoing list of previous parent
        if (incoming != null) {
            incoming.fromv.outgoing.remove(incoming);
        }
        incoming = new SPTEdge(parent, this, ep);
        parent.outgoing.add(incoming);
        return incoming;
    }

    public String toString() {
        return this.mirror.getLabel() + " (" + this.weightSum + ")";
    }

    public String getName() {
        return this.mirror.getName();
    }

    public String getStopId() {
        return this.mirror.getStopId();
    }

    public boolean equals(SPTVertex v) {
        return v.mirror == mirror && v.incoming == incoming;
    }
}
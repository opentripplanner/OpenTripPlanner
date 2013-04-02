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

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.routing.core.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an ordinary location in space, typically an intersection.
 * 
 * TODO(flamholz): the various constants in this class should be factored out and configurable. Likely calls for another class. TurnCostModel or
 * something of the like.
 */
public class IntersectionVertex extends StreetVertex {

    private static final Logger LOG = LoggerFactory.getLogger(IntersectionVertex.class);

    private static final long serialVersionUID = 1L;

    /**
     * Does this intersection have a traffic light?
     */
    @Getter
    @Setter
    private boolean trafficLight;

    /**
     * Is this a free-flowing intersection, i.e. should it have no delay at all? e.g., freeway ramps, &c.
     */
    @Getter
    @Setter
    private boolean freeFlowing;
    
    /**
     * The model that computes turn/traversal costs.
     * 
     * TODO(flamholz): this is a weird place to inject the model. We do it here for historical reasons - this is where costs have usually been
     * computed. Consider doing it higher up, like inside the search code?
     */
    private static IntersectionTraversalCostModel costModel = new SimpleIntersectionTraversalCostModel();

    /** Computes the cost of traversing this intersection in seconds */
    public double computeTraversalCost(PlainStreetEdge from, PlainStreetEdge to, TraverseMode mode,
            RoutingRequest options, float fromSpeed, float toSpeed) {
        // TODO(flamholz): move this free-flowing check into the cost model?
        if (freeFlowing) {
            return 0;
        }

        if (inferredFreeFlowing()) {
            LOG.debug("Inferred that IntersectionVertex {} is free-flowing", getIndex());
            return 0;
        }
        
        return costModel.computeTraversalCost(this, from, to, mode, options, fromSpeed, toSpeed);
    }

    protected boolean inferredFreeFlowing() {
        return getDegreeIn() == 1 && getDegreeOut() == 1 && !this.trafficLight;
    }

    public IntersectionVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
        freeFlowing = false;
        trafficLight = false;
    }

    public IntersectionVertex(Graph g, String label, double x, double y) {
        this(g, label, x, y, label);
    }

}

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/* This represents the connection between a street vertex and a transit vertex
 * where going from the street to the vehicle is immediate -- such as at a 
 * curbside bus stop.
 */
public class StreetTransitLink extends AbstractEdge implements WalkableEdge {

    private static GeometryFactory _geometryFactory = new GeometryFactory();
    private boolean wheelchairAccessible;
        
    public StreetTransitLink(Vertex fromv, Vertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        this.wheelchairAccessible = wheelchairAccessible;
    }

    private static final long serialVersionUID = -3311099256178798981L;
    private static final double STL_TRAVERSE_COST = 1;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(), getToVertex().getCoordinate()};
        return _geometryFactory.createLineString(coordinates);
    }

    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "street transit link";
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        if (s0.justTransferred) {
            return null;
        }
        if (wo.wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(1);
        //technically, we only need to do this when we're going
        //off the street onto transit, but it won't hurt 
        //to do it unconditionally.
        s1.justTransferred = true;
        return new TraverseResult(STL_TRAVERSE_COST, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (s0.justTransferred) {
            return null;
        }
        if (wo.wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        State s1 = s0.clone();
        s1.incrementTimeInSeconds(-1);
        s1.justTransferred = true;
        return new TraverseResult(STL_TRAVERSE_COST, s1);
    }

}

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

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/** 
 * This represents the connection between a street vertex and a transit vertex
 * where going from the street to the vehicle is immediate -- such as at a 
 * curbside bus stop.
 */
public class StreetTransitLink extends AbstractEdge {

    private static final long serialVersionUID = -3311099256178798981L;
    private static final double STL_TRAVERSE_COST = 1;

    private static GeometryFactory _geometryFactory = new GeometryFactory();
    private boolean wheelchairAccessible;

    private TransitStop transitStop;

    public StreetTransitLink(StreetVertex fromv, TransitStop tov, boolean wheelchairAccessible) {
    	super(fromv, tov);
    	transitStop = tov;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public StreetTransitLink(TransitStop fromv, StreetVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        transitStop = fromv;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate()};
        return _geometryFactory.createLineString(coordinates);
    }

    public TraverseMode getMode() {
        return TraverseMode.STL;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "street transit link";
    }

    public State traverse(State s0) {
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        // disallow traversing two StreetTransitLinks in a row.
        // prevents router using transit stops as shortcuts to get around turn restrictions.
        if (s0.getBackEdge() instanceof StreetTransitLink)
            return null;
        // Do not check here whether transit modes are selected. A check for the presence of 
        // transit modes will instead be done in the following PreBoard edge.
        // This allows finding transit stops with walk-only options.
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(transitStop.getStreetToStopTime() + 1);
        s1.incrementWeight(STL_TRAVERSE_COST + transitStop.getStreetToStopTime());
        return s1.makeState();
    }

    public State optimisticTraverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(STL_TRAVERSE_COST);
        return s1.makeState();
    }
    
    // anecdotally, the lower bound search is about 2x faster when you don't reach stops
    // and therefore don't even consider boarding
    @Override
    public double weightLowerBound(TraverseOptions options) {
        return options.transitAllowed() ? 0 : Double.POSITIVE_INFINITY;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public Trip getTrip() {
        return null;
    }

    public boolean isRoundabout() {
        return false;
    }

    public String toString() {
        return "StreetTransitLink(" + fromv + " -> " + tov + ")";
    }


}

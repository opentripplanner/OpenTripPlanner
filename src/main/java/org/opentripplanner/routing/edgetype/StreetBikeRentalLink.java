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

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * This represents the connection between a street vertex and a bike rental station vertex.
 * 
 */
public class StreetBikeRentalLink extends Edge {

    private static final long serialVersionUID = 1L;

    private BikeRentalStationVertex bikeRentalStationVertex;

    public StreetBikeRentalLink(StreetVertex fromv, BikeRentalStationVertex tov) {
        super(fromv, tov);
        bikeRentalStationVertex = tov;
    }

    public StreetBikeRentalLink(BikeRentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        bikeRentalStationVertex = fromv;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        return null;
    }

    public String getName() {
        return bikeRentalStationVertex.getName();
    }

    @Override
    public String getName(Locale locale) {
        return bikeRentalStationVertex.getName(locale);
    }

    public State traverse(State s0) {
        // Do not even consider bike rental vertices unless bike rental is enabled.
        if ( ! s0.getOptions().allowBikeRental) {
            return null;
        }
        // Disallow traversing two StreetBikeRentalLinks in a row.
        // This prevents the router from using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetBikeRentalLink) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        //assume bike rental stations are more-or-less on-street
        s1.incrementTimeInSeconds(1);
        s1.incrementWeight(1);
        s1.setBackMode(s0.getNonTransitMode());
        return s1.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return options.modes.contains(TraverseMode.BICYCLE) ? 0 : Double.POSITIVE_INFINITY;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public String toString() {
        return "StreetBikeRentalLink(" + fromv + " -> " + tov + ")";
    }
}

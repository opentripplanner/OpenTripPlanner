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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.Locale;

/**
 * This represents the connection between a street vertex and a vehicle rental station vertex.
 * 
 */
public class StreetVehicleRentalLink extends Edge {

    private static final long serialVersionUID = 1L;

    private VehicleRentalStationVertex vehicleRentalStationVertex;

    public StreetVehicleRentalLink(StreetVertex fromv, VehicleRentalStationVertex tov) {
        super(fromv, tov);
        vehicleRentalStationVertex = tov;
    }

    public StreetVehicleRentalLink(VehicleRentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        vehicleRentalStationVertex = fromv;
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
        return vehicleRentalStationVertex.getName();
    }

    @Override
    public String getName(Locale locale) {
        return vehicleRentalStationVertex.getName(locale);
    }

    public State traverse(State s0) {
        // Do not even consider vehicle rental vertices unless vehicle rental is enabled.
        if (!s0.getOptions().allowVehicleRental) {
            return null;
        }
        // Disallow traversing two StreetVehicleRentalLinks in a row.
        // This prevents the router from using vehicle rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetVehicleRentalLink) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.setBackMode(s0.getNonTransitMode());
        return s1.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return options.modes.contains(TraverseMode.MICROMOBILITY) ? 0 : Double.POSITIVE_INFINITY;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public String toString() {
        return "StreetVehicleRentalLink(" + fromv + " -> " + tov + ")";
    }
}

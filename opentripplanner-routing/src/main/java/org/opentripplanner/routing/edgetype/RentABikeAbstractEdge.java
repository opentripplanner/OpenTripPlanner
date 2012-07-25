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

import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Renting or dropping off a rented bike edge.
 * 
 * @author laurent
 * 
 */
public abstract class RentABikeAbstractEdge extends AbstractEdge {

	private static final long serialVersionUID = 1L;

	public RentABikeAbstractEdge(Vertex from, Vertex to) {
		super(from, to);
	}

	protected State traverseRent(State s0) {
            RoutingRequest options = s0.getOptions();
            /*
             * If we already have a bike (rented or own) we won't go any faster by having a second
             * one.
             */
            if (!s0.getNonTransitMode(options).equals(TraverseMode.WALK))
                return null;
            /*
             * To rent a bike, we need to have BICYCLE in allowed modes.
             */
            if (!options.getModes().contains(TraverseMode.BICYCLE))
                return null;
            
            BikeRentalStationVertex dropoff = (BikeRentalStationVertex) tov;
            if (options.isUseBikeRentalAvailabilityInformation() && dropoff.getBikesAvailable() == 0) {
                return null;
            }
            EdgeNarrative en = new FixedModeEdge(this, s0.getNonTransitMode(options));

            StateEditor s1 = s0.edit(this, en);
            s1.incrementWeight(options.isArriveBy() ? options.bikeRentalDropoffCost : options.bikeRentalPickupCost);
            s1.incrementTimeInSeconds(options.isArriveBy() ? options.bikeRentalDropoffTime : options.bikeRentalPickupTime);
            s1.setBikeRenting(true);
            State s1b = s1.makeState();
            return s1b;
	}

	protected State traverseDropoff(State s0) {
            RoutingRequest options = s0.getOptions();
            /*
             * To dropoff a bike, we need to have rented one.
             */
            if (!s0.isBikeRenting())
                return null;
            BikeRentalStationVertex pickup = (BikeRentalStationVertex) tov;
            if (options.isUseBikeRentalAvailabilityInformation() && pickup.getSpacesAvailable() == 0) {
                return null;
            }
            EdgeNarrative en = new FixedModeEdge(this, s0.getNonTransitMode(options));

            StateEditor s1e = s0.edit(this, en);
            s1e.incrementWeight(options.isArriveBy() ? options.bikeRentalPickupCost : options.bikeRentalDropoffCost);
            s1e.incrementTimeInSeconds(options.isArriveBy() ? options.bikeRentalPickupTime : options.bikeRentalDropoffTime);
            s1e.setBikeRenting(false);
            State s1 = s1e.makeState();
            return s1;
	}

	@Override
	public double getDistance() {
		return 0;
	}

	@Override
	public Geometry getGeometry() {
		return null;
	}

	@Override
	public TraverseMode getMode() {
		return TraverseMode.WALK;
	}

	@Override
	public String getName() {
		return getToVertex().getName();
	}

	@Override
	public boolean hasBogusName() {
		return false;
	}
}

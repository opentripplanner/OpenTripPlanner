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
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.EdgeNarrative;

public class TransitNarrative extends DelegatingEdgeNarrative {

	private Trip trip;
	private String headsign;

	public TransitNarrative(Trip trip, EdgeNarrative edge) {
		super(edge);
		this.trip = trip;
	}

	public TransitNarrative(Trip trip, String headsign, EdgeNarrative edge) {
		super(edge);
		this.trip = trip;
		this.headsign = headsign;
	}
	
    public String getDirection() {
        return getTrip().getTripHeadsign();
    }

   
	@Override
	public String getName() {
		return GtfsLibrary.getRouteName(trip.getRoute());
	}

	@Override
	public Trip getTrip() {
		if (headsign == null) {
			return trip;
		} else {
			Trip newtrip = new Trip();
			newtrip.setTripHeadsign(headsign); /* This is the only difference */

			newtrip.setBlockId(trip.getBlockId());
			newtrip.setDirectionId(trip.getDirectionId());
			newtrip.setDirectionId(trip.getDirectionId());
			newtrip.setId(trip.getId());
			newtrip.setRoute(trip.getRoute());
			newtrip.setRouteShortName(trip.getRouteShortName());
			newtrip.setServiceId(trip.getServiceId());
			newtrip.setShapeId(trip.getServiceId());
			newtrip.setTripBikesAllowed(trip.getTripBikesAllowed());
			newtrip.setTripShortName(trip.getTripShortName());
			newtrip.setWheelchairAccessible(trip.getWheelchairAccessible());

			return newtrip;
		}
	}

	public String toString() {
		return "TransitNarrative(" + base + ")";
	}
}

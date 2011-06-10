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

package org.opentripplanner.routing.transit_index;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.services.TransitIndexService;

public class TransitIndexServiceImpl implements TransitIndexService,
		Serializable {
	private static final long serialVersionUID = -8147894489513820239L;

	private HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute;
	private HashMap<AgencyAndId, RouteVariant> variantsByTrip;

	private HashMap<AgencyAndId, Edge> preAlightEdges;

	private HashMap<AgencyAndId, Edge> preBoardEdges;

	private HashMap<AgencyAndId, List<String>> directionsForRoute;

	public TransitIndexServiceImpl(
			HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute,
			HashMap<AgencyAndId, RouteVariant> variantsByTrip,
			HashMap<AgencyAndId, Edge> preBoardEdges,
			HashMap<AgencyAndId, Edge> preAlightEdges,
			HashMap<AgencyAndId, List<String>> directionsForRoute) {
		this.variantsByRoute = variantsByRoute;
		this.variantsByTrip = variantsByTrip;
		this.preBoardEdges = preBoardEdges;
		this.preAlightEdges = preAlightEdges;
		this.directionsForRoute = directionsForRoute;
	}

	public List<RouteVariant> getVariantsForRoute(AgencyAndId route) {
		return variantsByRoute.get(route);
	}

	public RouteVariant getVariantForTrip(AgencyAndId trip) {
		return variantsByTrip.get(trip);
	}

	@Override
	public Edge getPrealightEdge(AgencyAndId stop) {
		return preAlightEdges.get(stop);
	}

	@Override
	public Edge getPreboardEdge(AgencyAndId stop) {
		return preBoardEdges.get(stop);
	}

	@Override
	public List<String> getDirectionsForRoute(AgencyAndId route) {
		return directionsForRoute.get(route);
	}

}

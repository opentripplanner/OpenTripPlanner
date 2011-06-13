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
package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.model.transit.RouteData;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

// NOTE - /ws/transit is the full path -- see web.xml

@Path("/transit")
@XmlRootElement
@Autowire
public class TransitIndex {

	private static final Logger LOGGER = Logger.getLogger(TransitIndex.class.getCanonicalName());

	private GraphService graphService;

	@Autowired
	public void setGraphService(GraphService graphService) {
		this.graphService = graphService;
	}

	/**
	 * Return data about a route, such as its variants and directions,
	 * that OneBusAway's API doesn't handle
	 */
	@GET
	@Path("/routeData")
	@Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_XML })
	public RouteData getRouteData(
			@QueryParam("agency") String agency,
			@QueryParam("id") String id) throws JSONException {
		
		TransitIndexService transitIndexService = graphService.getGraph().getService(TransitIndexService.class);
		RouteData response = new RouteData();
		AgencyAndId routeId = new AgencyAndId(agency, id);
		response.id = routeId;
		List<RouteVariant> variants = transitIndexService.getVariantsForRoute(routeId);
		
		response.variants = variants;	
		response.directions = new ArrayList<String>(transitIndexService.getDirectionsForRoute(routeId));
		
		return response;
	}

}

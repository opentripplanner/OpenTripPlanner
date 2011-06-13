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

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.model.patch.PatchCreationResponse;
import org.opentripplanner.api.model.patch.PatchResponse;
import org.opentripplanner.api.model.patch.PatchSet;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.patch.RouteNotePatch;
import org.opentripplanner.routing.services.PatchService;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

// NOTE - /ws/patch is the full path -- see web.xml

@Path("/patch")
@XmlRootElement
@Autowire
public class Patcher {

	private static final Logger LOGGER = Logger.getLogger(Patcher.class.getCanonicalName());

	private PatchService patchservice;

	@Autowired
	public void setPatchService(PatchService patchService) {
		this.patchservice = patchService;
	}

	/**
	 * Return a list of all patches that apply to a given stop
	 * 
	 * @return Returns either an XML or a JSON document, depending on the HTTP
	 *         Accept header of the client making the request.
	 * 
	 * @throws JSONException
	 */
	@GET
	@Path("/stopPatches")
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
			MediaType.TEXT_XML })
	public PatchResponse getStopPatches(
			@QueryParam("agency") String agency,
			@QueryParam("id") String id) throws JSONException {

		PatchResponse response = new PatchResponse();
		Collection<Patch> patches = patchservice.getStopPatches(new AgencyAndId(agency, id));
		for (Patch patch : patches) {
			response.addPatch(patch);
		}
		return response;
	}

	/**
	 * Return a list of all patches that apply to a given route
	 * 
	 * @return Returns either an XML or a JSON document, depending on the HTTP
	 *         Accept header of the client making the request.
	 * 
	 * @throws JSONException
	 */
	@GET
	@Path("/routePatches")
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
			MediaType.TEXT_XML })
	public PatchResponse getRoutePatches(
			@QueryParam("agency") String agency,
			@QueryParam("id") String id) throws JSONException {

		PatchResponse response = new PatchResponse();
		Collection<Patch> patches = patchservice.getRoutePatches(new AgencyAndId(agency, id));
		for (Patch patch : patches) {
			response.addPatch(patch);
		}
		return response;
	}

	@RolesAllowed("user")
	@POST
	@Path("/patch")
	@Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
			MediaType.TEXT_XML })
	@Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public PatchCreationResponse createPatches(PatchSet patches) throws JSONException {
		PatchCreationResponse response = new PatchCreationResponse();
		for (Patch patch : patches.patches) {
			if (patch.getId() == null) {
				response.status = "Every patch must have an id";
				return response;
			}
			if (patch instanceof RouteNotePatch && ((RouteNotePatch) patch).getRoute().getId().equals("")) {
				response.status = "Every route note patch must have a route id";
				return response;
			}
		}
		for (Patch patch : patches.patches) {
			patchservice.apply(patch);
		}
		response.status = "OK";
		return response;
	}
}

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

package org.opentripplanner.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.opentripplanner.api.parameter.BoundingBox;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Maybe the internal geocoder resource should just chain to defined external geocoders?
 */
@Path("/geocode")
public class ExternalGeocoderResource {
  
// uncommenting injectparam will require a specific Geocoder to be instantiated
//    @InjectParam 
    public Geocoder geocoder;
    
    @GET
    @Produces({MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    public GeocoderResults geocode(
            @QueryParam("address") String address,
            @QueryParam("bbox") BoundingBox bbox) {
        if (address == null) {
            badRequest ("no address");
        }
        Envelope env = (bbox == null) ? null : bbox.envelope();
        return geocoder.geocode(address, env);
    }

    private void badRequest (String message) {
        throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
                .entity(message).type("text/plain").build());
    }
}

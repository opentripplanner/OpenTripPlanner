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

package org.opentripplanner.geocoder.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResults;


@Path("/geocode")
public class GeocoderServer {
    
    private Geocoder geocoder;
    
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public GeocoderResults geocode(@QueryParam("address") String address) {
        if (address == null) {
            throw new WebApplicationException(Response.status(400)
                    .entity("no address")
                    .type("text/plain")
                    .build());
        }
        return geocoder.geocode(address);
    }
}

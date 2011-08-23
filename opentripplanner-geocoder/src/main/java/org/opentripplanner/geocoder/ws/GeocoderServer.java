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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


@Path("/geocode")
public class GeocoderServer {
    
    private Geocoder geocoder;
    
    public void setGeocoder(Geocoder geocoder) {
        this.geocoder = geocoder;
    }

    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public GeocoderResults geocode(
            @QueryParam("address") String address,
            @QueryParam("bbox") String bbox) {
        if (address == null) {
            error("no address");
        }
        if (bbox != null) {
            // left,top,right,bottom
            String[] elem = bbox.split(",");
            if (elem.length == 4) {
                try {
                    double x1 = Double.parseDouble(elem[0]);
                    double y1 = Double.parseDouble(elem[1]);
                    double x2 = Double.parseDouble(elem[2]);
                    double y2 = Double.parseDouble(elem[3]);
                    Envelope envelope = new Envelope(new Coordinate(x1, y1), new Coordinate(x2, y2));
                    return geocoder.geocode(address, envelope);
                } catch (NumberFormatException e) {
                    error("bad bounding box: use left,top,right,bottom");
                }
            } else {
                error("bad bounding box: use left,top,right,bottom");
            }
        }
        return geocoder.geocode(address, null);
    }

    private void error(String message) {
        throw new WebApplicationException(Response.status(400)
                .entity(message)
                .type("text/plain")
                .build());
    }
}

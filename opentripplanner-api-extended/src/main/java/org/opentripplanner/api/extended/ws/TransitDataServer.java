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

package org.opentripplanner.api.extended.ws;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.extended.ws.model.TransitServerDepartures;
import org.opentripplanner.api.extended.ws.model.TransitServerDetailedStop;
import org.opentripplanner.api.extended.ws.model.TransitServerRoute;
import org.opentripplanner.api.extended.ws.model.TransitServerRoutes;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

@Path("/")
@Autowire
public class TransitDataServer {

    private TransitServerGtfs transitServerGtfs;

    @Autowired
    public void setTransitServerGtfs(TransitServerGtfs transitServerGtfs) {
        this.transitServerGtfs = transitServerGtfs;
    }
    
    @GET
    @Path("/")
    @Produces("text/html")
    public String getIndex() {
        return "<html><body><h2>The system works</h2></body></html>";
    }
    
    @GET
    @Path("routes")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerRoutes getRoutes() throws JSONException {
        return new TransitServerRoutes(transitServerGtfs);
    }
    
    @GET
    @Path("departures")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerDepartures getDepartures(@QueryParam("lat") String lat,
                                                 @QueryParam("lon") String lon,
                                                 @DefaultValue("3") @QueryParam("n") int n) throws JSONException {
        String latlon = buildLatLon(lat, lon);
        return new TransitServerDepartures(latlon, n, transitServerGtfs);
    }
    
    @GET
    @Path("routes/{route_id}")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerRoute getRoute(@PathParam("route_id") String routeId) throws JSONException {
        return new TransitServerRoute(transitServerGtfs, routeId);
    }
    
    @GET
    @Path("stop")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerDetailedStop getDetailedStop(@QueryParam("lat") String lat,
                                                     @QueryParam("lon") String lon,
                                                     @DefaultValue("3") @QueryParam("n") int n) throws JSONException {
        String latlon = buildLatLon(lat, lon);
        return new TransitServerDetailedStop(transitServerGtfs, latlon, n);
    }
    
    private String buildLatLon(String lat, String lon) {
        if (lat == null || lon == null) {
            throw new NullPointerException("Got null for a lat/lon value: " + lat + " - " + lon);
        }
        return lat + "," + lon;
    }
}

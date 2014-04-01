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

package org.opentripplanner.geocoder.reverse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;*/


@Path("/municoder")
public class MunicoderServer {

    /* FIXME Spring config looked like this:
        <bean id="boundaryResolver" class="org.opentripplanner.municoder.ShapefileBoundaryResolver"
    	  init-method="initialize">
    	<property name="shapefile" value="/var/otp/shp/pdx_boundaries.shp" />
    	<property name="nameField" value="CITYNAME" />
    </bean>

     */

    // FIXME inject context
    private BoundaryResolver boundaryResolver;
   
    public void setBoundaryResolver(BoundaryResolver boundaryResolver) {
        this.boundaryResolver = boundaryResolver;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON}) // APPLICATION_XML + "; charset=UTF-8", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    public String resolveLocation(
            @QueryParam("location") String location,
            @QueryParam("callback") String callback) {
        if (location == null) {
            error("no location to resolve");
        }
        String arr[] = location.split(",");
        //System.out.println("br="+boundaryResolver);
        //return arr[0];
        String result = boundaryResolver.resolve(Double.parseDouble(arr[1]), Double.parseDouble(arr[0]));
        result = (result == null) ? "{}" : "{ \"name\" : \""+result+"\" }";
        if(callback != null) {
            return callback+"("+result+");";
        }
        return result;
    }

    private void error(String message) {
        throw new WebApplicationException(Response.status(400)
                .entity(message)
                .type("text/plain")
                .build());
    }
}

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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.index.model.PatternDetail;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 *  Return patterns for route
 *
 *  Conceptually this is similar to IndexAPI.getPatternsForRoute, but includes more details and filtering options
 */
@Path("/routers/{routerId}/patternsForRoute")
@XmlRootElement
public class RoutePatternsResource {

    /**
     * Route to return patterns for
     */
    @QueryParam("route")
    private String routeStr;

    /**
     * Minimum time in service, in Unix epoch time. Defaults to 1 hour before
     * the current time. If returnAllPatterns=true, this parameter is ignored.
     */
    @QueryParam("minTime")
    private Long minTime;

    /**
     * Maximum time in service, in Unix epoch time. Defaults to 1 hour before
     * the current time. If returnAllPatterns=true, this parameter is ignored.
     */
    @QueryParam("maxTime")
    private Long maxTime;

    /**
     * If true, return all patterns for route, otherwise, return only patterns
     * that are currently in service;
     */
    @QueryParam("returnAllPatterns")
    private boolean returnAllPatterns = false;

    private GraphIndex index;

    public RoutePatternsResource(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        index = router.graph.index;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q})
    public List<PatternDetail> getPatternsInService() {
        AgencyAndId routeId = AgencyAndId.convertFromString(routeStr, ':');
        Route route = index.routeForId.get(routeId);

        // look at testing whether patterns are in service at Timetable.temporallyViable

        throw new IllegalArgumentException("not implemented");
    }

}

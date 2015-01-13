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

import static org.opentripplanner.api.resource.ServerInfo.Q;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

@Path("/routers/{routerId}/metadata")
@XmlRootElement
public class Metadata {

    @Context OTPServer otpServer;

    /** Returns metadata about the graph -- presently, this is just the extent of the graph. */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public GraphMetadata getMetadata(@PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        return router.graph.getMetadata();
    }

}

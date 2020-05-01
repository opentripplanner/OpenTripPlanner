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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by demory on 7/26/18.
 */

@Path("/routers/{routerId}/park_and_ride")
public class ParkAndRide {

    @Context
    OTPServer otpServer;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParkAndRide(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @QueryParam("maxTransitDistance") Double maxTransitDistance,
            @PathParam("routerId") String routerId) {

        Router router = otpServer.getRouter(routerId);
        if (router == null) return null;

        Envelope envelope;
        if (lowerLeft != null) {
            envelope = getEnvelope(lowerLeft, upperRight);
        } else {
            envelope = new Envelope(-180, 180, -90, 90);
        }

        List<ParkAndRideInfo> prs = new ArrayList<>();
        for (Vertex v : router.graph.getVertices()) {
            // Check if vertex is a ParkAndRideVertex
            if (!(v instanceof ParkAndRideVertex)) continue;

            // Check if vertex is within envelope
            if (!envelope.contains(v.getX(), v.getY())) continue;

            // Check if vertex is within maxTransitDistance of a stop (if specified)
            if (maxTransitDistance != null) {
                List<TransitStop> stops = router.graph.streetIndex.getNearbyTransitStops(
                        new Coordinate(v.getX(), v.getY()), maxTransitDistance);
                if (stops.isEmpty()) continue;
            }

            prs.add(new ParkAndRideInfo((ParkAndRideVertex) v));
        }

        return Response.status(Status.OK).entity(prs).build();
    }

    /** Envelopes are in latitude, longitude format */
    public static Envelope getEnvelope(String lowerLeft, String upperRight) {
        String[] lowerLeftParts = lowerLeft.split(",");
        String[] upperRightParts = upperRight.split(",");

        Envelope envelope = new Envelope(Double.parseDouble(lowerLeftParts[1]),
                Double.parseDouble(upperRightParts[1]), Double.parseDouble(lowerLeftParts[0]),
                Double.parseDouble(upperRightParts[0]));
        return envelope;
    }

    public class ParkAndRideInfo {
        private static final long serialVersionUID = 1L;

        public String name;

        public Double x, y;

        public ParkAndRideInfo(ParkAndRideVertex vertex) {
            this.name = vertex.getName();
            this.x = vertex.getX();
            this.y = vertex.getY();
        }
    }

}

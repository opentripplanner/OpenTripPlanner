package org.opentripplanner.ext.parkAndRideApi;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by demory on 7/26/18.
 */

@Path("/routers/{ignoreRouterId}/park_and_ride")
public class ParkAndRideResource {

    @Context
    OTPServer otpServer;

    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated
    @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParkAndRide(
        @QueryParam("lowerLeft") String lowerLeft,
        @QueryParam("upperRight") String upperRight,
        @QueryParam("maxTransitDistance") Double maxTransitDistance
    ) {

        Router router = otpServer.getRouter();

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
                List<TransitStopVertex> stops = router.graph.getStreetIndex().getNearbyTransitStops(
                    new Coordinate(v.getX(), v.getY()), maxTransitDistance);
                if (stops.isEmpty()) { continue; }
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

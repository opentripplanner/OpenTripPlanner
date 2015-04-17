package org.opentripplanner.api.resource;

import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.api.model.PointSetShort;
import org.opentripplanner.standalone.OTPServer;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This Jersey REST Resource creates and lists PointSets.
 *
 * PointSets serve as destinations in web analyst one-to-many indicators.
 * They can also serve as origins in many-to-many indicators.
 *
 * PointSets are one of the three main web analyst resources:
 * Pointsets
 * Indicators
 * TimeSurfaces
 */
@Path("/pointsets")
public class PointSetResource {

    @Context
    OTPServer server;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPointSets () {
        return Response.ok().entity(PointSetShort.list(server.pointSetCache.getPointSetIds())).build();
    }

    @GET
    @Path("/{pointSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPointSet (
    		@PathParam("pointSetId") String pointSetId) {
    	
        final PointSet pset = server.pointSetCache.get(pointSetId);
        if (pset == null) {
            return Response.status(Status.NOT_FOUND).entity("Invalid PointSet ID.").build();
        }
        if (pset.capacity > 200) {
            // too big, just give a summary
            return Response.ok().entity(new PointSetShort(pointSetId, pset)).build();
        }
        return Response.ok().entity(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                pset.writeJson(output);
            }
        }).build();
    }

}

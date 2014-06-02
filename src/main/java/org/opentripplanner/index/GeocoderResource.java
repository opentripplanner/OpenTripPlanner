package org.opentripplanner.index;

import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * OTP simple built-in geocoder.
 * Client geocoder modules usually read XML, but GeocoderBuiltin reads JSON.
 */
@Path("/routers/{routerId}/geocode")
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderResource {

    private final LuceneIndex index;

    public GeocoderResource (@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        GraphIndex graphIndex = otpServer.graphService.getGraph(routerId).index;
        synchronized (graphIndex) {
            if (graphIndex.luceneIndex == null) {
                // Synchronously lazy-initialize the Lucene index
                graphIndex.luceneIndex = new LuceneIndex(graphIndex, false);
            }
            index = graphIndex.luceneIndex;
        }
    }

    @GET
    public Response textSearch (@QueryParam("query") String query) {
        return Response.status(Response.Status.OK).entity(index.query(query)).build();
    }

}

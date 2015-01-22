package org.opentripplanner.index;

import org.opentripplanner.common.LuceneIndex;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import javax.ws.rs.DefaultValue;
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
        Router router = otpServer.getRouter(routerId);
        GraphIndex graphIndex = router.graph.index;
        synchronized (graphIndex) {
            if (graphIndex.luceneIndex == null) {
                // Synchronously lazy-initialize the Lucene index
                graphIndex.luceneIndex = new LuceneIndex(graphIndex, otpServer.basePath, false);
            }
            index = graphIndex.luceneIndex;
        }
    }

    /**
     * Geocode using data using the OTP graph for stops, clusters and street names
     *
     * @param query The query string we want to geocode
     * @param autocomplete Whether we should use the query string to do a prefix match
     * @param stops Search for stops, either by name or stop code
     * @param clusters Search for clusters by their name
     * @param corners Search for street corners using at least one of the street names
     * @return list of results in in the format expected by GeocoderBuiltin.js in the OTP Leaflet client
     */
    @GET
    public Response textSearch (@QueryParam("query") String query,
                                @QueryParam("autocomplete") @DefaultValue("false") boolean autocomplete,
                                @QueryParam("stops") @DefaultValue("true") boolean stops,
                                @QueryParam("clusters") @DefaultValue("false") boolean clusters,
                                @QueryParam("corners") @DefaultValue("true") boolean corners
                                ) {
        return Response.status(Response.Status.OK).entity(index.query(query, autocomplete, stops, clusters, corners)).build();
    }

}

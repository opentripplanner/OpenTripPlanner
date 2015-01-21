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
     * @param autoComplete Whether we should use the query string to do a prefix match
     * @param showStops Search for stops, either for name or stop code
     * @param showClusters Search for clusters by their name
     * @param showCorners Search for street corners using at least one of the street names
     * @return list of results in in the format expected by GeocoderBuiltin.js in the OTP Leaflet client
     */
    @GET
    public Response textSearch (@QueryParam("query") String query,
                                @QueryParam("autoComplete") @DefaultValue("false") boolean autoComplete,
                                @QueryParam("showStops") @DefaultValue("true") boolean showStops,
                                @QueryParam("showClusters") @DefaultValue("false") boolean showClusters,
                                @QueryParam("showCorners") @DefaultValue("true") boolean showCorners
                                ) {
        return Response.status(Response.Status.OK).entity(index.query(query, autoComplete, showStops, showClusters, showCorners)).build();
    }

}

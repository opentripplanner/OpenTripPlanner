package org.opentripplanner.api.resource;

import com.google.common.collect.Maps;
import org.opentripplanner.analyst.Indicator;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.api.common.RoutingResource;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.routing.algorithm.EarliestArrivalSPTService;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.OTPServer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Path("/surfaces")
@Produces({ MediaType.APPLICATION_JSON })
public class SurfaceResource extends RoutingResource {

    @Context
    OTPServer server;

    @Context
    UriInfo uriInfo;

    @PUT
    public Response createSurface(@QueryParam("cutoffMinutes") @DefaultValue("90") int cutoffMinutes) {

        // Build the request
        try {
            RoutingRequest req = buildRequest(0); // batch must be true
            Graph graph = server.graphService.getGraph();
            req.setRoutingContext(graph);
            EarliestArrivalSPTService sptService = new EarliestArrivalSPTService();
            sptService.setMaxDuration(60 * cutoffMinutes);
            ShortestPathTree spt = sptService.getShortestPathTree(req);
            req.cleanup();
            if (spt != null) {
                TimeSurface surface = new TimeSurface(spt);
                surface.params = Maps.newHashMap();
                for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
                    // include only the first instance of each query parameter
                    surface.params.put(e.getKey(), e.getValue().get(0));
                }
                server.surfaceCache.add(surface);
                return Response.ok().entity(new TimeSurfaceShort(surface)).build(); // .created(URI)
            } else {
                return Response.noContent().entity("NO SPT").build();
            }
        } catch (ParameterException pex) {
            return Response.status(Response.Status.BAD_REQUEST).entity("BAD USER").build();
        }

    }

    /** List all the available surfaces. */
    @GET
    public Response getTimeSurfaceList () {
        return Response.ok().entity(TimeSurfaceShort.list(server.surfaceCache.cache)).build();
    }

    /** Describe a specific surface. */
    @GET @Path("/{surfaceId}")
    public Response getTimeSurfaceList (@PathParam("surfaceId") Integer surfaceId) {
        TimeSurface surface = server.surfaceCache.get(surfaceId);
        if (surface == null) return Response.status(Response.Status.NOT_FOUND).entity("Invalid surface ID.").build();
        return Response.ok().entity(new TimeSurfaceShort(surface)).build();
    }

    /** Evaluate a surface at all the points in a PointSet. */
    @GET @Path("/{surfaceId}/indicator")
    public Response getIndicator (@PathParam("surfaceId") Integer surfaceId,
                                  @QueryParam("targets")  String  targetPointSetId,
                                  @QueryParam("origins")  String  originPointSetId) {

        final TimeSurface surf = server.surfaceCache.get(surfaceId);
        if (surf == null) return badRequest("Invalid TimeSurface ID.");
        final PointSet pset = server.pointSetCache.get(targetPointSetId);
        if (pset == null) return badRequest("Missing or invalid target PointSet ID.");
        final Indicator indicator = new Indicator(pset, surf);
        if (indicator == null) return badServer("Could not compute indicator as requested.");
        return Response.ok().entity(new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                indicator.writeJson(output);
            }
        }).build();

    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Bad request: " + message).build();
    }

    private Response badServer(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Server fail: " + message).build();
    }

}

package org.opentripplanner.api.ws.deployer;

import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

import com.sun.jersey.api.spring.Autowire;
import com.sun.jersey.spi.resource.Singleton;

/**
 * These API endpoints correspond to methods on the GraphService interface.
 * They allow remotely loading, reloading, and evicting graphs on a running server.
 * @author abyrd
 */
@Secured({ "ROLE_DEPLOYER" })
@Path("/deployer/routerIds/")
@Singleton
@Autowire
public class RouterIds {
    
    @Autowired GraphService graphService;

    @GET @Produces({ MediaType.APPLICATION_JSON })
    public Response getAllGraphIds() {
        Collection<String> ids = graphService.getGraphIds();
        return Response.status(Status.OK).entity(ids).build();
    }

    @PUT @Produces({ MediaType.APPLICATION_JSON })
    public Response reloadGraphs(@QueryParam("path") String path, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict) {
        GraphServiceImpl gsi = (GraphServiceImpl) graphService;
        if (path != null) {
            gsi.setPath(path);
            return Response.status(Status.OK).entity("updated path.").build();
        } else {
            boolean allSuccess = gsi.reloadGraphs(preEvict);
            Collection<String> ids = graphService.getGraphIds();
            return Response.status(Status.OK).build();
        }
    }

    @GET @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response getGraphId(@PathParam("routerId") String routerId) {
        Graph graph = graphService.getGraph(routerId);
        if (graph == null)
            return Response.status(Status.NOT_FOUND).entity("graph id not registered.").build();
        else
            return Response.status(Status.OK).entity(graph.toString()).build();
    }
    
    @PUT @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response putGraphId(@PathParam("routerId") String routerId, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict) {
        boolean success = graphService.registerGraph(routerId, preEvict);
        if (success)
            return Response.status(201).entity("graph registered.").build();
        else
            return Response.status(404).entity("graph not found or other error.").build();
    }

    @DELETE @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteGraphId(@PathParam("routerId") String routerId) {
        boolean existed = graphService.evictGraph(routerId);
        if (existed)
            return Response.status(200).entity("graph evicted.").build();
        else
            return Response.status(404).entity("graph did not exist.").build();
    }
    
}

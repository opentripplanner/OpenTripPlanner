package org.opentripplanner.api.ws.deployer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collection;

import javax.ws.rs.Consumes;
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
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger LOG = LoggerFactory.getLogger(RouterIds.class);

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
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response putGraphId(
            @PathParam("routerId") String routerId, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict, 
            @QueryParam("upload") @DefaultValue("false") boolean upload,
            InputStream is) {
        Graph graph;
        if (preEvict) {
            LOG.debug("pre-evicting graph");
            graphService.evictGraph(routerId);
        }
        if (upload) {
            LOG.debug("deserializing graph from PUT data stream...");
            try {
                graph = Graph.load(is, LoadLevel.FULL);
                graphService.registerGraph(routerId, graph);
                return Response.status(Status.CREATED).entity(graph.toString()).build();
            } catch (Exception e) {
                return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
            }
        } else { // load from local filesystem
            LOG.debug("attempting to load graph from server's local filsystem.");
            boolean success = graphService.registerGraph(routerId, preEvict);
            if (success)
                return Response.status(201).entity("graph registered.").build();
            else
                return Response.status(404).entity("graph not found or other error.").build();
        }
    }

    @DELETE @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteAll() {
        int nEvicted = graphService.evictAll();
        String message = String.format("%d graphs evicted.", nEvicted);
        return Response.status(200).entity(message).build();
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

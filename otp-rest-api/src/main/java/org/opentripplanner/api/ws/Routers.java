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

package org.opentripplanner.api.ws;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.api.model.RouterInfo;
import org.opentripplanner.api.model.RouterList;
import org.opentripplanner.api.ws.impl.StoredHullService;
import org.opentripplanner.api.ws.services.HullService;
import org.opentripplanner.common.geometry.GraphUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.Responses;
import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Geometry;

/**
 * This REST API endpoint allows remotely loading, reloading, and evicting graphs on a running server.
 * 
 * A GraphService maintains a mapping between routerIds and specific Graph objects.
 * The HTTP verbs are used as follows to manipulate that mapping:
 * 
 * GET - see the registered routerIds and Graphs, verify whether a particular routerId is registered
 * PUT - create or replace a mapping from a routerId to a Graph loaded from the server filesystem
 * POST - create or replace a mapping from a routerId to a serialized Graph sent in the request
 * DELETE - de-register a routerId, releasing the reference to the associated graph
 * 
 * The HTTP request URLs are of the form /ws/routers/{routerId}, where the routerId is optional. 
 * If a routerId is supplied in the URL, the verb will act upon the mapping for that specific 
 * routerId. If no routerId is given, the verb will act upon all routerIds currently registered.
 * 
 * For example:
 * 
 * GET http://localhost/opentripplanner-api-webapp/ws/routers
 * will retrieve a list of all registered routerId -> Graph mappings and their geographic bounds.
 * 
 * GET http://localhost/opentripplanner-api-webapp/ws/routers/london
 * will return status code 200 and a brief description of the 'london' graph including geographic 
 * bounds, or 404 if the 'london' routerId is not registered.
 * 
 * PUT http://localhost/opentripplanner-api-webapp/ws/routers
 * will reload the graphs for all currently registered routerIds from disk.
 * 
 * PUT http://localhost/opentripplanner-api-webapp/ws/routers/paris
 * will load a Graph from a sub-directory called 'paris' and associate it with the routerId 'paris'.
 * 
 * DELETE http://localhost/opentripplanner-api-webapp/ws/routers/paris
 * will release the Paris Graph and de-register the 'paris' routerId.
 * 
 * DELETE http://localhost/opentripplanner-api-webapp/ws/routers
 * will de-register all currently registered routerIds.
 * 
 * The GET methods are not secured, but all other methods are secured under ROLE_ROUTERS.
 * See documentation for individual methods for additional parameters.
 */
@Path("/routers")
@XmlRootElement
@Autowire
public class Routers {

    private static final Logger LOG = LoggerFactory.getLogger(Routers.class);

    @Autowired GraphService graphService;
    
    /** 
     * Returns a list of routers and their bounds. 
     * @return a representation of the graphs and their geographic bounds, in JSON or XML depending
     * on the Accept header in the HTTP request.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public RouterList getRouterIds()
            throws JSONException {
        RouterList routerList = new RouterList();
        for (String id : graphService.getRouterIds()) {
            routerList.routerInfo.add(getRouterInfo(id));
        }
        return routerList;
    }

    /** 
     * Returns the bounds for a specific routerId, or verifies whether it is registered. 
     * @returns status code 200 if the routerId is registered, otherwise a 404.
     */
    @GET @Path("{routerId}") @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public RouterInfo getGraphId(@PathParam("routerId") String routerId) {
        // factor out build one entry
        RouterInfo routerInfo = getRouterInfo(routerId);
        if (routerInfo == null)
            throw new WebApplicationException(Responses.notFound()
                    .entity("Graph id '" + routerId + "' not registered.").type("text/plain")
                    .build());
        return routerInfo;
    }
    
    private RouterInfo getRouterInfo(String routerId) {
        Graph graph = graphService.getGraph(routerId);
        if (graph == null)
            return null;
        RouterInfo routerInfo = new RouterInfo();
        routerInfo.routerId = routerId;
        HullService service = graph.getService(HullService.class);
        if (service == null) {
            Geometry hull = GraphUtils.makeConvexHull(graph);
            service = new StoredHullService(hull);
            graph.putService(HullService.class, service);
        }
        routerInfo.polygon = service.getHull();
        return routerInfo;
    }

    /** 
     * Reload the graphs for all registered routerIds from disk.
     */
    @Secured({ "ROLE_ROUTERS" })
    @PUT @Produces({ MediaType.APPLICATION_JSON })
    public Response reloadGraphs(@QueryParam("path") String path, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict) {
        GraphServiceImpl gsi = (GraphServiceImpl) graphService;
        gsi.reloadGraphs(preEvict);
        return Response.status(Status.OK).build();
    }

    /** 
     * Load the graph for the specified routerId from disk.
     * @param preEvict before reloading each graph, evict the existing graph. This will prevent 
     * memory usage from increasing during the reload, but routing will be unavailable on this 
     * routerId for the duration of the operation.
     * @param upload read the graph from the PUT data stream instead of from disk.
     */
    @Secured({ "ROLE_ROUTERS" })
    @PUT @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response putGraphId(
            @PathParam("routerId") String routerId, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict) {
        if (preEvict) {
            LOG.debug("pre-evicting graph");
            graphService.evictGraph(routerId);
        }
        LOG.debug("attempting to load graph from server's local filsystem.");
        boolean success = graphService.registerGraph(routerId, preEvict);
        if (success)
            return Response.status(201).entity("graph registered.").build();
        else
            return Response.status(404).entity("graph not found or other error.").build();
    }

    /** 
     * Deserialize a graph sent with the HTTP request as POST data, associating it with the given 
     * routerId.
     */
    @Secured({ "ROLE_ROUTERS" })
    @POST @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response postGraphOverWire (
            @PathParam("routerId") String routerId, 
            @QueryParam("preEvict") @DefaultValue("true") boolean preEvict, 
            @QueryParam("loadLevel") @DefaultValue("FULL") LoadLevel level,
            InputStream is) {
        if (preEvict) {
            LOG.debug("pre-evicting graph");
            graphService.evictGraph(routerId);
        }
        LOG.debug("deserializing graph from POST data stream...");
        Graph graph;
        try {
            graph = Graph.load(is, level);
            graphService.registerGraph(routerId, graph);
            return Response.status(Status.CREATED).entity(graph.toString()).build();
        } catch (Exception e) {
            return Response.status(Status.BAD_REQUEST).entity(e.toString()).build();
        }
    }

    /** De-register all registered routerIds, evicting them from memory. */
    @Secured({ "ROLE_ROUTERS" })
    @DELETE @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteAll() {
        int nEvicted = graphService.evictAll();
        String message = String.format("%d graphs evicted.", nEvicted);
        return Response.status(200).entity(message).build();
    }

    /** 
     * De-register a specific routerId, evicting the associated graph from memory. 
     * @return status code 200 if the routerId was de-registered, 
     * 404 if the routerId was not registered. 
     */
    @Secured({ "ROLE_ROUTERS" })
    @DELETE @Path("{routerId}") @Produces({ MediaType.TEXT_PLAIN })
    public Response deleteGraphId(@PathParam("routerId") String routerId) {
        boolean existed = graphService.evictGraph(routerId);
        if (existed)
            return Response.status(200).entity("graph evicted.").build();
        else
            return Response.status(404).entity("graph did not exist.").build();
    }

}

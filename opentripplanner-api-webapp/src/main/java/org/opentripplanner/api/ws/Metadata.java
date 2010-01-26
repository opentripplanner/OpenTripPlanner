package org.opentripplanner.api.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

@Path("/metadata")
@XmlRootElement
@Autowire
public class Metadata {

    private Graph graph;

    @Autowired
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Returns metadata about the graph -- presently, this is just the extent of the graph.
     *
     * @return Returns either an XML or a JSON document, depending on the HTTP Accept header of the
     *         client making the request.
     *
     * @throws JSONException
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public GraphMetadata getMetadata() throws JSONException {

        return new GraphMetadata(graph);
    }
}

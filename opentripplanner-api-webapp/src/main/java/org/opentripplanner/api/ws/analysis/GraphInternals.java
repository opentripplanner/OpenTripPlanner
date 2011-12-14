package org.opentripplanner.api.ws.analysis;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.api.model.analysis.EdgeSet;
import org.opentripplanner.api.model.analysis.FeatureCount;
import org.opentripplanner.api.model.analysis.VertexSet;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Graph.LoadLevel;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.access.annotation.Secured;

import com.sun.jersey.api.spring.Autowire;
import com.sun.jersey.spi.resource.Singleton;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * Data about the edges and vertices of a graph. This data is tied to a particular internal graph
 * representation. OTP internals
 * 
 * @author novalis
 * 
 */
@Path("/internals")
@XmlRootElement
@Autowire
@Singleton
public class GraphInternals {
    private GraphService graphService;

    STRtree vertexIndex;

    STRtree edgeIndex;

    @Required
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    public synchronized void initIndexes() {
        if (vertexIndex != null) {
            return;
        }
        graphService.setLoadLevel(LoadLevel.DEBUG);
        Graph graph = graphService.getGraph();
        vertexIndex = new STRtree();
        edgeIndex = new STRtree();
        for (Vertex v : graph.getVertices()) {
            Envelope vertexEnvelope = new Envelope(v.getCoordinate());
            vertexIndex.insert(vertexEnvelope, v);
            for (Edge e : v.getOutgoing()) {
                if (e instanceof DirectEdge) {
                    Envelope envelope;
                    Geometry geometry = ((DirectEdge) e).getGeometry();
                    if (geometry == null) {
                        envelope = vertexEnvelope;
                    } else {
                        envelope = geometry.getEnvelopeInternal();
                    }
                    edgeIndex.insert(envelope, e);
                }
            }
        }
        vertexIndex.build();
        edgeIndex.build();
    }

    /**
     * Get vertices inside a bbox.
     * 
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/vertices")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getVertices(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight) {

        initIndexes();

        Envelope envelope = getEnvelope(lowerLeft, upperRight);

        VertexSet out = new VertexSet();
        Graph graph = graphService.getGraph();

        @SuppressWarnings("unchecked")
        List<Vertex> query = vertexIndex.query(envelope);
        out.vertices = query;   
        return out.withGraph(graph);
    }


    /**
     * Get edges inside a bbox.
     * 
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/edges")
    @Produces({ MediaType.APPLICATION_JSON })
    public Object getEdges(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight) {

        initIndexes();

        Envelope envelope = getEnvelope(lowerLeft, upperRight);

        EdgeSet out = new EdgeSet();
        Graph graph = graphService.getGraph();

        @SuppressWarnings("unchecked")
        List<Edge> query = edgeIndex.query(envelope);
        out.edges = query;   
        return out.withGraph(graph);
    }


    /**
     * Count vertices and edges inside a bbox.
     * 
     * @return
     */
    @Secured({ "ROLE_USER" })
    @GET
    @Path("/countFeatures")
    @Produces({ MediaType.APPLICATION_JSON })
    public FeatureCount countVertices(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight) {

        initIndexes();

        Envelope envelope = getEnvelope(lowerLeft, upperRight);

        FeatureCount out = new FeatureCount();

        @SuppressWarnings("unchecked")
        List<Vertex> vertexQuery = vertexIndex.query(envelope);
        out.vertices = vertexQuery.size();
        
        @SuppressWarnings("unchecked")
        List<Edge> edgeQuery = edgeIndex.query(envelope);
        out.edges = edgeQuery.size();
        
        return out;
    }

    private Envelope getEnvelope(String lowerLeft, String upperRight) {
        String[] lowerLeftParts = lowerLeft.split(",");
        String[] upperRightParts = upperRight.split(",");

        Envelope envelope = new Envelope(Double.parseDouble(lowerLeftParts[1]),
                Double.parseDouble(upperRightParts[1]), Double.parseDouble(lowerLeftParts[0]),
                Double.parseDouble(upperRightParts[0]));
        return envelope;
    }
}

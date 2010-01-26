package org.opentripplanner.api.ws;

import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Envelope;

@XmlRootElement
public class GraphMetadata {

    /**
     * The bounding box of the graph, in decimal degrees.
     */
    private double minLatitude, minLongitude, maxLatitude, maxLongitude;

    public GraphMetadata() {
    }

    public GraphMetadata(Graph graph) {
        /* generate extents */
        Envelope env = new Envelope();
        for (Vertex v : graph.getVertices()) {
            env.expandToInclude(v.getCoordinate());
        }
        minLongitude = env.getMinX();
        maxLongitude = env.getMaxX();
        minLatitude = env.getMinY();
        maxLatitude = env.getMaxY();
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

}

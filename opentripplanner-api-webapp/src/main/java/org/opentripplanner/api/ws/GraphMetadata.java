package org.opentripplanner.api.ws;

import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Envelope;

@XmlRootElement
public class GraphMetadata {

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

    public void setMinLatitude(double minLatitude) {
        this.minLatitude = minLatitude;
    }

    public double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLongitude(double minLongitude) {
        this.minLongitude = minLongitude;
    }

    public double getMinLongitude() {
        return minLongitude;
    }

    public void setMaxLatitude(double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLongitude(double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public double getMaxLongitude() {
        return maxLongitude;
    }

}

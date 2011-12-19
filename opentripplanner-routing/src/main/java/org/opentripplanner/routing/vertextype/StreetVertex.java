package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Graph;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class StreetVertex extends AbstractVertex {

    private static final long serialVersionUID = 1L;

    String streetName;

    public StreetVertex(Graph g, String label, Coordinate coord, String streetName) {
        this(g, label, coord.x, coord.y, streetName);
        this.streetName = streetName; 
    }

    public StreetVertex(Graph g, String label, double x, double y, String streetName) {
        super(g, label, x, y);
        this.streetName = streetName; 
    }
    
    public String getStreetName() {
        return streetName;
    }

}

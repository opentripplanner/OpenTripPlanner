package org.opentripplanner.profile;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;

public class StreetEdgeInfo {

    public Integer edgeId;
    public Integer distance;
    public EncodedPolylineBean geometry;
    public String mode;
    public String streetName;
    public RelativeDirection relativeDirection;
    public AbsoluteDirection absoluteDirection;
    public Boolean stayOn; 
    public Boolean area; 
    public Boolean bogusName; 

    public StreetEdgeInfo(Edge edge) {
        this.edgeId = edge.getId();
        this.distance = (int) edge.getDistance();
        if(edge.getGeometry() != null) {
            this.geometry = PolylineEncoder.createEncodings(edge.getGeometry());
        }
    }
}

package org.opentripplanner.profile;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;


/**
 * This is a response model class which holds data that will be serialized and returned to the client.
 * It is not used internally in routing.
 * It represents a single street edge in a series of on-street (walking/biking/driving) directions.
 * TODO could this be merged with WalkStep when profile routing and normal routing converge?
 */
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
    
    public BikeRentalStationInfo bikeRentalOnStation;
    public BikeRentalStationInfo bikeRentalOffStation;
    
    public StreetEdgeInfo(Edge edge) {
        this.edgeId = edge.getId();
        this.distance = (int) edge.getDistance();
        if(edge.getGeometry() != null) {
            this.geometry = PolylineEncoder.createEncodings(edge.getGeometry());
        }
    }
}

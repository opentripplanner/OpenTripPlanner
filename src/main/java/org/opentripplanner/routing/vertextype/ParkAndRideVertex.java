package org.opentripplanner.routing.vertextype;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;

/**
 * A vertex for a park and ride area.
 * Connected to streets by ParkAndRideLinkEdge.
 * Transition for parking the car is handled by ParkAndRideEdge.
 * 
 * @author laurent
 * 
 */
public class ParkAndRideVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private String id;

    public ParkAndRideVertex(Graph g, String label, String id, double x, double y, I18NString name) {
        super(g, label, x, y, name);
        setId(id);
    }
    
    public void setId(String id){
    	this.id = id;
    }
    
    public String getId(){
    	return this.id;
    }
}

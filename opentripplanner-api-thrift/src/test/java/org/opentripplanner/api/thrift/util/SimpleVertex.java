package org.opentripplanner.api.thrift.util;

import org.opentripplanner.api.thrift.definition.GraphVertex;
import org.opentripplanner.api.thrift.definition.LatLng;
import org.opentripplanner.api.thrift.definition.Location;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * Vertex class used for testing.
 * @author avi
 *
 */
class SimpleVertex extends StreetVertex {

    private static final long serialVersionUID = 1L;

    public SimpleVertex(Graph g, String label, double lat, double lon) {
        super(g, label, lon, lat, label);
    }
    
    public boolean equals(GraphVertex gv) {
    	boolean b = (gv.getLabel() == getLabel() &&
    				 gv.getName() == getName() &&
    				 gv.getIn_degree() == getDegreeIn() &&
    				 gv.getOut_degree() == getDegreeOut());
    	if (!b) return false;
    	
    	Location loc = gv.getLocation();
    	LatLng ll = loc.getLat_lng();
    	b = (ll.getLat() == getY() &&
    		 ll.getLng() == getX());
    	return b;
    }
}

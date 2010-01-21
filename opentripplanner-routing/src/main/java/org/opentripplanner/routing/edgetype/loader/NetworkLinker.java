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

package org.opentripplanner.routing.edgetype.loader;

import java.util.ArrayList;
import java.util.Collection;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextypes.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkLinker {
    
    private static Logger _log = LoggerFactory.getLogger(NetworkLinker.class);
    
    private Graph graph;

    public NetworkLinker(Graph graph) {
        this.graph = graph;
    }

    /**
     * Link the transit network to the street network.  Connect each transit vertex to the nearest
     * Street edge with a StreetTransitLink.  
     */
    public void createLinkage() {

        _log.debug("constructing index...");
        StreetVertexIndexServiceImpl index = new StreetVertexIndexServiceImpl(graph);
        index.setup();
        
        _log.debug("creating linkages...");
        int i = 0;
        ArrayList<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        
        for (Vertex v : vertices) {
            
            if( i % 500 == 0)
                _log.debug("vertices=" + i + "/" + vertices.size());
            i++;
            
            if (v.getType() == TransitStop.class) {
                Vertex nearestIntersection = index.getClosestVertex(v.getCoordinate(), false);

                if (nearestIntersection != null) {
                    if (nearestIntersection instanceof StreetLocation) {
                        nearestIntersection = ((StreetLocation) nearestIntersection).reify(graph);
                    }
                    graph.addEdge(new StreetTransitLink(nearestIntersection, v, true));
                    graph.addEdge(new StreetTransitLink(v, nearestIntersection, false));
                }
            }
        }
    }
}

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

import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OneStreetVertex;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
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
        index.setup_modifiable();
        
        _log.debug("creating linkages...");
        int i = 0;
        ArrayList<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());
        
        for (Vertex v : vertices) {
            
            if( i % 500 == 0)
                _log.debug("vertices=" + i + "/" + vertices.size());
            i++;
            
            if (v instanceof TransitStop) {
                Vertex nearestIntersection = index.getClosestVertex(v.getCoordinate(), false);

                if (nearestIntersection != null) {
                    if (nearestIntersection instanceof StreetLocation) {
                        if(((StreetLocation) nearestIntersection).streets != null) {
                            ((StreetLocation) nearestIntersection).reify(graph);
                            index.reified((StreetLocation) nearestIntersection);
                        }
                    } else if (nearestIntersection instanceof OneStreetVertex) {
                        //this kind of vertex can only have one Street edge in each direction
                        //so we need to create a spare vertex to connect the STL to.

                        OneStreetVertex osvertex = ((OneStreetVertex) nearestIntersection);
                        GenericVertex newV = new GenericVertex(nearestIntersection.getLabel() + " approach", nearestIntersection.getX(), nearestIntersection.getY());
                        Street approach = new Street(nearestIntersection, newV, 0);
                        Street approachBack = new Street(newV, nearestIntersection, 0);
                        osvertex.inStreet.setToVertex(newV);
                        osvertex.outStreet.setFromVertex(newV);
                        newV.addIncoming(osvertex.inStreet);
                        newV.addOutgoing(osvertex.outStreet);
                        osvertex.inStreet = approachBack;
                        osvertex.outStreet = approach;
                        nearestIntersection = newV;
                        graph.addVertex(newV);
                    }
                    TransitStop ts = (TransitStop) v;
                    boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                    graph.addEdge(new StreetTransitLink(nearestIntersection, v, wheelchairAccessible));
                    graph.addEdge(new StreetTransitLink(v, nearestIntersection, wheelchairAccessible));
                }
            }
        }
    }
}

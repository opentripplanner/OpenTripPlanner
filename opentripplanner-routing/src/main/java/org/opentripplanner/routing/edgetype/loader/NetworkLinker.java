/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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

import java.util.Collection;
import java.util.List;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.vertextypes.Intersection;
import org.opentripplanner.routing.vertextypes.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

public class NetworkLinker {
    
    private static Logger _log = LoggerFactory.getLogger(NetworkLinker.class);
    
    private Graph graph;

    public NetworkLinker(Graph graph) {
        this.graph = graph;
    }

    @SuppressWarnings("unchecked")
    public void createLinkage() {

        STRtree index = new STRtree();
        Collection<Vertex> vertices = graph.getVertices();

        _log.debug("constructing index...");
        
        for (Vertex v : vertices) {
            index.insert(new Envelope(v.getCoordinate()), v);
        }
        
        _log.debug("creating linkages...");
        int i = 0;
        
        for (Vertex v : vertices) {
            
            if( i % 100 == 0)
                _log.debug("vertices=" + i + "/" + vertices.size());
            i++;
            
            if (v.getType() == TransitStop.class) {
                // find nearby vertices

                Envelope env = new Envelope(v.getCoordinate());
                env.expandBy(0.0018); 
                List<Vertex> nearby = (List<Vertex>) index.query(env);

                Vertex nearestIntersection = null;
                double minDistance = 1000000000;
                Coordinate coord = v.getCoordinate();
                double lat1 = coord.y;
                double lon1 = coord.x;
                for (Vertex nv : nearby) {
                    if (nv == v) {
                        continue;
                    }
                    if (nv.distance(v) > 200) {
                        //STRtree.query does not guarantee that all found points will actually be within the envelope
                        continue;
                    }
                    coord = nv.getCoordinate();
                    double lat2 = coord.y;
                    double lon2 = coord.x;
                    double distance = DistanceLibrary.distance(lat1, lon1, lat2, lon2) * 2;

                    if (nv.getType() == Intersection.class) {
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearestIntersection = nv;
                        }
                    }
                }

                if (nearestIntersection != null) {
                    graph.addEdge(new StreetTransitLink(nearestIntersection, v, true));
                    graph.addEdge(new StreetTransitLink(v, nearestIntersection, false));
                }
            }
        }
    }
}

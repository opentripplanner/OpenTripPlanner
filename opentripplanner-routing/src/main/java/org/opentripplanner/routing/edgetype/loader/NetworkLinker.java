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

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.factory.FindMaxWalkDistances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkLinker {

    private static Logger _log = LoggerFactory.getLogger(NetworkLinker.class);

    private Graph graph;

    private NetworkLinkerLibrary networkLinkerLibrary;

    public NetworkLinker(Graph graph) {
        this.graph = graph;
        this.networkLinkerLibrary = new NetworkLinkerLibrary(graph);
    }

    /**
     * Link the transit network to the street network. Connect each transit vertex to the nearest
     * Street edge with a StreetTransitLink.
     * 
     * @param index
     */
    public void createLinkage() {

        _log.debug("creating linkages...");
        int i = 0;
        ArrayList<GraphVertex> vertices = new ArrayList<GraphVertex>(graph.getVertices());

        for (GraphVertex gv : vertices) {
            Vertex v = gv.vertex;
            if (i % 100000 == 0)
                _log.debug("vertices=" + i + "/" + vertices.size());
            i++;

            if (v instanceof TransitStop) {
                // only connect transit stops that (a) are entrances, or (b) have no associated
                // entrances
                TransitStop ts = (TransitStop) v;
                if (!ts.isEntrance()) {
                    boolean hasEntrance = false;

                    for (Edge e : gv.getOutgoing()) {
                        if (e instanceof PathwayEdge) {
                            hasEntrance = true;
                            break;
                        }
                    }
                    if (hasEntrance) {
                        // transit stop has entrances
                        continue;
                    }
                }

                boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                if (!networkLinkerLibrary.connectVertexToStreets(v, wheelchairAccessible)) {
                    _log.warn("Stop " + ts + " not near any streets; it will not be usable");
                }
            }
        }

        networkLinkerLibrary.addAllReplacementEdgesToGraph();
        networkLinkerLibrary.markLocalStops();
        FindMaxWalkDistances.find(graph);
    }
}

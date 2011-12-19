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

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.GraphBuilderAnnotation.Variety;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.factory.FindMaxWalkDistances;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
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
        // iterate over a copy of vertex list because it will be modified
        ArrayList<Vertex> vertices = new ArrayList<Vertex>(graph.getVertices());

        for (TransitStop ts : IterableLibrary.filter(vertices, TransitStop.class)) {
            // only connect transit stops that (a) are entrances, or (b) have no associated
            // entrances
            if (ts.isEntrance() || !ts.hasEntrances()) {
                boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                if (!networkLinkerLibrary.connectVertexToStreets(ts, wheelchairAccessible)) {
                    _log.warn(GraphBuilderAnnotation.register(graph, Variety.STOP_UNLINKED, ts));
                }
            }
        }
        // no longer necessary
        //networkLinkerLibrary.addAllReplacementEdgesToGraph();
        networkLinkerLibrary.markLocalStops();
        FindMaxWalkDistances.find(graph);
    }
}

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

package org.opentripplanner.graph_builder.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;

public class StreetUtils {

    /**
     * Merge certain unnecessary vertices of a street graph.  Specifically,
     * deletes a @{link StreetVertex} if it has connections only to two neighbors, 
     * and has the same name as one of those neighbors.
     *   
     * @param graph
     */
    public static void unify(Graph graph) {
        ArrayList<GraphVertex> vertices = new ArrayList<GraphVertex>(graph.getVertices());
        for (GraphVertex gv : vertices) {
            if (gv.getDegreeOut() == 2 && gv.getDegreeIn() == 2) {
                //this corner may be entirely removable

                Collection<Edge> edges = gv.getOutgoing();
                Iterator<Edge> it = edges.iterator();
                Edge out1 = it.next();
                Edge out2 = it.next();

                if (out1.getName() == out2.getName() && out1.getName() != null) {
                    //remove vertex
                    GenericVertex tov1 = (GenericVertex) out1.getToVertex();
                    /*
                    GenericVertex tov2 = (GenericVertex) out2.getToVertex();
                    graph.removeVertex(gv.vertex);
                    for (Edge e: gv.getIncoming()) {
                        graph.getGraphVertex(e.getFromVertex()).removeOutgoing(e);
                    }
                    graph.getGraphVertex(tov1).removeIncoming(out1);
                    graph.getGraphVertex(tov2).removeIncoming(out2);
*/
                    tov1.mergeFrom(graph, (GenericVertex) gv.vertex);
                }
            }
        }
    }

}

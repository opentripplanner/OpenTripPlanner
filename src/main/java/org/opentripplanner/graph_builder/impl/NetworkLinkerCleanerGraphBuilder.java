/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.graph_builder.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Graph;

/**
 *
 * @author mabu
 */
public class NetworkLinkerCleanerGraphBuilder implements GraphBuilder {

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        NetworkLinkerLibrary networkLinkerLibrary = (NetworkLinkerLibrary) extra.get(NetworkLinkerLibrary.class);
        //remove replaced edges
        for (HashSet<StreetEdge> toRemove : networkLinkerLibrary.getReplacements().keySet()) {
            for (StreetEdge edge : toRemove) {
                edge.getFromVertex().removeOutgoing(edge);
                edge.getToVertex().removeIncoming(edge);
            }
        }
        //and add back in replacements
        for (LinkedList<P2<StreetEdge>> toAdd : networkLinkerLibrary.getReplacements().values()) {
            for (P2<StreetEdge> edges : toAdd) {
                StreetEdge edge1 = edges.first;
                if (edge1.getToVertex().getLabel().startsWith("split ") || edge1.getFromVertex().getLabel().startsWith("split ")) {
                    continue;
                }
                edge1.getFromVertex().addOutgoing(edge1);
                edge1.getToVertex().addIncoming(edge1);
                StreetEdge edge2 = edges.second;
                if (edge2 != null) {
                    edge2.getFromVertex().addOutgoing(edge2);
                    edge2.getToVertex().addIncoming(edge2);
                }
            }
        }
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("network_linking_cleaning");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "network_linking");
    }

    @Override
    public void checkInputs() {
    }
}

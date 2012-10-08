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

package org.opentripplanner.api.model.internals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opentripplanner.model.json_serialization.EdgeSetJSONSerializer;
import org.opentripplanner.model.json_serialization.WithGraph;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class EdgeSet {
    public List<WrappedEdge> edges;

    @JsonSerialize(using = EdgeSetJSONSerializer.class)
    class EdgeSetWithGraph extends WithGraph {
        EdgeSetWithGraph(Graph graph, EdgeSet edgeSet) {
            super(graph, edgeSet);
        }
    }

    public EdgeSetWithGraph withGraph(Graph graph) {
        return new EdgeSetWithGraph(graph, this);
    }

    public void addEdges(Collection<Edge> newEdges, Graph graph) {
        if (edges == null) {
            edges = new ArrayList<WrappedEdge>();
        }
        for (Edge edge : newEdges) {
            edges.add(new WrappedEdge(edge, graph.getIdForEdge(edge)));
        }
    }
}

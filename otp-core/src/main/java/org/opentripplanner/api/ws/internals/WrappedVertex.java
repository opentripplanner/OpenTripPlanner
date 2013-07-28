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

package org.opentripplanner.api.ws.internals;

import org.opentripplanner.model.json_serialization.VertexSetJSONSerializer;
import org.opentripplanner.model.json_serialization.WithGraph;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class WrappedVertex {

    public Vertex vertex;

    public WrappedVertex() {
    }

    public WrappedVertex(Vertex vertex) {
        this.vertex = vertex;
    }
    
    public WithGraph withGraph(Graph graph) {
        return new WrappedVertexWithGraph(graph, this);
    }

    @JsonSerialize(using=VertexSetJSONSerializer.class)
    class WrappedVertexWithGraph extends WithGraph {
        WrappedVertexWithGraph(Graph graph, WrappedVertex vertex) {
            super(graph, vertex);
        }
    }
}

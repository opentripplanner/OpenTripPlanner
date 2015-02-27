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

package org.opentripplanner.graph_builder.module;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check the every vertex and edge in the graph to make sure the edge lists and from/to
 * members are coherent, and that there are no edgeless vertices. Primarily intended for debugging.
 */
public class GraphCoherencyCheckerModule implements GraphBuilderModule {


    /** An set of ids which identifies what stages this graph builder provides (i.e. streets, elevation, transit) */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(GraphCoherencyCheckerModule.class);

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        boolean coherent = true;
        LOG.info("checking graph coherency...");
        for (Vertex v : graph.getVertices()) {
            if (v.getOutgoing().isEmpty() && v.getIncoming().isEmpty()) {
                LOG.error("vertex {} has no edges", v);
                coherent = false;
            }
            for (Edge e : v.getOutgoing()) {
                if (e.getFromVertex() != v) {
                    LOG.error("outgoing edge of {}: from vertex {} does not match", v, e);
                    coherent = false;
                }
                if (e.getToVertex() == null) {
                    LOG.error("outgoing edge has no to vertex {}", e);
                    coherent = false;
                }
            }
            for (Edge e : v.getIncoming()) {
                if (e.getFromVertex() == null) {
                    LOG.error("incoming edge has no from vertex {}", e);
                    coherent = false;
                }
                if (e.getToVertex() != v) {
                    LOG.error("incoming edge of {}: to vertex {} does not match", v, e);
                    coherent = false;
                }
            }
        }
        LOG.info("edge lists and from/to members are {}coherent.", coherent ? "": "not ");
    }

    @Override
    public void checkInputs() {
        //No inputs other than the graph itself
    }
    
}

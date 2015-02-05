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

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.graph_builder.annotation.BikeParkUnlinked;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mabu
 */
public class BikeParkLinkerGraphBuilder implements GraphBuilder {

    private static Logger LOG = LoggerFactory.getLogger(BikeParkLinkerGraphBuilder.class);

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        //TODO: copy only BikeParkVertex vertices

        // iterate over a copy of vertex list because it will be modified
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        vertices.addAll(graph.getVertices());

        NetworkLinkerLibrary networkLinkerLibrary = (NetworkLinkerLibrary) extra.get(NetworkLinkerLibrary.class);
        LOG.info("Linking bike P+R stations...");
        for (BikeParkVertex bprv : Iterables.filter(vertices, BikeParkVertex.class)) {
            if (!networkLinkerLibrary.connectVertexToStreets(bprv).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeParkUnlinked(bprv)));
            }
        }
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("bike_parks_linker");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "network_linking");
    }

    @Override
    public void checkInputs() {
    }
}

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

package org.opentripplanner.graph_builder.impl;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilder} plugin that links up the stops of a transit network to a street network.
 * Should be called after both the transit network and street network are loaded.
 */
public class TransitToStreetNetworkGraphBuilderImpl implements GraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(TransitToStreetNetworkGraphBuilderImpl.class);

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("network_linking", "streets"); // why not "transit" ?
    }

    /**
     * Link the transit network to the street network. Connect each transit vertex to the nearest
     * Street edge with a StreetTransitLink.
     */
    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Linking transit stops to streets...");
        NetworkLinkerLibrary networkLinkerLibrary = (NetworkLinkerLibrary) extra.get(NetworkLinkerLibrary.class);
         LOG.debug("creating linkages...");
        // iterate over a copy of vertex list because it will be modified
        ArrayList<Vertex> vertices = new ArrayList<Vertex>();
        vertices.addAll(graph.getVertices());

        int nUnlinked = 0;
        for (TransitStop ts : Iterables.filter(vertices, TransitStop.class)) {
            // if the street is already linked there is no need to linked it again,
            // could happened if using the prune isolated island
            boolean alreadyLinked = false;
            for(Edge e:ts.getOutgoing()){
                if(e instanceof StreetTransitLink) {
                    alreadyLinked = true;
                    break;
                }
            }
            if(alreadyLinked) continue;
            // only connect transit stops that (a) are entrances, or (b) have no associated
            // entrances
            if (ts.isEntrance() || !ts.hasEntrances()) {
                boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                if (!networkLinkerLibrary.connectVertexToStreets(ts, wheelchairAccessible).getResult()) {
                    LOG.debug(graph.addBuilderAnnotation(new StopUnlinked(ts)));
                    nUnlinked += 1;
                }
            }
        }
        if (nUnlinked > 0) {
            LOG.warn("{} transit stops were not close enough to the street network to be connected to it.", nUnlinked);
        }
    }

    @Override
    public void checkInputs() {
        //no inputs
    }
}

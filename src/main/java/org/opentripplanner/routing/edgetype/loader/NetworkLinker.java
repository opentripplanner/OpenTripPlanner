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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.google.common.collect.Iterables;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.BikeParkUnlinked;
import org.opentripplanner.graph_builder.annotation.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkLinker {

    private static Logger LOG = LoggerFactory.getLogger(NetworkLinker.class);

    private Graph graph;

    private NetworkLinkerLibrary networkLinkerLibrary;

    public NetworkLinker(Graph graph, HashMap<Class<?>,Object> extra) {
        this.graph = graph;
        this.networkLinkerLibrary = new NetworkLinkerLibrary(graph, extra);
        networkLinkerLibrary.options = new RoutingRequest(TraverseMode.BICYCLE);
    }

    public NetworkLinker(Graph graph) {
        // we should be using Collections.emptyMap(), but it breaks Java's broken-ass type checker
        this(graph, new HashMap<Class<?>, Object>());
    }

    /**
     * Link the transit network to the street network. Connect each transit vertex to the nearest
     * Street edge with a StreetTransitLink.
     */
    public void createLinkage() {

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
        //remove replaced edges
        for (HashSet<StreetEdge> toRemove : networkLinkerLibrary.replacements.keySet()) {
            for (StreetEdge edge : toRemove) {
                edge.getFromVertex().removeOutgoing(edge);
                edge.getToVertex().removeIncoming(edge);
            }
        }
        //and add back in replacements
        for (LinkedList<P2<StreetEdge>> toAdd : networkLinkerLibrary.replacements.values()) {
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

        /*
         * TODO Those two steps should be in a separate builder, really. We re-use this builder to
         * prevent having to spatially re-index several times the street network. Instead we could
         * have a "spatial indexer" builder that add a spatial index to the graph, and make all
         * builders that rely on spatial indexing to add a dependency to this builder. And we do not
         * link stations directly in the OSM build as they can come from other builders (static bike
         * rental or P+R builders) and street data can be coming from shapefiles.
         */
        LOG.debug("Linking bike rental stations...");
        for (BikeRentalStationVertex brsv : Iterables.filter(vertices,
                BikeRentalStationVertex.class)) {
            if (!networkLinkerLibrary.connectVertexToStreets(brsv).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeRentalStationUnlinked(brsv)));
            }
        }

        LOG.debug("Linking bike P+R stations...");
        for (BikeParkVertex bprv : Iterables.filter(vertices, BikeParkVertex.class)) {
            if (!networkLinkerLibrary.connectVertexToStreets(bprv).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeParkUnlinked(bprv)));
            }
        }
    }
}

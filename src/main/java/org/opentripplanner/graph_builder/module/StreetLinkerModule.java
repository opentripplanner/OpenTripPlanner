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

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.annotation.BikeParkUnlinked;
import org.opentripplanner.graph_builder.annotation.BikeRentalStationUnlinked;
import org.opentripplanner.graph_builder.annotation.StopUnlinked;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.loader.NetworkLinkerLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that links various objects
 * in the graph to the street network. It should be run after both the transit network and street network are loaded.
 * It links three things: transit stops, bike rental stations, and park-and-ride lots. Therefore it should be run
 * even when there's no GTFS data present to make bike rental services and parking lots usable.
 */
public class StreetLinkerModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(StreetLinkerModule.class);

    private NetworkLinkerLibrary networkLinkerLibrary;

    private Graph graph;

    private ArrayList<TransitStop> stopVertices;

    private ArrayList<BikeRentalStationVertex> bikeRentalVertices;

    private ArrayList<BikeParkVertex> bikeParkVertices;

    public List<String> provides() {
        return Arrays.asList("street to transit", "linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets"); // why not "transit" ?
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        this.networkLinkerLibrary = new NetworkLinkerLibrary(graph, extra);
        this.graph = graph;
        if(graph.hasStreets) {
            //Copies vertices of specific type to specific lists
            //because vertices will be modified
            stopVertices = new ArrayList<>();
            bikeRentalVertices = new ArrayList<>();
            bikeParkVertices = new ArrayList<>();
            for (Vertex v: graph.getVertices()) {
                if (v instanceof TransitStop) {
                    stopVertices.add((TransitStop)v);
                } else if (v instanceof BikeRentalStationVertex) {
                    bikeRentalVertices.add((BikeRentalStationVertex)v);
                } else if (v instanceof BikeParkVertex) {
                    bikeParkVertices.add((BikeParkVertex)v);
                }
            }
            if (graph.hasTransit) {
                linkTransit();
            }
            linkBikeRentalStations();
            linkParkRideStations();
            cleanGraph();
        }
    }

    /**
     * Links transit stops to Streets
     */
    private void linkTransit() {
        LOG.info("Linking transit stops to streets...");
        int nUnlinked = 0;
        for (TransitStop ts : stopVertices) {
            // There are two stop-to-street linkers in OTP. One using tagged stops, and this one, which uses geometry and heuristics.
            // If this stop was already linked using the "tagged stop" hints from OSM, there is no need to link it again.
            // This could happen if using the "prune isolated islands" <-- TODO clarify this last line of comment
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

    /*
     * We re-use this builder to prevent having to spatially re-index several times the street network,
     * and to group together all the linking operations which are very similar (all use the same linker function).
     * Alternatively we could add a spatial index to the graph right after the OSM module, and make all
     * builders that rely on spatial indexing have a dependency on that spatial index builder. And we do not
     * link stations directly in the OSM build as they can come from other builders (static bike
     * rental or P+R builders) and street data can be coming from shapefiles.
     */

    /**
     * Links Bike rental vertices to streets
     */
    private void linkBikeRentalStations() {
        LOG.info("Linking bike rental stations...");
        for (BikeRentalStationVertex brsv : bikeRentalVertices) {
            if (!networkLinkerLibrary.connectVertexToStreets(brsv).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeRentalStationUnlinked(brsv)));
            }
        }
    }

    /**
     * Links P+R stations to streets
     */
    private void linkParkRideStations() {
        LOG.info("Linking bike P+R stations...");
        for (BikeParkVertex bprv : bikeParkVertices) {
            if (!networkLinkerLibrary.connectVertexToStreets(bprv).getResult()) {
                LOG.warn(graph.addBuilderAnnotation(new BikeParkUnlinked(bprv)));
            }
        }
    }

    /**
     * Removes split edges and replaced it with split edges which were split
     * when linking transit/bike rent/P+R
     */
    private void cleanGraph() {
        // 1. Remove replaced edges
        // Leave in the edges that were split. Splitting edges seems to change their traversal times so it
        // introduces artificial differences between graphs in Analyst comparisons. We can re-activate edge
        // removal once we have found a solution to that problem.
//        for (HashSet<StreetEdge> toRemove : networkLinkerLibrary.getReplacements().keySet()) {
//            for (StreetEdge edge : toRemove) {
//                edge.getFromVertex().removeOutgoing(edge);
//                edge.getToVertex().removeIncoming(edge);
//            }
//        }
        // 2. Add the replacements to the graph
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
    public void checkInputs() {
        //no inputs
    }
}

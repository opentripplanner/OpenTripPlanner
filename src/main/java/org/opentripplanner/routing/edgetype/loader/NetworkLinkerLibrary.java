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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.extra_graph.EdgesForRoute;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetBikeParkLink;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkLinkerLibrary {

    private static Logger LOG = LoggerFactory.getLogger(NetworkLinkerLibrary.class);

    /* for each original bundle of (turn)edges making up a street, a list of 
       edge pairs that will replace it */
    HashMap<HashSet<StreetEdge>, LinkedList<P2<StreetEdge>>> replacements =
        new HashMap<HashSet<StreetEdge>, LinkedList<P2<StreetEdge>>>();
    
    /* a map to track which vertices were associated with each linked vertex, to avoid repeat splitting */
    HashMap<Vertex, Collection<StreetVertex>> splitVertices = 
            new HashMap<Vertex, Collection<StreetVertex>> (); 

    /* by default traverse options allow walking only, which is what we want */
    RoutingRequest options = new RoutingRequest();

    Graph graph;

    StreetVertexIndexServiceImpl index;

    EdgesForRoute edgesForRoute;

    public NetworkLinkerLibrary(Graph graph, Map<Class<?>, Object> extra) {
        this.graph = graph;
        EdgesForRoute edgesForRoute = (EdgesForRoute) extra.get(EdgesForRoute.class);
        this.edgesForRoute = edgesForRoute;
        LOG.debug("constructing index...");
        this.index = new StreetVertexIndexServiceImpl(graph);
    }

    /**
     * The entry point for networklinker to link each transit stop.
     * 
     * @param v
     * @param wheelchairAccessible
     * @return true if the links were successfully added, otherwise false
     */
    public LinkRequest connectVertexToStreets(TransitStop v, boolean wheelchairAccessible) {
        LinkRequest request = new LinkRequest(this);
        request.connectVertexToStreets(v, wheelchairAccessible);
        return request;
    }

    /**
     * The entry point for networklinker to link each bike rental station.
     * 
     * @param v
     */
    public LinkRequest connectVertexToStreets(BikeRentalStationVertex v) {
        LinkRequest request = new LinkRequest(this);
        request.connectVertexToStreets(v, new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.BICYCLE), new LinkRequest.StreetLinkFactory<BikeRentalStationVertex>() {
            @Override
            public Collection<? extends Edge> connect(StreetVertex sv, BikeRentalStationVertex v) {
                return Arrays.asList(new StreetBikeRentalLink(sv, v), new StreetBikeRentalLink(v,
                        sv));
            }
        });
        return request;
    }

   /** 
     * The entry point for networklinker to link each bike park.
     * 
     * @param v
     */
    public LinkRequest connectVertexToStreets(BikeParkVertex v) {
        LinkRequest request = new LinkRequest(this);
        request.connectVertexToStreets(v, new TraverseModeSet(TraverseMode.WALK,
                TraverseMode.BICYCLE), new LinkRequest.StreetLinkFactory<BikeParkVertex>() {
            @Override
            public Collection<? extends Edge> connect(StreetVertex sv, BikeParkVertex v) {
                return Arrays.asList(new StreetBikeParkLink(sv, v), new StreetBikeParkLink(v, sv));
            }
        });
        return request;
    }

    /**
     * Gets read only view of list of edge pairs that are replaced
     *
     * @return
     */
    public Map<HashSet<StreetEdge>, LinkedList<P2<StreetEdge>>> getReplacements() {
        return Collections.unmodifiableMap(replacements);
    }

}

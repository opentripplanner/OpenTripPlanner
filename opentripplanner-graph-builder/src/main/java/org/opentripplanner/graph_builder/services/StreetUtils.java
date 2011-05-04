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

import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreetUtils {

    private static Logger _log = LoggerFactory.getLogger(StreetUtils.class);

    /**
     * Make an ordinary graph into an edge-based graph.
     * @param endpoints 
     * @param coordinateToStreetNames 
     */
    public static void makeEdgeBased(Graph graph, Collection<Vertex> endpoints) {
        
        /* generate turns */

        _log.debug("converting to edge-based graph");
        
        ArrayList<DirectEdge> turns = new ArrayList<DirectEdge>(endpoints.size());
        
        for (Vertex v : endpoints) {
            for (Edge e : graph.getIncoming(v)) {
                PlainStreetEdge pse = (PlainStreetEdge) e;
                boolean replaced = false;
                StreetVertex v1 = getStreetVertexForEdge(graph, pse);
                for (Edge e2 : graph.getOutgoing(v)) {
                    StreetVertex v2 = getStreetVertexForEdge(graph, (PlainStreetEdge) e2);
                    if (v1 != v2 && !v1.getEdgeId().equals(v2.getEdgeId())) {
                        turns.add(new TurnEdge(v1, v2));
                        replaced = true;
                    }
                }
                if (!replaced) {
                    pse.setFromVertex(v1);
                    turns.add(pse);
                }
            }
        }
        /* remove standard graph */

        for (Vertex v : endpoints) {
            graph.removeVertexAndEdges(v);
        }
        /* add turns */
        for (DirectEdge e : turns) {
            graph.addEdge(e);
        }
    }

    private static StreetVertex getStreetVertexForEdge(Graph graph, PlainStreetEdge e) {
        boolean back = e.back;
        
        String id = e.getId();
        Vertex v = graph.getVertex(id + (back ? " back" : ""));
        if (v != null) {
            return (StreetVertex) v;
        }

        StreetVertex newv = new StreetVertex(id, e.getGeometry(), e.getName(), e.getLength(), back);
        newv.setWheelchairAccessible(e.isWheelchairAccessible());
        newv.setBicycleSafetyEffectiveLength(e.getBicycleSafetyEffectiveLength());
        newv.setCrossable(e.isCrossable());
        newv.setPermission(e.getPermission());
        newv.setSlopeOverride(e.getSlopeOverride());
        newv.setElevationProfile(e.getElevationProfile());
        newv.setRoundabout(e.isRoundabout());
        graph.addVertex(newv);
        return newv;
    }
}

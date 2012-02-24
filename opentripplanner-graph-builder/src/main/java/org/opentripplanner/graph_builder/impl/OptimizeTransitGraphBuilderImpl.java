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

import java.util.HashMap;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.BasicTripPattern;
import org.opentripplanner.routing.edgetype.PatternEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Replace BasicTripPatterns with ArrayTripPatterns.
 */
public class OptimizeTransitGraphBuilderImpl implements GraphBuilder {

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        for (Vertex v : graph.getVertices()) {
            for (Edge e: v.getOutgoing()) {
                if (e instanceof PatternEdge) {
                    PatternEdge pe = (PatternEdge) e;
                    TripPattern pattern = pe.getPattern();
                    if (pattern instanceof BasicTripPattern) {
                        pe.setPattern(((BasicTripPattern) pattern).convertToArrayTripPattern());
                    }
                }
            }
        }
    }
}

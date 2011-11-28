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

package org.opentripplanner.graph_builder.impl.reach;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.impl.DistanceLibrary;
import org.opentripplanner.routing.impl.GraphSerializationLibrary;
import org.opentripplanner.routing.reach.EdgeWithReach;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class ReachTest extends TestCase {

    Graph graph;
    
    @Test
    public void testReachComputer() throws Exception {
        graph = new Graph();

        makeGraph(graph);
        TraverseOptions options = new TraverseOptions(TraverseMode.WALK);
        options.walkReluctance = 1;
        options.speed = 1;
        
        ReachComputerGraphBuilderImpl computer = new ReachComputerGraphBuilderImpl();
        computer.epsilon = 500;
        Vertex vertex = graph.getVertex("56th_24th");
        Collection<Vertex> streetVertices = Arrays.asList(vertex);
        
        OverlayGraph ograph = new OverlayGraph(graph);
        computer.partialTreesPhase(ograph, streetVertices,
                options, false);
        
        for (EdgeWithReach e : IterableLibrary.filter(vertex.getOutgoing(), EdgeWithReach.class)) {
            assertTrue(e.getReach() < 400);
        }
        
        List<TraverseOptions> optionlist = Arrays.asList(options);
        ContractionHierarchySet hierarchy = new ContractionHierarchySet(graph, optionlist);
        GraphSerializationLibrary.writeGraph(hierarchy, new File("/tmp/ReachTestGraph.obj"));
    }
    
    void makeGraph(Graph graph) {

        vertex("56th_24th", 47.669457, -122.387577);
        vertex("56th_22nd", 47.669462, -122.384739);
        vertex("56th_20th", 47.669457, -122.382106);

        vertex("market_24th", 47.668690, -122.387577);
        vertex("market_ballard", 47.668683, -122.386096);
        vertex("market_22nd", 47.668686, -122.384749);
        vertex("market_leary", 47.668669, -122.384392);
        vertex("market_russell", 47.668655, -122.382997);
        vertex("market_20th", 47.668684, -122.382117);

        vertex("shilshole_24th", 47.668419, -122.387534);
        vertex("shilshole_22nd", 47.666519, -122.384744);
        vertex("shilshole_vernon", 47.665938, -122.384048);
        vertex("shilshole_20th", 47.664356, -122.382192);

        vertex("ballard_turn", 47.668509, -122.386069);
        vertex("ballard_22nd", 47.667624, -122.384744);
        vertex("ballard_vernon", 47.666422, -122.383158);
        vertex("ballard_20th", 47.665476, -122.382128);

        vertex("leary_vernon", 47.666863, -122.382353);
        vertex("leary_20th", 47.666682, -122.382160);

        vertex("russell_20th", 47.667846, -122.382128);

        edges("56th_24th", "56th_22nd", "56th_20th");

        edges("56th_24th", "market_24th");
        edges("56th_22nd", "market_22nd");
        edges("56th_20th", "market_20th");

        edges("market_24th", "market_ballard", "market_22nd", "market_leary", "market_russell",
                "market_20th");
        edges("market_24th", "shilshole_24th", "shilshole_22nd", "shilshole_vernon",
                "shilshole_20th");
        edges("market_ballard", "ballard_turn", "ballard_22nd", "ballard_vernon", "ballard_20th");
        edges("market_leary", "leary_vernon", "leary_20th");
        edges("market_russell", "russell_20th");

        edges("market_22nd", "ballard_22nd", "shilshole_22nd");
        edges("leary_vernon", "ballard_vernon", "shilshole_vernon");
        edges("market_20th", "russell_20th", "leary_20th", "ballard_20th", "shilshole_20th");

    }
    

    private Vertex vertex(String label, double lat, double lon) {
        Vertex v = new Vertex(label, lat, lon);
        graph.addVertex(v);
        return v;
    }

    private void edges(String... vLabels) {
        for (int i = 0; i < vLabels.length - 1; i++) {
            Vertex vA = graph.getVertex(vLabels[i]);
            Vertex vB = graph.getVertex(vLabels[i + 1]);

            graph.addEdge(vA, vB, edge(vA, vB));
            graph.addEdge(vB, vA, edge(vB, vA));
        }
    }

    private Edge edge(Vertex vA, Vertex vB) {
        Coordinate c1 = vA.getCoordinate();
        Coordinate c2 = vB.getCoordinate();
        
        double length = DistanceLibrary.distance(c1, c2);
        String name = vA.getName() + " - " + vB.getName();
        GeometryFactory gf = new GeometryFactory();
        LineString geometry = gf.createLineString(new Coordinate[] { c1, c2});
        
        return new PlainStreetEdge(vA, vB, geometry, name, length, StreetTraversalPermission.ALL, false);
    }

}

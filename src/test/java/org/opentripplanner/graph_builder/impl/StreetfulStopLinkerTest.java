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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class StreetfulStopLinkerTest {
    @Test
    public final void testStreetfulStopLinker() {
        final double speed = new RoutingRequest().walkSpeed;

        Graph graph = new Graph();

        Stop stopA = new Stop();
        Stop stopB = new Stop();
        Stop stopC = new Stop();
        Stop stopD = new Stop();
        Stop stopE = new Stop();

        stopA.setId(new AgencyAndId("Stop", "A"));
        stopA.setLon(0);
        stopA.setLat(0);
        stopB.setId(new AgencyAndId("Stop", "B"));
        stopB.setLon(0);
        stopB.setLat(3);
        stopC.setId(new AgencyAndId("Stop", "C"));
        stopC.setLon(3);
        stopC.setLat(3);
        stopD.setId(new AgencyAndId("Stop", "D"));
        stopD.setLon(3);
        stopD.setLat(0);
        stopE.setId(new AgencyAndId("Stop", "E"));
        stopE.setLon(2);
        stopE.setLat(2);

        TransitStop transitStopA = new TransitStop(graph, stopA);
        TransitStop transitStopB = new TransitStop(graph, stopB);
        TransitStop transitStopC = new TransitStop(graph, stopC);
        TransitStop transitStopD = new TransitStop(graph, stopD);
        TransitStop transitStopE = new TransitStop(graph, stopE);

        IntersectionVertex intersectionA = new IntersectionVertex(graph, "Intersection A", 1, 1);
        IntersectionVertex intersectionB = new IntersectionVertex(graph, "Intersection B", 1, 2);
        IntersectionVertex intersectionC = new IntersectionVertex(graph, "Intersection C", 2, 2);
        IntersectionVertex intersectionD = new IntersectionVertex(graph, "Intersection D", 2, 1);

        intersectionA.freeFlowing = (true);
        intersectionB.freeFlowing = (true);
        intersectionC.freeFlowing = (true);
        intersectionD.freeFlowing = (true);

        new StreetTransitLink(transitStopA, intersectionA, true);
        new StreetTransitLink(intersectionB, transitStopB, true);
        new StreetTransitLink(intersectionC, transitStopC, true);
        new StreetTransitLink(intersectionD, transitStopD, true);
        new StreetTransitLink(intersectionA, transitStopE, true);

        PackedCoordinateSequence coordinatesAB = new PackedCoordinateSequence.Double(
                new double[]{1, 1, 1, 2}, 2);
        PackedCoordinateSequence coordinatesBC = new PackedCoordinateSequence.Double(
                new double[]{1, 2, 2, 2}, 2);
        PackedCoordinateSequence coordinatesCD = new PackedCoordinateSequence.Double(
                new double[]{2, 2, 2, 1}, 2);
        PackedCoordinateSequence coordinatesAD = new PackedCoordinateSequence.Double(
                new double[]{1, 1, 2, 1}, 2);

        GeometryFactory geometryFactory = new GeometryFactory();

        LineString lineStringAB = new LineString(coordinatesAB, geometryFactory);
        LineString lineStringBC = new LineString(coordinatesBC, geometryFactory);
        LineString lineStringCD = new LineString(coordinatesCD, geometryFactory);
        LineString lineStringAD = new LineString(coordinatesAD, geometryFactory);

        // Powers of 2 avoid complications related to floating point arithmetic
        new StreetEdge(intersectionA, intersectionB, lineStringAB, "Edge AB", 2 * speed,
                StreetTraversalPermission.ALL, false);
        new StreetEdge(intersectionB, intersectionC, lineStringBC, "Edge BC", 4 * speed,
                StreetTraversalPermission.ALL, false);
        new StreetEdge(intersectionC, intersectionD, lineStringCD, "Edge CD", 8 * speed,
                StreetTraversalPermission.ALL, false);
        new StreetEdge(intersectionA, intersectionD, lineStringAD, "Edge AD", 16 * speed,
                StreetTraversalPermission.ALL, false);

        StreetfulStopLinker streetfulStopLinker = new StreetfulStopLinker();

        assertEquals(9, graph.countVertices());
        assertEquals(9, graph.countEdges());

        // The duration of the shortest path (A => E) is 2 seconds
        streetfulStopLinker.maxDuration = 1;
        streetfulStopLinker.buildGraph(graph, null);
        assertEquals(9, graph.countEdges());

        // The duration of the longest path (A => D) is 16 seconds
        streetfulStopLinker.maxDuration = 18;
        streetfulStopLinker.buildGraph(graph, null);
        assertEquals(13, graph.countEdges());
        assertEquals(9, graph.countVertices());

        final double results[] = new double[4];
        for (Edge edge : graph.getEdges()) {
            if (edge instanceof SimpleTransfer) {
                assertEquals(transitStopA, edge.getFromVertex());
                assertNotSame(transitStopA, edge.getToVertex());
                double EPSILON_D = 0.1;
                if (edge.getToVertex().equals(transitStopB)) {
                    LineString lineString = edge.getGeometry();
                    assertEquals(2, lineString.getNumPoints());
                    assertEquals(1.0, lineString.getPointN(0).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(0).getY(), 0.0);
                    assertEquals(1.0, lineString.getPointN(1).getX(), 0.0);
                    assertEquals(2.0, lineString.getPointN(1).getY(), 0.0);
                    results[0] = edge.getDistance();
                }
                if (edge.getToVertex().equals(transitStopC)) {
                    LineString lineString = edge.getGeometry();
                    assertEquals(3, lineString.getNumPoints());
                    assertEquals(1.0, lineString.getPointN(0).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(0).getY(), 0.0);
                    assertEquals(1.0, lineString.getPointN(1).getX(), 0.0);
                    assertEquals(2.0, lineString.getPointN(1).getY(), 0.0);
                    assertEquals(2.0, lineString.getPointN(2).getX(), 0.0);
                    assertEquals(2.0, lineString.getPointN(2).getY(), 0.0);
                    results[1] = edge.getDistance();
                }
                if (edge.getToVertex().equals(transitStopD)) {
                    LineString lineString = edge.getGeometry();
                    assertEquals(4, lineString.getNumPoints());
                    assertEquals(1.0, lineString.getPointN(0).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(0).getY(), 0.0);
                    assertEquals(1.0, lineString.getPointN(1).getX(), 0.0);
                    assertEquals(2.0, lineString.getPointN(1).getY(), 0.0);
                    assertEquals(2.0, lineString.getPointN(2).getX(), 0.0);
                    assertEquals(2.0, lineString.getPointN(2).getY(), 0.0);
                    assertEquals(2.0, lineString.getPointN(3).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(3).getY(), 0.0);
                    results[2] = edge.getDistance();
                }
                if (edge.getToVertex().equals(transitStopE)) {
                    LineString lineString = edge.getGeometry();
                    assertEquals(2, lineString.getNumPoints());
                    assertEquals(1.0, lineString.getPointN(0).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(0).getY(), 0.0);
                    assertEquals(1.0, lineString.getPointN(1).getX(), 0.0);
                    assertEquals(1.0, lineString.getPointN(1).getY(), 0.0);
                    results[3] = edge.getDistance();
                }
            }
        }
        // TODO confirm with author that we are checking these all at the end to make sure each conditional was hit.
        assertEquals(results[0],  2.0 * speed, 0.001); // street length quantum is the millimeter
        assertEquals(results[1],  6.0 * speed, 0.001);
        assertEquals(results[2], 14.0 * speed, 0.001);
        assertEquals(results[3],  0.0 * speed, 0.001);
    }
}

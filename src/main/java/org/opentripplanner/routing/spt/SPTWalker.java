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

package org.opentripplanner.routing.spt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Walk over a SPT tree to geometrically visit all nodes and edge geometry. For each geometry longer
 * than the provided base length d0, split it in several steps of equal length and shorter than d0.
 * For each walk step call the visitor callback.
 * 
 * @author laurent
 */
public class SPTWalker {

    private static final Logger LOG = LoggerFactory.getLogger(SPTWalker.class);

    public static interface SPTVisitor {
        public boolean accept(Edge e);

        /**
         * Note: The same state can be visited several times (from different edges).
         * 
         * @param c
         * @param s0
         * @param s1
         * @param d0 Curvilinear coordinate of c on [s0-s1], in meters
         * @param d1 Curvilinear coordinate of c on [s1-s0], in meters
         */
        public void visit(Coordinate c, State s0, State s1, double d0, double d1);
    }

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private ShortestPathTree spt;

    public SPTWalker(ShortestPathTree spt) {
        this.spt = spt;
    }

    /**
     * Walk over a SPT. Call a visitor for each visited point.
     * 
     * @param spt
     * @return
     */
    public void walk(SPTVisitor visitor, double d0) {
        int nTotal = 0, nSkippedDupEdge = 0, nSkippedNoGeometry = 0;
        Collection<? extends State> allStates = spt.getAllStates();
        Set<Edge> processedEdges = new HashSet<Edge>(allStates.size());
        for (State s0 : allStates) {
            for (Edge e : s0.getVertex().getIncoming()) {
                // Take only street
                if (e != null && visitor.accept(e)) {
                    State s1 = spt.getState(e.getFromVertex());
                    if (s1 == null)
                        continue;
                    if (e.getFromVertex() != null && e.getToVertex() != null) {
                        // Hack alert: e.hashCode() throw NPE
                        if (processedEdges.contains(e)) {
                            nSkippedDupEdge++;
                            continue;
                        }
                        processedEdges.add(e);
                    }
                    Vertex vx0 = s0.getVertex();
                    Vertex vx1 = s1.getVertex();
                    LineString lineString = e.getGeometry();
                    if (lineString == null) {
                        nSkippedNoGeometry++;
                        continue;
                    }

                    // Length of linestring
                    double lineStringLen = distanceLibrary.fastLength(lineString);
                    visitor.visit(vx0.getCoordinate(), s0, s1, 0.0, lineStringLen);
                    visitor.visit(vx1.getCoordinate(), s0, s1, lineStringLen, 0.0);
                    nTotal += 2;
                    Coordinate[] pList = lineString.getCoordinates();
                    boolean reverse = vx1.getCoordinate().equals(pList[0]);
                    // Split the linestring in nSteps
                    if (lineStringLen > d0) {
                        int nSteps = (int) Math.floor(lineStringLen / d0) + 1; // Number of steps
                        double stepLen = lineStringLen / nSteps; // Length of step
                        double startLen = 0; // Distance at start of current seg
                        double curLen = stepLen; // Distance cursor
                        int ns = 1;
                        for (int i = 0; i < pList.length - 1; i++) {
                            Coordinate p0 = pList[i];
                            Coordinate p1 = pList[i + 1];
                            double segLen = distanceLibrary.fastDistance(p0, p1);
                            while (curLen - startLen < segLen) {
                                double k = (curLen - startLen) / segLen;
                                Coordinate p = new Coordinate(p0.x * (1 - k) + p1.x * k, p0.y
                                        * (1 - k) + p1.y * k);
                                visitor.visit(p, reverse ? s1 : s0, reverse ? s0 : s1, curLen,
                                        lineStringLen - curLen);
                                nTotal++;
                                curLen += stepLen;
                                ns++;
                            }
                            startLen += segLen;
                            if (ns >= nSteps)
                                break;
                        }
                    }
                }
            }
        }
        LOG.info("SPTWalker: Generated {} points ({} dup edges, {} no geometry) from {} states.",
                nTotal, nSkippedDupEdge, nSkippedNoGeometry, allStates.size());
    }
}

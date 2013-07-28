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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.LineString;

/**
 * Print statistics on geometry data for a graph (number of geometry, average number of points, size
 * distribution, etc...)
 */
public class GeometryStatisticsGraphBuilderImpl implements GraphBuilder {

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    /**
     * An set of ids which identifies what stages this graph builder provides (i.e. streets,
     * elevation, transit)
     */
    public List<String> provides() {
        return Collections.emptyList();
    }

    /** A list of ids of stages which must be provided before this stage */
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(GeometryStatisticsGraphBuilderImpl.class);

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        int nGeometry = 0;
        int nPoints = 0;
        Map<Integer, AtomicInteger> lenDistribution = new HashMap<Integer, AtomicInteger>();
        Map<Integer, AtomicInteger> nptDistribution = new HashMap<Integer, AtomicInteger>();
        int maxNptCount = 0;
        int maxLenCount = 0;

        for (Edge e : graph.getEdges()) {
            if (e.getGeometry() != null) {
                LineString geometry = e.getGeometry();
                // Number of points
                nGeometry++;
                int n = geometry.getNumPoints();
                nPoints += n;
                AtomicInteger nptCount = nptDistribution.get(n);
                if (nptCount == null) {
                    nptCount = new AtomicInteger(0);
                    nptDistribution.put(n, nptCount);
                }
                if (nptCount.addAndGet(1) > maxNptCount)
                    maxNptCount = nptCount.get();
                // Length
                double lenMeters = distanceLibrary.fastLength(geometry);
                int lenSlot = (int) Math.round(Math.log(lenMeters) * 2);
                AtomicInteger lenCount = lenDistribution.get(lenSlot);
                if (lenCount == null) {
                    lenCount = new AtomicInteger(0);
                    lenDistribution.put(lenSlot, lenCount);
                }
                if (lenCount.addAndGet(1) > maxLenCount)
                    maxLenCount = lenCount.get();
            }
        }
        LOG.info(String.format(
                "Graph contains %d geometries, total %d points, average %.02f points/geometry.",
                nGeometry, nPoints, nPoints * 1.0 / nGeometry));

        LOG.info("Number of geometry per geometry length (log scale):");
        List<Integer> lenSlots = new ArrayList<Integer>(lenDistribution.keySet());
        Collections.sort(lenSlots);
        for (int lenSlot : lenSlots) {
            double minLen = Math.exp(lenSlot / 2.0);
            double maxLen = Math.exp((lenSlot + 1) / 2.0);
            LOG.info(String.format("%9.03f-%9.03f m : %s %d", minLen, maxLen,
                    chart(lenDistribution.get(lenSlot).get(), maxLenCount, 60), lenDistribution
                            .get(lenSlot).get()));
        }

        LOG.info("Number of geometry per number of points (linear scale):");
        List<Integer> nptSlots = new ArrayList<Integer>(nptDistribution.keySet());
        Collections.sort(nptSlots);
        for (int nptSlot : nptSlots) {
            LOG.info(String.format("%d : %s %d", nptSlot,
                    chart(nptDistribution.get(nptSlot).get(), maxNptCount, 60), nptDistribution
                            .get(nptSlot).get()));
        }

    }

    private String chart(int x, int xMax, int len) {
        StringBuffer retval = new StringBuffer();
        for (int i = 0; i < Math.round(x * 1.0 * len / xMax); i++)
            retval.append("*");
        return retval.toString();
    }

    @Override
    public void checkInputs() {
        // no inputs to check
    }
}

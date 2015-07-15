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

import com.google.common.collect.Iterables;
import org.opentripplanner.graph_builder.annotation.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} module that links up the stops of a transit network among themselves. This is necessary for
 * routing in long-distance mode.
 *
 * It will use the street network if OSM data has already been loaded into the graph.
 * Otherwise it will use straight-line distance between stops.
 *
 * TODO make tests for this that are sensitive to the presence of trip patterns
 */
public class DirectTransferGenerator implements GraphBuilderModule {

    private static Logger LOG = LoggerFactory.getLogger(DirectTransferGenerator.class);

    int maxDuration = 60 * 10;

    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("street to transit");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        /* Initialize graph index which is needed by the nearby stop finder. */
        if (graph.index == null) {
            graph.index = new GraphIndex(graph);
        }

        /* The linker will use streets if they are available, or straight-line distance otherwise. */
        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(graph, maxDuration);
        if (nearbyStopFinder.useStreets) {
            LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
        } else {
            LOG.info("Creating direct transfer edges between stops using straight line distance (not streets)...");
        }

        int nTransfersTotal = 0;
        int nLinkableStops = 0;

        for (TransitStop ts0 : Iterables.filter(graph.getVertices(), TransitStop.class)) {

            /* Skip stops that are entrances to stations or whose entrances are coded separately */
            if (!ts0.isStreetLinkable()) continue;
            if (++nLinkableStops % 1000 == 0) {
                LOG.info("Linked {} stops", nLinkableStops);
            }
            LOG.debug("Linking stop '{}' {}", ts0.getStop(), ts0);

            /* Determine the set of stops that are already reachable via other pathways or transfers */
            Set<TransitStop> pathwayDestinations = new HashSet<TransitStop>();
            for (Edge e : ts0.getOutgoing()) {
                if (e instanceof PathwayEdge || e instanceof SimpleTransfer) {
                    if (e.getToVertex() instanceof TransitStop) {
                        TransitStop to = (TransitStop) e.getToVertex();
                        pathwayDestinations.add(to);
                    }
                }
            }

            /* Make transfers to each nearby stop that is the closest stop on some trip pattern. */
            int n = 0;
            for (NearbyStopFinder.StopAtDistance sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0)) {
                /* Skip the origin stop, loop transfers are not needed. */
                if (sd.tstop == ts0 || pathwayDestinations.contains(sd.tstop)) continue;
                new SimpleTransfer(ts0, sd.tstop, sd.dist, sd.geom);
                n += 1;
            }
            LOG.debug("Linked stop {} to {} nearby stops on other patterns.", ts0.getStop(), n);
            if (n == 0) {
                LOG.debug(graph.addBuilderAnnotation(new StopNotLinkedForTransfers(ts0)));
            }
            nTransfersTotal += n;
        }
        LOG.info("Done connecting stops to one another. Created a total of {} transfers from {} stops.", nTransfersTotal, nLinkableStops);
        graph.hasDirectTransfers = true;
    }

    @Override
    public void checkInputs() {
        // No inputs
    }


}

package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import java.util.Optional;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.graph_builder.annotation.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.module.NearbyStopFinder.StopAtDistance;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.WheelchairEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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

    final double radiusMeters;

    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("street to transit");
    }

    public DirectTransferGenerator (double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    @Override
    public void buildGraph(Graph graph, GraphBuilderModuleSummary graphBuilderModuleSummary) {
        // Make sure the graph index has been initialized because it is needed by the nearby stop finder. Don't
        // recalculate the street index because it is only used for finding nearby transit stops which should already
        // have been indexed properly in a previous build module.
        graph.index(false);

        /* The linker will use streets if they are available, or straight-line distance otherwise. */
        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(graph, radiusMeters);
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

            // we build two transfers: one which is wheelchair accessible and one which isn't
            nTransfersTotal += createTransfers(graph, nearbyStopFinder, ts0, pathwayDestinations);
        }
        LOG.info("Done connecting stops to one another. Created a total of {} transfers from {} stops.", nTransfersTotal, nLinkableStops);
        graph.hasDirectTransfers = true;
    }

    /**
     * Create a set of transfers from the input TransitStop to all possible stops nearby. What is considered
     * "nearby" is defined by radiusMeters.
     *
     * These transfers are also added to the graph.
     */
    private int createTransfers(
            Graph graph,
            NearbyStopFinder nearbyStopFinder,
            TransitStop ts0,
            Set<TransitStop> pathwayDestinations
    ) {
        /* Make transfers to each nearby stop that is the closest stop on some trip pattern. */
        int numTransfersCreated = 0;
        for (NearbyStopFinder.StopAtDistance sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0, false)) {
            /* Skip the origin stop, loop transfers are not needed. */
            if (sd.tstop == ts0 || pathwayDestinations.contains(sd.tstop)) continue;

            // we check all the edges in the transfer path if there are wheelchair-inaccessible ones
            boolean isWheelchairAccessible = Optional.ofNullable(sd.edges)
                    .map(List::stream)
                    .orElse(Stream.empty())
                    .filter(e -> e instanceof WheelchairEdge)
                    .map(edge -> (WheelchairEdge) edge)
                    .allMatch(WheelchairEdge::isWheelchairAccessible);

            new SimpleTransfer(ts0, sd.tstop, sd.dist, isWheelchairAccessible, sd.geom, sd.edges);
            numTransfersCreated++;

            // if there is an edge that isn't accessible we generate a second transfer which is
            if(!isWheelchairAccessible) {
                StopAtDistance stop =
                        nearbyStopFinder.calculateStopAtDistance(ts0, sd.tstop, true);
                if(stop != null) {
                    new SimpleTransfer(ts0, stop.tstop, stop.dist, true, stop.geom, stop.edges);
                    numTransfersCreated++;
                }
            }
        }
        LOG.debug("Linked stop {} to {} nearby stops on other patterns.", ts0.getStop(),
                numTransfersCreated
        );
        if (numTransfersCreated == 0) {
            LOG.debug(graph.addBuilderAnnotation(new StopNotLinkedForTransfers(ts0)));
        }
        return numTransfersCreated;
    }

    @Override
    public void checkInputs() {
        // No inputs
    }


}

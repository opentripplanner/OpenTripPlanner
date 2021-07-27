package org.opentripplanner.graph_builder.module;

import com.google.common.collect.Iterables;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} module that links up the stops of a transit
 * network among themselves. This is necessary for routing in long-distance mode.
 *
 * It will use the street network if OSM data has already been loaded into the graph.
 * Otherwise it will use straight-line distance between stops.
 *
 * TODO make tests for this that are sensitive to the presence of trip patterns
 */
public class DirectTransferGenerator implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTransferGenerator.class);

    final double radiusByDurationInSeconds;

    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("street to transit");
    }

    public DirectTransferGenerator (double radiusByDurationInSeconds) {
        this.radiusByDurationInSeconds = radiusByDurationInSeconds;
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        /* Initialize graph index which is needed by the nearby stop finder. */
        if (graph.index == null) {
            graph.index = new GraphIndex(graph);
        }

        /* The linker will use streets if they are available, or straight-line distance otherwise. */
        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(graph, radiusByDurationInSeconds);
        if (nearbyStopFinder.useStreets) {
            LOG.info("Creating direct transfer edges between stops using the street network from OSM...");
        } else {
            LOG.info("Creating direct transfer edges between stops using straight line distance (not streets)...");
        }

        Iterable<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);

        ProgressTracker progress = ProgressTracker.track(
                "Create transfer edges", 1000, Iterables.size(stops)
        );
        int nTransfersTotal = 0;
        int nLinkableStops = 0;

        RoutingRequest streetRequest =
            org.opentripplanner.routing.algorithm.raptor.transit.Transfer.prepareTransferRoutingRequest(new RoutingRequest());

        // This could be multi-threaded, in which case we'd need to be careful about the lifecycle of NearbyStopFinder instances.
        for (TransitStopVertex ts0 : stops) {
            Stop stop = ts0.getStop();
            LOG.debug("Linking stop '{}' {}", stop, ts0);

            /* Make transfers to each nearby stop that is the closest stop on some trip pattern. */
            int n = 0;
            for (NearbyStop sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0, streetRequest, false)) {
                // Skip the origin stop, loop transfers are not needed.
                if (sd.stop == stop) { continue; }
                // Skip stop if ForbiddenTransfers is enabled and transfer is forbidden
                if (OTPFeature.ForbiddenTransfers.isOn() && sd.stop instanceof Stop) {
                  Transfer transfer = graph.getTransferService().findTransfer(stop, (Stop) sd.stop);
                  if (transfer != null && transfer.isForbidden()) { continue; }
                }
                graph.transfersByStop.put(
                    stop,
                    new SimpleTransfer(ts0.getStop(), sd.stop, sd.distance, sd.edges)
                );
                n += 1;
            }
            if (OTPFeature.FlexRouting.isOn()) {
                // This code is for finding transfers from FlexStopLocations to Stops, transfers
                // from Stops to FlexStopLocations and between Stops are already covered above.
                for (NearbyStop sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0,  streetRequest, true)) {
                    // Skip the origin stop, loop transfers are not needed.
                    if (sd.stop == ts0.getStop()) { continue; }
                    if (sd.stop instanceof Stop) { continue; }
                    graph.transfersByStop.put(sd.stop,
                        new SimpleTransfer(sd.stop, ts0.getStop(), sd.distance, sd.edges)
                    );
                    n += 1;
                }
            }
            LOG.debug("Linked stop {} to {} nearby stops on other patterns.", stop, n);
            if (n == 0) {
                issueStore.add(new StopNotLinkedForTransfers(ts0));
            }
            else {
                nLinkableStops++;
            }
            //Keep lambda! A method-ref would causes incorrect class and line number to be logged
            //noinspection Convert2MethodRef
            progress.step(m -> LOG.info(m));
            nTransfersTotal += n;
        }
        LOG.info(progress.completeMessage());
        LOG.info("Done connecting stops to one another. Created a total of {} transfers from {} stops.", nTransfersTotal, nLinkableStops);
        graph.hasDirectTransfers = true;
    }

    @Override
    public void checkInputs() {
        // No inputs
    }


}

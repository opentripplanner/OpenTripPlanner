package org.opentripplanner.graph_builder.module;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} module that links up the stops of a transit
 * network among themselves. This is necessary for routing in long-distance mode.
 *
 * It will use the street network if OSM data has already been loaded into the graph.
 * Otherwise it will use straight-line distance between stops.
 */
public class DirectTransferGenerator implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(DirectTransferGenerator.class);

    final double radiusByDurationInSeconds;

    private final List<RoutingRequest> transferRequests;

    public List<String> provides() {
        return List.of("linking");
    }

    public List<String> getPrerequisites() {
        return List.of("street to transit");
    }

    public DirectTransferGenerator (double radiusByDurationInSeconds, List<RoutingRequest> transferRequests) {
        this.radiusByDurationInSeconds = radiusByDurationInSeconds;
        this.transferRequests = transferRequests;
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

        List<TransitStopVertex> stops = graph.getVerticesOfType(TransitStopVertex.class);

        ProgressTracker progress = ProgressTracker.track(
            "Create transfer edges for stops",
            1000,
            stops.size()
        );

        AtomicInteger nTransfersTotal = new AtomicInteger();
        AtomicInteger nLinkedStops = new AtomicInteger();

        // This is a synchronizedMultimap so that a parallel stream may be used to insert elements.
        var transfersByStop = Multimaps.<StopLocation, PathTransfer>synchronizedMultimap(HashMultimap.create());

        stops.stream().parallel().forEach(ts0 -> {
            /* Make transfers to each nearby stop that has lowest weight on some trip pattern.
             * Use map based on the list of edges, so that only distinct transfers are stored. */
            Map<TransferKey, PathTransfer> distinctTransfers = new HashMap<>();
            Stop stop = ts0.getStop();
            LOG.debug("Linking stop '{}' {}", stop, ts0);

            for (RoutingRequest transferProfile : transferRequests) {
                RoutingRequest streetRequest = Transfer.prepareTransferRoutingRequest(transferProfile);

                for (NearbyStop sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0, streetRequest, false)) {
                    // Skip the origin stop, loop transfers are not needed.
                    if (sd.stop == stop) { continue; }
                    distinctTransfers.put(
                        new TransferKey(stop, sd.stop, sd.edges),
                        new PathTransfer(stop, sd.stop, sd.distance, sd.edges)
                    );
                }
                if (OTPFeature.FlexRouting.isOn()) {
                    // This code is for finding transfers from FlexStopLocations to Stops, transfers
                    // from Stops to FlexStopLocations and between Stops are already covered above.
                    for (NearbyStop sd : nearbyStopFinder.findNearbyStopsConsideringPatterns(ts0, streetRequest,  true)) {
                        // Skip the origin stop, loop transfers are not needed.
                        if (sd.stop == stop) { continue; }
                        if (sd.stop instanceof Stop) { continue; }
                        distinctTransfers.put(
                            new TransferKey(sd.stop, stop, sd.edges),
                            new PathTransfer(sd.stop, stop, sd.distance, sd.edges)
                        );
                    }
                }
            }

            LOG.debug("Linked stop {} with {} transfers to stops with different patterns.", stop, distinctTransfers.size());
            if (distinctTransfers.isEmpty()) {
                issueStore.add(new StopNotLinkedForTransfers(ts0));
            } else {
                distinctTransfers.values()
                        .forEach(transfer -> transfersByStop.put(transfer.from, transfer));
                nLinkedStops.incrementAndGet();
                nTransfersTotal.addAndGet(distinctTransfers.size());
            }

            //Keep lambda! A method-ref would causes incorrect class and line number to be logged
            //noinspection Convert2MethodRef
            progress.step(m -> LOG.info(m));
        });

        graph.transfersByStop.putAll(transfersByStop);

        LOG.info(progress.completeMessage());
        LOG.info("Done connecting stops to one another. Created a total of {} transfers from {} stops.", nTransfersTotal, nLinkedStops);
        graph.hasDirectTransfers = true;
    }

    @Override
    public void checkInputs() {
        // No inputs
    }

    private static class TransferKey {
        private final StopLocation source;
        private final StopLocation target;
        private final List<Edge> edges;

        private TransferKey(StopLocation source, StopLocation target, List<Edge> edges) {
            this.source = source;
            this.target = target;
            this.edges = edges;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            final TransferKey that = (TransferKey) o;
            return source.equals(that.source) && target.equals(that.target) && Objects.equals(edges, that.edges);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target, edges);
        }
    }
}

package org.opentripplanner.graph_builder.module;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.transfer.MinTimeTransfer;
import org.opentripplanner.routing.graph.Graph;

/**
 * Transit data (for example transfers.txt) can describe how long a transfer between two stops can
 * take. The previous graph build step {@link DirectTransferGenerator} calculates the transfers on
 * the OSM street network and calculates a time, too.
 * <p>
 * In this step we take both data sources and combine them: if we already have an OSM-based transfer
 * we use its edges in order to display a nice shape rather than a straight line, but we override
 * with the one from the transit data.
 */
public class ApplyMinTimeTransfers implements GraphBuilderModule {

    // https://en.wikipedia.org/wiki/Preferred_walking_speed
    public final float AVERAGE_WALKING_SPEED = 1.4f;

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        Collection<MinTimeTransfer> minTimeTransfers =
                (Collection<MinTimeTransfer>) extra.getOrDefault(
                        MinTimeTransfer.class,
                        Collections.EMPTY_LIST
                );

        minTimeTransfers.forEach(minTimeTransfer -> {
            var transfers = graph.transfersByStop.get(minTimeTransfer.from);
            var pathTransfers = transfers.stream()
                    .filter(pathTransfer -> pathTransfer.to.getId()
                            .equals(minTimeTransfer.to.getId()));

            // we can have potentially multiple transfers if we compute them for bicycle or wheelchair users, for example,
            // so we use the shortest and assume it's the walking one
            var shortestTransfer =
                    pathTransfers.min(Comparator.comparingDouble(PathTransfer::getDistanceMeters));

            // transfers don't have a fixed time but a distance so the time for traversal depends
            // on the walking speed. here we convert the minimum time back to a distance so that
            // logic still works. this is in line with the GTFS spec with says that the
            // mininum transfer time is for a "typical rider".
            // https://github.com/google/transit/blob/master/gtfs/spec/en/reference.md#transferstxt
            final float meters =
                    minTimeTransfer.minTransferTime.getSeconds() / AVERAGE_WALKING_SPEED;

            shortestTransfer.ifPresent(existingPathTransfer -> {
                var adjustedTransfer = existingPathTransfer.copyWithDistanceMeters(meters);
                transfers.remove(existingPathTransfer);
                transfers.add(adjustedTransfer);
            });

            if (shortestTransfer.isEmpty()) {
                // no path found on OSM network, so we generate one with a straight line instead.
                // this is likely a problem in the OSM data.
                var directLineTransfer =
                        new PathTransfer(minTimeTransfer.from, minTimeTransfer.to, meters, null);
                transfers.add(directLineTransfer);
                issueStore.add(
                        "NoTransferStreetPath",
                        "Transit data contains transfer %s but no path found on street network.",
                        minTimeTransfer
                );
            }

        });

    }

    @Override
    public void checkInputs() {
        // No inputs
    }

}

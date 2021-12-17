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

public class ApplyMinTimeTransfers implements GraphBuilderModule {

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

            final float meters =
                    minTimeTransfer.minTransferTime.getSeconds() / AVERAGE_WALKING_SPEED;

            shortestTransfer.ifPresent(existingPathTransfer -> {
                var adjustedTransfer = existingPathTransfer.copyWithDistanceMeters(meters);
                transfers.remove(existingPathTransfer);
                transfers.add(adjustedTransfer);
            });

            if (shortestTransfer.isEmpty()) {
                var directLineTransfer =
                        new PathTransfer(minTimeTransfer.from, minTimeTransfer.to, meters, null);
                transfers.add(directLineTransfer);
                issueStore.add(
                        "NoTransferPathFound",
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

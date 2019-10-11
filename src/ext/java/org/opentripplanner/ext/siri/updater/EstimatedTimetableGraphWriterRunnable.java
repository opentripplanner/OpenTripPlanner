package org.opentripplanner.ext.siri.updater;

import com.google.common.base.Preconditions;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

import java.util.List;

public class EstimatedTimetableGraphWriterRunnable implements GraphWriterRunnable {
    private static Logger LOG = LoggerFactory.getLogger(EstimatedTimetableGraphWriterRunnable.class);

    /**
     * True iff the list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private final boolean fullDataset;

    private final String feedId;


    /**
     * The list with updates to apply to the graph
     */
    private final List<EstimatedTimetableDeliveryStructure> updates;


    EstimatedTimetableGraphWriterRunnable(
            final boolean fullDataset,
            final String feedId,
            final List<EstimatedTimetableDeliveryStructure> updates
    ) {
        // Preconditions
        Preconditions.checkNotNull(updates);

        // Set fields
        this.fullDataset = fullDataset;
        this.updates = updates;
        this.feedId = feedId;
    }

    @Override
    public void run(Graph graph) {
        SiriTimetableSnapshotSource snapshotSource;
        // TODO OTP2 - This is not thread safe, we should inject the snapshotSource on this class,
        //           - it will work because the snapshotSource is created already.
        snapshotSource = graph.getOrSetupTimetableSnapshotProvider(SiriTimetableSnapshotSource::new);

        if (snapshotSource != null) {
            snapshotSource.applyEstimatedTimetable(graph, feedId, fullDataset, updates);
        } else {
            LOG.error("Could not find realtime data snapshot source in graph."
                    + " The following updates are not applied: {}", updates);
        }
    }
}

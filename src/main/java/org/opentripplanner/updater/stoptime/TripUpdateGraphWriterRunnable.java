package org.opentripplanner.updater.stoptime;

import com.google.common.base.Preconditions;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class TripUpdateGraphWriterRunnable implements GraphWriterRunnable {
    private static Logger LOG = LoggerFactory.getLogger(TripUpdateGraphWriterRunnable.class);

    /**
     * True iff the list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private final boolean fullDataset;
    
    /**
     * The list with updates to apply to the graph
     */
    private final List<TripUpdate> updates;

    private final String feedId;

    TripUpdateGraphWriterRunnable(final boolean fullDataset, final List<TripUpdate> updates, final String feedId) {
        // Preconditions
        Preconditions.checkNotNull(updates);
        Preconditions.checkNotNull(feedId);
        
        // Set fields
        this.fullDataset = fullDataset;
        this.updates = updates;
        this.feedId = feedId;
    }

    @Override
    public void run(Graph graph) {
        // Apply updates to graph using realtime snapshot source. The source is retrieved from the graph using the
        // setup method which return the instance, we do not need to provide any creator because the
        // TimetableSnapshotSource should already be set up
        TimetableSnapshotSource snapshotSource = graph.getOrSetupTimetableSnapshotProvider(null);
        if (snapshotSource != null) {
            snapshotSource.applyTripUpdates(graph, fullDataset, updates, feedId);
        } else {
            LOG.error("Could not find realtime data snapshot source in graph."
                    + " The following updates are not applied: {}", updates);
        }
    }
}

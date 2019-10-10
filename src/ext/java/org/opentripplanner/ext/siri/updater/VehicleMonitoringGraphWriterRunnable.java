package org.opentripplanner.ext.siri.updater;

import com.google.common.base.Preconditions;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;

import java.util.List;

public class VehicleMonitoringGraphWriterRunnable implements GraphWriterRunnable {
    private static Logger LOG = LoggerFactory.getLogger(VehicleMonitoringGraphWriterRunnable.class);

    /**
     * True iff the list with updates represent all updates that are active right now, i.e. all
     * previous updates should be disregarded
     */
    private final boolean fullDataset;

    private final String feedId;

    private SiriTimetableSnapshotSource snapshotSource;

    /**
     * The list with updates to apply to the graph
     */
    private final List<VehicleMonitoringDeliveryStructure> updates;


    public VehicleMonitoringGraphWriterRunnable(final boolean fullDataset, final String feedId, final List<VehicleMonitoringDeliveryStructure> updates) {
        // Preconditions
        Preconditions.checkNotNull(updates);

        // Set fields
        this.fullDataset = fullDataset;
        this.updates = updates;
        this.feedId = feedId;
    }

    @Override
    public void run(Graph graph) {
        // Apply updates to graph using realtime snapshot source

        if (snapshotSource == null) {
            snapshotSource = new SiriTimetableSnapshotSource(graph);
        }
            snapshotSource.applyVehicleMonitoring(graph, feedId, fullDataset, updates);

    }
}

package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This GraphUpdater will make an OTP2 Raptor TransitLayer from a TimetableSnapshot at regular intervals.
 * It only handles updating the Graph's realtimeTransitLayer. The Graph's scheduled transitLayer should already be
 * created during Router startup.
 *
 * This could be created automatically after GraphUpdater.setup() is called on all the updaters, at which point we can
 * see whether graph.timetableSnapshotSource is non-null (which means we have a source of incoming updates to copy).
 * But by creating it explicitly, we can set the polling interval and avoid treating this as a special case.
 */
public class RaptorTransitLayerGraphUpdater implements GraphUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorTransitLayerGraphUpdater.class);

    private static final int DEFAULT_INTERVAL_SECONDS = 40;

    private int updateIntervalSeconds;

    private GraphUpdaterManager updaterManager;

    private Graph graph;

    private int lastSnapshotHashCode = 0;

    @Override
    public void setGraphUpdaterManager (GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup (Graph graph) {
        this.graph = graph;
    }

    @Override
    public void run () {
        // TODO OTP2 - See discussion in PR #2794 (Graph.java@650)
        LOG.info("Periodically making an OTP2 Raptor TransitLayer from the TimetableSnapshot at intervals of {} seconds.",
                updateIntervalSeconds);
        while (true) {
            // Sleep before mapping, waiting for the timetableSnapshotSource to be created by another graph updater.
            try {
                Thread.sleep(1000 * updateIntervalSeconds);
            } catch (InterruptedException e) {
                // Reset interrupted status and exit the loop immediately for shutdown.
                Thread.currentThread().interrupt();
                break;
            }
            final TimetableSnapshot timetableSnapshot = graph.getTimetableSnapshot();
            if (timetableSnapshot != null) {
                // Only build a new layer if we got a different snapshot than last time through the loop.
                if (timetableSnapshot.hashCode() != lastSnapshotHashCode) {
                    lastSnapshotHashCode = timetableSnapshot.hashCode();
                    final TransitLayer realtimeTransitLayer = TransitLayerMapper.map(graph);
                    // Although this only performs one assign, it follows the updater convention of submitting a task.
                    updaterManager.execute(g -> g.realtimeTransitLayer = realtimeTransitLayer);
                }
            }
        }
    }

    @Override
    public void teardown () {
    }

    @Override
    public void configure (Graph graph, JsonNode jsonNode) throws Exception {
        updateIntervalSeconds = jsonNode.get("updateIntervalSeconds").asInt(DEFAULT_INTERVAL_SECONDS);
    }

}

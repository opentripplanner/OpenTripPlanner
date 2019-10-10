package org.opentripplanner.ext.siri.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.JsonConfigurable;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

import java.util.concurrent.ExecutionException;

/**
 * Update OTP stop time tables from some (realtime) source
 *
 * Usage example ('rt' name is an example) in file 'Graph.properties':
 *
 * <pre>
 * rt.type = stop-time-updater
 * rt.frequencySec = 60
 * rt.sourceType = gtfs-http
 * rt.url = http://host.tld/path
 * rt.feedId = TA
 * </pre>
 *
 */
public class SiriETUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriETUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    protected GraphUpdaterManager updaterManager;

    /**
     * Update streamer
     */
    private EstimatedTimetableSource updateSource;

    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Integer logFrequency;

    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Integer maxSnapshotFrequency;

    /**
     * Property to set on the RealtimeDataSnapshotSource
     */
    private Boolean purgeExpiredData;

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    private String feedId;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void configurePolling(Graph graph, JsonNode config) throws Exception {
        // Create update streamer from preferences
        feedId = config.path("feedId").asText("");
        String sourceType = config.path("sourceType").asText();

        updateSource = new SiriETHttpTripUpdateSource();

        // Configure update source
        if (updateSource instanceof JsonConfigurable) {
            ((JsonConfigurable) updateSource).configure(graph, config);
        } else {
            throw new IllegalArgumentException(
                    "Unknown update streamer source type: " + sourceType);
        }

        int logFrequency = config.path("logFrequency").asInt(-1);
        if (logFrequency >= 0) {
            this.logFrequency = logFrequency;
        }
        int maxSnapshotFrequency = config.path("maxSnapshotFrequencyMs").asInt(-1);
        if (maxSnapshotFrequency >= 0) {
            this.maxSnapshotFrequency = maxSnapshotFrequency;
        }
        this.purgeExpiredData = config.path("purgeExpiredData").asBoolean(true);

        blockReadinessUntilInitialized = config.path("blockReadinessUntilInitialized").asBoolean(false);

        LOG.info("Creating stop time updater (SIRI ET) running every {} seconds : {}", pollingPeriodSeconds, updateSource);
    }

    @Override
    public void setup(Graph graph) throws InterruptedException, ExecutionException {
        // Create a realtime data snapshot source and wait for runnable to be executed
        updaterManager.executeBlocking(new GraphWriterRunnable() {
            @Override
            public void run(Graph graph) {
                // Only create a realtime data snapshot source if none exists already
                SiriTimetableSnapshotSource snapshotSource = new SiriTimetableSnapshotSource(graph);

                // Set properties of realtime data snapshot source
                if (logFrequency != null) {
                    snapshotSource.logFrequency = (logFrequency);
                }
                if (maxSnapshotFrequency != null) {
                    snapshotSource.maxSnapshotFrequency = (maxSnapshotFrequency);
                }
                if (purgeExpiredData != null) {
                    snapshotSource.purgeExpiredData = (purgeExpiredData);
                }
            }
        });
    }

    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() throws Exception {
        // Get update lists from update source
        Siri updates = updateSource.getUpdates();
        boolean fullDataset = updateSource.getFullDatasetValueOfLastUpdates();

        if (updates != null && updates.getServiceDelivery().getEstimatedTimetableDeliveries() != null) {
            // Handle trip updates via graph writer runnable
            EstimatedTimetableGraphWriterRunnable runnable =
                    new EstimatedTimetableGraphWriterRunnable(fullDataset, feedId, updates.getServiceDelivery().getEstimatedTimetableDeliveries());
            if (!isReady()) {
                LOG.info("Execute blocking tripupdates");
                updaterManager.executeBlocking(runnable);
            } else {
                updaterManager.execute(runnable);
            }
        }
        if (updates != null &&
                updates.getServiceDelivery() != null &&
                updates.getServiceDelivery().isMoreData() != null &&
                updates.getServiceDelivery().isMoreData()) {
            LOG.info("More data is available - fetching immediately");
            runPolling();
        }
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (updateSource == null) ? "NONE" : updateSource.toString();
        return "Polling SIRI ET updater with update source = " + s;
    }
}

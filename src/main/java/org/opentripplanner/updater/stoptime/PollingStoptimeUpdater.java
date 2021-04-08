package org.opentripplanner.updater.stoptime;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Update OTP stop time tables from some (realtime) source
 *
 * Usage example:
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
public class PollingStoptimeUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(PollingStoptimeUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    private GraphUpdaterManager updaterManager;

    /**
     * Update streamer
     */
    private TripUpdateSource updateSource;

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
    private final Boolean purgeExpiredData;

    /**
     * Feed id that is used for the trip ids in the TripUpdates
     */
    private final String feedId;

    private final boolean fuzzyTripMatching;

    /**
     * Set only if we should attempt to match the trip_id from other data in TripDescriptor
     */
    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    public PollingStoptimeUpdater(PollingStoptimeUpdaterParameters parameters) {
        super(parameters);
        // Create update streamer from preferences
        this.feedId = parameters.getFeedId();
        this.updateSource = createSource(parameters);

        // Configure updater FIXME why are the fields objects instead of primitives? this allows null values...
        int logFrequency = parameters.getLogFrequency();
        if (logFrequency >= 0) {
            this.logFrequency = logFrequency;
        }
        int maxSnapshotFrequency = parameters.getMaxSnapshotFrequencyMs();
        if (maxSnapshotFrequency >= 0) {
            this.maxSnapshotFrequency = maxSnapshotFrequency;
        }
        this.purgeExpiredData = parameters.purgeExpiredData();
        this.fuzzyTripMatching = parameters.fuzzyTripMatching();

        LOG.info("Creating stop time updater running every {} seconds : {}", pollingPeriodSeconds, updateSource);
    }

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    @Override
    public void setup(Graph graph) {
        if (fuzzyTripMatching) {
            this.fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(new RoutingService(graph));
        }

        // Only create a realtime data snapshot source if none exists already
        TimetableSnapshotSource snapshotSource =
            graph.getOrSetupTimetableSnapshotProvider(TimetableSnapshotSource::new);

        // Set properties of realtime data snapshot source
        if (logFrequency != null) {
            snapshotSource.logFrequency = logFrequency;
        }
        if (maxSnapshotFrequency != null) {
            snapshotSource.maxSnapshotFrequency = maxSnapshotFrequency;
        }
        if (purgeExpiredData != null) {
            snapshotSource.purgeExpiredData = purgeExpiredData;
        }
        if (fuzzyTripMatcher != null) {
            snapshotSource.fuzzyTripMatcher = fuzzyTripMatcher;
        }
    }

    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() {
        // Get update lists from update source
        List<TripUpdate> updates = updateSource.getUpdates();
        boolean fullDataset = updateSource.getFullDatasetValueOfLastUpdates();

        if (updates != null) {
            // Handle trip updates via graph writer runnable
            TripUpdateGraphWriterRunnable runnable =
                    new TripUpdateGraphWriterRunnable(fullDataset, updates, feedId);
            updaterManager.execute(runnable);
        }
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (updateSource == null) ? "NONE" : updateSource.toString();
        return "Streaming stoptime updater with update source = " + s;
    }


    private static TripUpdateSource createSource(PollingStoptimeUpdaterParameters parameters) {
        switch (parameters.getSourceType()) {
            case GTFS_RT_HTTP:
                return new GtfsRealtimeHttpTripUpdateSource(parameters.httpSourceParameters());
            case GTFS_RT_FILE:
                return new GtfsRealtimeFileTripUpdateSource(parameters.fileSourceParameters());
        }
        throw new IllegalArgumentException(
            "Unknown update streamer source type: " + parameters.getSourceType()
        );
    }
}

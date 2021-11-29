package org.opentripplanner.ext.siri.updater;

import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.WriteToGraphCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.ServiceDelivery;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.VehicleMonitoringDeliveryStructure;

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
public class SiriVMUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(SiriVMUpdater.class);

    /**
     * Parent update manager. Is used to execute graph writer runnables.
     */
    protected WriteToGraphCallback saveResultOnGraph;

    /**
     * Update streamer
     */
    private final VehicleMonitoringSource updateSource;

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

    /**
     * Set only if we should attempt to match the trip_id from other data in TripDescriptor
     */
    private SiriFuzzyTripMatcher siriFuzzyTripMatcher;

    private final boolean fuzzyTripMatching;

    /**
     * The place where we'll record the incoming realtime timetables to make them available to the router in a thread
     * safe way.
     */
    private SiriTimetableSnapshotSource snapshotSource;

    public SiriVMUpdater(SiriVMUpdaterParameters config) {
        super(config);
        // Create update streamer from preferences
        feedId = config.getFeedId();

        updateSource = new SiriVMHttpTripUpdateSource(config.sourceParameters());

        int logFrequency = config.getLogFrequency();
        if (logFrequency >= 0) {
            this.logFrequency = logFrequency;
        }
        int maxSnapshotFrequency = config.getMaxSnapshotFrequencyMs();
        if (maxSnapshotFrequency >= 0) {
            this.maxSnapshotFrequency = maxSnapshotFrequency;
        }
        this.purgeExpiredData = config.purgeExpiredData();
        this.fuzzyTripMatching = config.fuzzyTripMatching();

        blockReadinessUntilInitialized = config.blockReadinessUntilInitialized();

        LOG.info("Creating stop time updater (SIRI VM) running every {} seconds : {}", pollingPeriodSeconds, updateSource);
    }

    @Override
    public void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph) {
        this.saveResultOnGraph = saveResultOnGraph;
    }

    @Override
    public void setup(Graph graph) {
        if (fuzzyTripMatching) {
            this.siriFuzzyTripMatcher = new SiriFuzzyTripMatcher(new RoutingService(graph));
        }
        // Only create a realtime data snapshot source if none exists already
        // TODO OTP2 - This is thread safe, but only because updater setup methods are called sequentially.
        //           - Ideally we should inject the snapshotSource on this class.
        snapshotSource = graph.getOrSetupTimetableSnapshotProvider(SiriTimetableSnapshotSource::new);

        // Set properties of realtime data snapshot source.
        // TODO OTP2 - this is overwriting these properties if they were specified by other updaters.
        //           - These should not be specified at a per-updater level, but at a per-router level.
        if (logFrequency != null) {
            snapshotSource.logFrequency = logFrequency;
        }
        if (maxSnapshotFrequency != null) {
            snapshotSource.maxSnapshotFrequency = maxSnapshotFrequency;
        }
        if (purgeExpiredData != null) {
            snapshotSource.purgeExpiredData = purgeExpiredData;
        }
        if (siriFuzzyTripMatcher != null) {
            siriFuzzyTripMatcher = new SiriFuzzyTripMatcher(new RoutingService(graph));
        }
    }

    /**
     * Repeatedly makes blocking calls to an UpdateStreamer to retrieve new stop time updates, and
     * applies those updates to the graph.
     */
    @Override
    public void runPolling() {
        boolean moreData = false;
        do {
            // Get update lists from update source
            Siri updates = updateSource.getUpdates();
            if (updates != null) {
                boolean fullDataset = updateSource.getFullDatasetValueOfLastUpdates();
                ServiceDelivery serviceDelivery = updates.getServiceDelivery();
                // Use isTrue in case isMoreData returns null. Mark this updater as primed after last page of updates.
                // Copy moreData into a final primitive, because the object moreData persists across iterations.
                moreData = BooleanUtils.isTrue(serviceDelivery.isMoreData());
                final boolean markPrimed = !moreData;
                List<VehicleMonitoringDeliveryStructure> vmds = serviceDelivery.getVehicleMonitoringDeliveries();
                if (vmds != null) {
                    saveResultOnGraph.execute(graph -> {
                        snapshotSource.applyVehicleMonitoring(graph, feedId, fullDataset, vmds);
                        if (markPrimed) primed = true;
                    });
                }
            }
        } while (moreData);
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        String s = (updateSource == null) ? "NONE" : updateSource.toString();
        return "Polling SIRI VM updater with update source = " + s;
    }

}

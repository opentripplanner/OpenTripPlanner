package org.opentripplanner.updater.alerts;

import java.io.InputStream;

import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.AlertPatchServiceImpl;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

/**
 * GTFS-RT alerts updater
 *
 * Usage example ('myalert' name is an example) in file 'Graph.properties':
 *
 * <pre>
 * myalert.type = real-time-alerts
 * myalert.frequencySec = 60
 * myalert.url = http://host.tld/path
 * myalert.earlyStartSec = 3600
 * myalert.feedId = TA
 * </pre>
 */
public class GtfsRealtimeAlertsUpdater extends PollingGraphUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(GtfsRealtimeAlertsUpdater.class);

    private GraphUpdaterManager updaterManager;

    private Long lastTimestamp = Long.MIN_VALUE;

    private String url;

    private String feedId;

    private GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher;

    private AlertPatchService alertPatchService;

    private long earlyStart;

    private AlertsUpdateHandler updateHandler = null;

    private boolean fuzzyTripMatching;

    @Override
    public void setGraphUpdaterManager(GraphUpdaterManager updaterManager) {
        this.updaterManager = updaterManager;
    }

    public void configure(GtfsRealTimeAlertsUpdaterConfig config) throws Exception {
        super.configure(config);

        String url = config.getUrl();
        if (url == null) {
            throw new IllegalArgumentException("Missing mandatory 'url' parameter");
        }
        this.url = url;
        this.earlyStart = config.getEarlyStartSec();
        this.feedId = config.getFeedId();
        this.fuzzyTripMatching = config.fuzzyTripMatching();

        LOG.info("Creating real-time alert updater running every {} seconds : {}", pollingPeriodSeconds, url);
    }

    @Override
    public void setup(Graph graph) {
        // TODO: add options to choose different patch services
        AlertPatchService alertPatchService = new AlertPatchServiceImpl(graph);
        if (fuzzyTripMatching) {
            this.fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(new RoutingService(graph));
        }
        this.alertPatchService = alertPatchService;
        if (updateHandler == null) {
            updateHandler = new AlertsUpdateHandler();
        }
        updateHandler.setEarlyStart(earlyStart);
        updateHandler.setFeedId(feedId);
        updateHandler.setAlertPatchService(alertPatchService);
        updateHandler.setFuzzyTripMatcher(fuzzyTripMatcher);
    }

    @Override
    protected void runPolling() {
        try {
            InputStream data = HttpUtils.getData(
                    url,
                    "Accept",
                    "application/x-google-protobuf, application/x-protobuf, application/protobuf, application/octet-stream, */*");
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }

            final FeedMessage feed = FeedMessage.PARSER.parseFrom(data);

            long feedTimestamp = feed.getHeader().getTimestamp();
            if (feedTimestamp <= lastTimestamp) {
                LOG.info("Ignoring feed with an old timestamp.");
                return;
            }

            // Handle update in graph writer runnable
            updaterManager.execute(new GraphWriterRunnable() {
                @Override
                public void run(Graph graph) {
                    updateHandler.update(feed);
                }
            });

            lastTimestamp = feedTimestamp;
        } catch (Exception e) {
            LOG.error("Error reading gtfs-realtime feed from " + url, e);
        }
    }

    @Override
    public void teardown() {
    }

    public String toString() {
        return "GtfsRealtimeUpdater(" + url + ")";
    }

    public interface GtfsRealTimeAlertsUpdaterConfig extends PollingGraphUpdaterConfig {
        int getEarlyStartSec();
        String getFeedId();
        boolean fuzzyTripMatching();
    }
}

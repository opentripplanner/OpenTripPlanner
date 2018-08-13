package org.opentripplanner.updater;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements logic that is shared between all polling updaters.
 * Usage example ('polling' name is an example and 'polling-updater' should be the type of a
 * concrete class derived from this abstract class):
 * 
 * <pre>
 * polling.type = polling-updater
 * polling.frequencySec = 60
 * </pre>
 * 
 * @see GraphUpdater
 */
public abstract class PollingGraphUpdater implements GraphUpdater {

    private static Logger LOG = LoggerFactory.getLogger(PollingGraphUpdater.class);

    /**
     * Mirrors GraphUpdater.run method. Only difference is that runPolling will be run multiple
     * times with pauses in between. The length of the pause is defined in the preference
     * frequencySec.
     */
    abstract protected void runPolling() throws Exception;

    /** Mirrors GraphUpdater.configure method. */
    abstract protected void configurePolling(Graph graph, JsonNode config) throws Exception;

    /** How long to wait after polling to poll again. */
    protected Integer pollingPeriodSeconds;

    /** The type name in the preferences JSON. FIXME String type codes seem like a red flag, should probably be removed. */
    private String type;

    @Override
    final public void run() {
        try {
            LOG.info("Polling updater started: {}", this);
            while (true) {
                try {
                    // Run concrete polling graph updater's implementation method.
                    runPolling();
                    if (pollingPeriodSeconds <= 0) {
                        // Non-positive polling period values mean to run the updater only once.
                        LOG.info("As requested in configuration, updater {} has run only once and will now stop.",
                                this.getClass().getSimpleName());
                        break;
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    LOG.error("Error while running polling updater of type {}", type, e);
                    // TODO Should we cancel the task? Or after n consecutive failures? cancel();
                }
                Thread.sleep(pollingPeriodSeconds * 1000);
            }
        } catch (InterruptedException e) {
            // When updater is interrupted
            LOG.error("Polling updater {} was interrupted and is stopping.", this.getClass().getName());
        }
    }

    /** Shared configuration code for all polling graph updaters. */
    @Override
    final public void configure (Graph graph, JsonNode config) throws Exception {
        pollingPeriodSeconds = config.path("frequencySec").asInt(60);
        type = config.path("type").asText("");
        // Additional configuration for the concrete subclass
        configurePolling(graph, config);
    }
}

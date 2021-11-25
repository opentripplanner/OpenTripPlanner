package org.opentripplanner.updater;

import org.opentripplanner.routing.graph.Graph;

/**
 * Interface for graph updaters. Objects that implement this interface should always be configured
 * via PreferencesConfigurable.configure after creating the object. GraphUpdaterConfigurator should
 * take care of that. Beware that updaters run in separate threads at the same time.
 * 
 * The only allowed way to make changes to the graph in an updater is by executing (anonymous)
 * GraphWriterRunnable objects via GraphUpdaterManager.execute.
 * 
 * Example implementations can be found in ExampleGraphUpdater and ExamplePollingGraphUpdater.
 */
public interface GraphUpdater {

    /**
     * Graph updaters must be aware of their manager to be able to execute GraphWriterRunnables.
     * GraphUpdaterConfigurator should take care of calling this function.
     */
    void setGraphUpdaterManager(WriteToGraphCallback saveResultOnGraph);

    /**
     * Here the updater can be initialized. If it throws, the updater won't be started (i.e. the run
     * method won't be called). All updaters' setup methods will be run sequentially in a single-threaded manner
     * before updates begin, in order to avoid concurrent reads/writes.
     */
    void setup(Graph graph) throws Exception;

    /**
     * This method will run in its own thread. It pulls or receives updates and applies them to the graph.
     * It must perform any writes to the graph by passing GraphWriterRunnables to GraphUpdaterManager.execute().
     * This queues up the write operations, ensuring that only one updater performs writes at a time.
     */
    void run() throws Exception;

    /**
     * Here the updater can clean up after itself.
     */
    void teardown();

    /**
     * Allow clients to wait for all realtime data to be loaded before submitting any travel plan requests.
     * This does not block use of the OTP server. The client must voluntarily hit an endpoint and wait for readiness.
     * TODO OTP2 This is really a bit backward. We should just run() the updaters once before scheduling them to poll,
     *           and not bring the router online until they have finished.
     */
    default boolean isPrimed() {
        return true;
    }

    /**
     * This is the updater "type" used in the configuration file. It should ONLY be used
     * to provide human friendly messages while logging and debugging.
     */
    String getConfigRef();
}

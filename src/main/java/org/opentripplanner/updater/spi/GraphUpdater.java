package org.opentripplanner.updater.spi;

/**
 * Interface for classes that fetch or receive information while the OTP instance is running and
 * make changes to the Graph and associated transit data to reflect the current situation. This is
 * typically information about disruptions to service, bicycle or parking availability, etc.
 * <p>
 * Each GraphUpdater implementation will be run in a separate thread, allowing it to make blocking
 * calls to fetch data or even sleep between periodic polling operations without affecting the rest
 * of the OTP instance.
 * <p>
 * GraphUpdater implementations are instantiated by UpdaterConfigurator. Each updater configuration
 * item in the router-config for a ThingUpdater is mapped to a corresponding configuration class
 * ThingUpdaterParameters, which is passed to the ThingUpdater constructor.
 * <p>
 * GraphUpdater implementations are only allowed to make changes to the Graph and related structures
 * by submitting instances implementing GraphWriterRunnable (often anonymous functions) to the
 * Graph writing callback function supplied to them by the GraphUpdaterManager after they're
 * constructed. In this way, changes are queued up by many GraphUpdaters running in parallel on
 * different threads, but are applied sequentially in a single-threaded manner to simplify reasoning
 * about concurrent reads and writes to the Graph.
 */
public interface GraphUpdater {
  /**
   * After a GraphUpdater is instantiated, the GraphUpdaterManager that instantiated it will
   * immediately supply a callback via this method. The GraphUpdater will employ that callback
   * every time it wants to queue up a write modification to the Graph or related data structures.
   */
  void setup(WriteToGraphCallback writeToGraphCallback);

  /**
   * The GraphUpdaterManager will run this method in its own long-running thread. This method then
   * pulls or receives updates and applies them to the graph. It must perform any writes to the
   * graph by passing GraphWriterRunnables to the WriteToGraphCallback, which queues up the write
   * operations, ensuring that only one submitted update performs writes at a time.
   */
  void run() throws Exception;

  /**
   * When the GraphUpdaterManager wants to stop all GraphUpdaters (for example when OTP is shutting
   * down) it will call this method, allowing the GraphUpdater implementation to shut down cleanly
   * and release resources.
   */
  default void teardown() {}

  /**
   * Allow clients to wait for all realtime data to be loaded before submitting any travel plan
   * requests. This does not block use of the OTP server. The client must voluntarily hit an
   * endpoint and wait for readiness.
   * TODO OTP2 This is really a bit backward. We should just run() the updaters once before scheduling them to poll,
   *           and not bring the router online until they have finished.
   */
  default boolean isPrimed() {
    return true;
  }

  /**
   * A GraphUpdater implementation uses this method to report its corresponding value of the "type"
   * field in the configuration file. This value should ONLY be used when providing human-friendly
   * messages while logging and debugging. Association of configuration to particular types is
   * performed by the UpdatersConfig.Type constructor calling factory methods.
   */
  String getConfigRef();
}

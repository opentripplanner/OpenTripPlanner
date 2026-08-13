package org.opentripplanner.updater;

/**
 * The graph should only be modified by a runnable implementing this interface, executed by the
 * GraphUpdaterManager. A few notes: - Don't spend more time in this runnable than necessary, it
 * might block other graph writer runnables. - Be aware that while only one graph writer runnable is
 * running to write to the graph, several request-threads might be reading the graph. - Be sure that
 * the request-threads always see a consistent view of the graph while planning.
 *
 * @param <C> the update context of the updater's write domain — {@link
 *            TransitRealTimeUpdateContext} or {@link StreetRealTimeUpdateContext}. The context
 *            exposes only the data owned by that domain.
 * @see GraphUpdaterManager
 */
public interface GraphWriterRunnable<C> {
  /**
   * This function is executed to modify the graph.
   */
  void run(C context);
}

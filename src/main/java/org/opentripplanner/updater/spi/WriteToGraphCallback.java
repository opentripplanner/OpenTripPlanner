package org.opentripplanner.updater.spi;

import java.util.concurrent.Future;
import org.opentripplanner.updater.GraphWriterRunnable;

public interface WriteToGraphCallback {
  /**
   * This is the method to use to modify the graph from the updaters. The runnables will be
   * scheduled after each other, guaranteeing that only one of these runnables will be active at any
   * time. If a particular GraphUpdater calls this method on more than one GraphWriterRunnable, they
   * should be executed in the same order that GraphUpdater made the calls.
   *
   * @param runnable is a graph writer runnable
   */
  Future<?> execute(GraphWriterRunnable runnable);
}

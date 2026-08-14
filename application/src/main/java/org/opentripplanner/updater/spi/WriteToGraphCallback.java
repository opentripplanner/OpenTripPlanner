package org.opentripplanner.updater.spi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.opentripplanner.updater.GraphWriterRunnable;

/**
 * @param <C> the update context of the updater's write domain, see {@link GraphWriterRunnable}
 */
public interface WriteToGraphCallback<C> {
  /**
   * A no-op callback that does nothing - useful for mocking in tests.
   */
  static <C> WriteToGraphCallback<C> noop() {
    return _ -> CompletableFuture.completedFuture(null);
  }

  /**
   * This is the method to use to modify the graph from the updaters. The runnables will be
   * scheduled after each other, guaranteeing that only one of these runnables will be active at any
   * time. If a particular GraphUpdater calls this method on more than one GraphWriterRunnable, they
   * should be executed in the same order that GraphUpdater made the calls.
   */
  Future<?> execute(GraphWriterRunnable<C> runnable);
}

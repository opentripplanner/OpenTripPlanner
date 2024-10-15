package org.opentripplanner.updater.spi;

import java.util.List;

/**
 * DataSource interface that can be used for different types of realtime updaters. It is assumed
 * that these methods are never called in parallel (i.e. updater doesn't run on multiple threads).
 */
public interface DataSource<T> {
  /**
   * Fetch current data about given type and availability from this source.
   *
   * @return true if this operation may have changed something in the list of types.
   */
  boolean update();

  /**
   * @return a List of all currently known objects. The updater will use this to update the Graph.
   */
  List<T> getUpdates();

  /**
   * @see GraphUpdater#setup
   */
  default void setup() {}
}

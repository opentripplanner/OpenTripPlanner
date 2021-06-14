package org.opentripplanner.updater;

import java.util.List;

/**
 * TODO clarify thread safety.
 * It appears that update() and getStations() are never called simultaneously by different threads, but is not stated.
 */
public interface DataSource<T> {

  /**
   * Fetch current data about given type and availability from this source.
   * @return true if this operation may have changed something in the list of types.
   */
  boolean update();

  /**
   * @return a List of all currently known objects. The updater will use this to update the Graph.
   */
  List<T> getUpdates();

  /**
   * @see org.opentripplanner.updater.GraphUpdater#setup
   */
  default void setup() {}
}

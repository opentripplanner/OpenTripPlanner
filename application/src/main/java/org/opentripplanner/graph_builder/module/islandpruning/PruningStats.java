package org.opentripplanner.graph_builder.module.islandpruning;

/**
 * Mutable counters tracked while {@link IslandPruningModule} processes islands and decides
 * whether to isolate, remove, restrict or convert edges to noThruTraffic.
 */
class PruningStats {

  private int isolated = 0;
  private int removed = 0;
  private int noThru = 0;
  private int restricted = 0;

  void incrementIsolated() {
    isolated++;
  }

  void incrementRemoved() {
    removed++;
  }

  void incrementNoThru() {
    noThru++;
  }

  void incrementRestricted() {
    restricted++;
  }

  int isolated() {
    return isolated;
  }

  int removed() {
    return removed;
  }

  int noThru() {
    return noThru;
  }

  int restricted() {
    return restricted;
  }
}

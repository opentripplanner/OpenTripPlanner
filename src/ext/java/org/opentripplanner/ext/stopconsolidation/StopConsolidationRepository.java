package org.opentripplanner.ext.stopconsolidation;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.stopconsolidation.model.ConsolidatedStopGroup;

/**
 * A writeable repository which contains the source data for the consolidation of stops.
 * This repository is built during graph build and then serialized into the graph.
 */
public interface StopConsolidationRepository extends Serializable {
  /**
   * Add groups to this repository.
   */
  void addGroups(Collection<ConsolidatedStopGroup> group);

  /**
   * Returns the list of consolidated stop groups.
   */
  List<ConsolidatedStopGroup> groups();
}

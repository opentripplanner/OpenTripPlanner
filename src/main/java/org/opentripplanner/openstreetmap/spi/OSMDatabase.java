package org.opentripplanner.openstreetmap.spi;

import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMWay;

public interface OSMDatabase {
  void addNode(OSMNode node);

  void addWay(OSMWay way);

  void addRelation(OSMRelation relation);

  /**
   * Called after the first phase, when all relations are loaded.
   */
  void doneFirstPhaseRelations();

  /**
   * Called after the second phase, when all ways are loaded.
   */
  void doneSecondPhaseWays();

  /**
   * Called after the third and final phase, when all nodes are loaded. After all relations, ways,
   * and nodes are loaded, handle areas.
   */
  void doneThirdPhaseNodes();
}

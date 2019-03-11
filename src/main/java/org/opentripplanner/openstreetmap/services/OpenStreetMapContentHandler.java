package org.opentripplanner.openstreetmap.services;

import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMWay;

/**
 * An interface to process/store parsed OpenStreetMap data.
 *
 * @see org.opentripplanner.openstreetmap.services.OpenStreetMapProvider
 */

public interface OpenStreetMapContentHandler {

  /**
   * Stores a node.
   */
  void addNode(OSMNode node);

  /**
   * Stores a way.
   */
  void addWay(OSMWay way);

  /**
   * Stores a relation.
   */
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
   * Called after the third and final phase, when all nodes are loaded.
   */
  void doneThirdPhaseNodes();
}

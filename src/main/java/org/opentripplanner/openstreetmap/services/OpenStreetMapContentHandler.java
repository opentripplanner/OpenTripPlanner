/* 
 Copyright 2008 Brian Ferris
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
  public void addNode(OSMNode node);

  /**
   * Stores a way.
   */
  public void addWay(OSMWay way);

  /**
   * Stores a relation.
   */
  public void addRelation(OSMRelation relation);

  /**
   * Called after the first phase, when all relations are loaded.
   */
  public void doneFirstPhaseRelations();

  /**
   * Called after the second phase, when all ways are loaded.
   */
  public void doneSecondPhaseWays();

  /**
   * Called after the third and final phase, when all nodes are loaded.
   */
  public void doneThirdPhaseNodes();
}

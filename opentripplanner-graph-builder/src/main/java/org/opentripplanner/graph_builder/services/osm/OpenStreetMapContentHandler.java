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

package org.opentripplanner.graph_builder.services.osm;

import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMWay;

/**
 * An interface to process/store parsed OpenStreetMap data.
 *
 * @see org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider
 */

public interface OpenStreetMapContentHandler {

  /**
   * Notifes the handler to expect the second stage of parsing (ie. nodes).
   */
  public void secondPhase();

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
}

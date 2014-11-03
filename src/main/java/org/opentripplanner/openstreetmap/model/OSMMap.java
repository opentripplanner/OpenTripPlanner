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

package org.opentripplanner.openstreetmap.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;

/**
 * A simplistic implementation of OSMContentHandler that simply stores all of the ways, nodes, and
 * relations in maps keyed by ids. For memory usage reasons, this is only used in tests.
 * 
 */
public class OSMMap implements OpenStreetMapContentHandler {

  private Map<Long, OSMNode> _nodes = new HashMap<Long, OSMNode>();

  private Map<Long, OSMWay> _ways = new HashMap<Long, OSMWay>();

  private Map<Long, OSMRelation> _relations = new HashMap<Long, OSMRelation>();

  public Map<Long, OSMNode> getNodes() {
    return _nodes;
  }

  public OSMNode getNodeForId(long nodeId) {
    return _nodes.get(nodeId);
  }

  public Map<Long, OSMWay> getWays() {
    return _ways;
  }

  public OSMWay getWayForId(long wayId) {
      return _ways.get(wayId);
  }

  public void pruneUnusedNodes() {
    Set<Long> nodes = new HashSet<Long>();
    for (OSMWay way : _ways.values()) {
      for (long id : way.getNodeRefs())
        nodes.add(id);
    }
    _nodes.keySet().retainAll(nodes);
  }

  /****
   * {@link OpenStreetMapContentHandler} Interface
   ****/

  public void addNode(OSMNode node) {
    _nodes.put(node.getId(), node);
  }

  public void addWay(OSMWay way) {
    _ways.put(way.getId(), way);
  }

  public void addRelation(OSMRelation relation) {
    _relations.put(relation.getId(), relation);
  }

  @Override
  public void doneFirstPhaseRelations() {
  }

  @Override
  public void doneSecondPhaseWays() {
  }

  @Override
  public void doneThirdPhaseNodes() {
  }
}

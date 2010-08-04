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

package org.opentripplanner.graph_builder.model.osm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;

public class OSMMap implements OpenStreetMapContentHandler {

  private Map<Integer, OSMNode> _nodes = new HashMap<Integer, OSMNode>();

  private Map<Integer, OSMWay> _ways = new HashMap<Integer, OSMWay>();

  private Map<Integer, OSMRelation> _relations = new HashMap<Integer, OSMRelation>();

  public Map<Integer, OSMNode> getNodes() {
    return _nodes;
  }

  public OSMNode getNodeForId(int nodeId) {
    return _nodes.get(nodeId);
  }

  public Map<Integer, OSMWay> getWays() {
    return _ways;
  }
  
  public OSMWay getWayForId(int wayId) {
      return _ways.get(wayId);
  }

  public void pruneUnusedNodes() {
    Set<Integer> nodes = new HashSet<Integer>();
    for (OSMWay way : _ways.values()) {
      for (int id : way.getNodeRefs())
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

}

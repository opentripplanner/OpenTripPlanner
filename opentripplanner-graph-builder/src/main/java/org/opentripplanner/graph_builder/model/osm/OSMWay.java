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

import java.util.ArrayList;
import java.util.List;

public class OSMWay extends OSMWithTags {

  private List<Integer> _nodes = new ArrayList<Integer>();

  private int id;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void addNodeRef(OSMNodeRef nodeRef) {
    _nodes.add(nodeRef.getRef());
  }

  public List<Integer> getNodeRefs() {
    return _nodes;
  }

  public String toString() {
    return "osm way " + id;
  }
}

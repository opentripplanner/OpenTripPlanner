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

import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMWithTags {

  private List<OSMRelationMember> _members = new ArrayList<OSMRelationMember>();

  public void addMember(OSMRelationMember member) {
    _members.add(member);
  }

  public List<OSMRelationMember> getMembers() {
    return _members;
  }

  public String toString() {
    return "osm relation " + id;
  }
}

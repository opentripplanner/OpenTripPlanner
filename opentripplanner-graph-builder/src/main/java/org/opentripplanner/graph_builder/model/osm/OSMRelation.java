/*
 * Copyright 2008 Brian Ferris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opentripplanner.graph_builder.model.osm;

import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMWithTags {

  private int _id;

  private List<OSMRelationMember> _members = new ArrayList<OSMRelationMember>();

  public int getId() {
    return _id;
  }

  public void setId(int id) {
    _id = id;
  }

  public void addMember(OSMRelationMember member) {
    _members.add(member);
  }

  public List<OSMRelationMember> getMembers() {
    return _members;
  }
}

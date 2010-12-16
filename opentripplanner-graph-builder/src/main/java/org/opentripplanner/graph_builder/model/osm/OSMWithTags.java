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
import java.util.Map;

public class OSMWithTags {

  private Map<String, String> _tags = new HashMap<String, String>();

  public void addTag(OSMTag tag) {
    _tags.put(tag.getK(), tag.getV());
  }
  
  public Map<String,String> getTags() {
    return _tags;
  }

  /** Returns a name-like value for an entity (if one exists). The otp: namespaced
   *  tags are created by {@link org.opentripplanner.graph_builder.impl.osm.OpenStreetMapGraphBuilderImpl#processRelations processRelations}
   */
  public String getAssumedName() {
    if(_tags.containsKey("name"))
      return _tags.get("name");

    if(_tags.containsKey("otp:route_name"))
      return _tags.get("otp:route_name");

    if(_tags.containsKey("otp:gen_name"))
      return _tags.get("otp:gen_name");

    if(_tags.containsKey("otp:route_ref"))
      return _tags.get("otp:route_ref");

    if(_tags.containsKey("ref"))
      return _tags.get("ref");

    return null;
  }
}

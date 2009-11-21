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
}

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

public class OSMTag {

  private String k;
  private String v;

  public String getK() {
    return k;
  }

  public void setK(String k) {
    this.k = k;
  }

  public String getV() {
    return v;
  }

  public void setV(String v) {
    this.v = v;
  }
}

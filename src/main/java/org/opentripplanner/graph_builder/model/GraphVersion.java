/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.graph_builder.model;

import java.io.Serializable;
import java.util.Date;

import org.opentripplanner.common.MavenVersion;

/**
 * Model representing metdata data about the current graph.
 * By default it is just the date the graph was constructed at
 * but it can optionally contain the contents of version.json
 * inserted by dev-ops of graph builder process
 */
public class GraphVersion implements Serializable {

  private Date createdDate;
  private String version;
  private MavenVersion builderVersion;

  public MavenVersion getBuilderVersion() {
    return this.builderVersion;
  }

  public void setBuilderVersion(MavenVersion v) {
    builderVersion = v;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date date) {
    createdDate = date;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String s) {
    version = s;
  }

  public String toString() {
    return "{created=" + createdDate + ", version=" + version + "}";
  }

}

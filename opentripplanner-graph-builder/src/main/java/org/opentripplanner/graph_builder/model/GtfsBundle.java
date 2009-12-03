/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GtfsBundle {

  private File path;

  private String defaultAgencyId;

  private Map<String, String> agencyIdMappings = new HashMap<String, String>();

  public File getPath() {
    return path;
  }

  public void setPath(File path) {
    this.path = path;
  }

  public String getDefaultAgencyId() {
    return defaultAgencyId;
  }

  public void setDefaultAgencyId(String defaultAgencyId) {
    this.defaultAgencyId = defaultAgencyId;
  }

  public Map<String, String> getAgencyIdMappings() {
    return agencyIdMappings;
  }

  public void setAgencyIdMappings(Map<String, String> agencyIdMappings) {
    this.agencyIdMappings = agencyIdMappings;
  }
}

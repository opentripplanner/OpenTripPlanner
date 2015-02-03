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

package org.opentripplanner.graph_builder.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.graph_builder.services.EntityReplacementStrategy;
import org.opentripplanner.gtfs.GtfsLibrary;

public class EntityReplacementStrategyFactory {

  private Map<Class<?>, File> _mappings = new HashMap<Class<?>, File>();

  public void setEntityMappings(Map<Class<?>, File> mappings) {
    _mappings.putAll(mappings);
  }

  public EntityReplacementStrategy create() throws IOException {
    EntityReplacementStrategyImpl impl = new EntityReplacementStrategyImpl();
    for (Map.Entry<Class<?>, File> entry : _mappings.entrySet()) {

      Class<?> entityClass = entry.getKey();
      File file = entry.getValue();

      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line = null;

      while ((line = reader.readLine()) != null) {
        String[] tokens = line.split("\\s+");
        List<AgencyAndId> ids = new ArrayList<AgencyAndId>();
        for (String token : tokens)
          ids.add(GtfsLibrary.convertIdFromString(token));
        for (int i = 1; i < ids.size(); i++)
          impl.addEntityReplacement(entityClass, ids.get(i), ids.get(0));
      }
      reader.close();
    }
    return impl;
  }

}

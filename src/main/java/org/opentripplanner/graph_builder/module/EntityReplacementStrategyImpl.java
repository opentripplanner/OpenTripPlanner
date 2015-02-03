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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.graph_builder.services.EntityReplacementStrategy;

public class EntityReplacementStrategyImpl implements EntityReplacementStrategy {

  private Map<Class<?>, Map<Serializable, Serializable>> _entityReplacement = new HashMap<Class<?>, Map<Serializable, Serializable>>();

  public void addEntityReplacement(Class<?> entityType, Serializable entityId,
      Serializable replacementEntityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null) {
      idMappings = new HashMap<Serializable, Serializable>();
      _entityReplacement.put(entityType, idMappings);
    }
    idMappings.put(entityId, replacementEntityId);
  }

  @Override
  public boolean hasReplacementEntities(Class<?> entityType) {
    return _entityReplacement.containsKey(entityType);
  }

  @Override
  public boolean hasReplacementEntity(Class<?> entityType, Serializable entityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null)
      return false;
    return idMappings.containsKey(entityId);
  }

  @Override
  public Serializable getReplacementEntityId(Class<?> entityType,
      Serializable entityId) {
    Map<Serializable, Serializable> idMappings = _entityReplacement.get(entityType);
    if (idMappings == null)
      return null;
    return idMappings.get(entityId);
  }

}

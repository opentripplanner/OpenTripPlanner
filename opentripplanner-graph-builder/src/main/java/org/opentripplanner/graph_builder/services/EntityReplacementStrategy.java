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

package org.opentripplanner.graph_builder.services;

import java.io.Serializable;

public interface EntityReplacementStrategy {
  
  public boolean hasReplacementEntities(Class<?> entityType);

  public boolean hasReplacementEntity(Class<?> entityType, Serializable id);

  public Serializable getReplacementEntityId(Class<?> entityType,
      Serializable id);
}

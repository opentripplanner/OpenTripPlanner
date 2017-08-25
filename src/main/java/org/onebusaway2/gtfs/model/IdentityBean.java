/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.csv_entities.HasExtensions;
import org.onebusaway.csv_entities.schema.annotations.CsvField;

public abstract class IdentityBean<T extends Serializable> implements
    Serializable, HasExtensions {

  private static final long serialVersionUID = 1L;

  @CsvField(ignore = true)
  private Map<Class<?>, Object> _extensionsByType = null;

  public abstract T getId();

  public abstract void setId(T id);

  @Override
  public void putExtension(Class<?> type, Object extension) {
    if (_extensionsByType == null) {
      _extensionsByType = new HashMap<Class<?>, Object>();
    }
    _extensionsByType.put(type, extension);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <X> X getExtension(Class<X> type) {
    if (_extensionsByType == null) {
      return null;
    }
    return (X) _extensionsByType.get(type);
  }

  /***************************************************************************
   * {@link Object}
   **************************************************************************/

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof IdentityBean<?>)
        || getClass() != obj.getClass())
      return false;
    IdentityBean<?> entity = (IdentityBean<?>) obj;
    return getId().equals(entity.getId());
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }
}

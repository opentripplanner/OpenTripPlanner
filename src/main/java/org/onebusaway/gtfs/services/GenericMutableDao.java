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
package org.onebusaway.gtfs.services;

import java.io.Serializable;

import org.onebusaway.gtfs.model.IdentityBean;

public interface GenericMutableDao extends GenericDao {
  
  public void open();

  public void saveEntity(Object entity);
  
  public void updateEntity(Object entity);
  
  public void saveOrUpdateEntity(Object entity);

  public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(
      T entity);

  public <T> void clearAllEntitiesForType(Class<T> type);
  
  public void flush();
  
  public void close();
}

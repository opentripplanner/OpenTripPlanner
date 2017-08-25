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
package org.onebusaway.gtfs.impl;

import java.io.Serializable;
import java.util.Collection;

import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.services.GenericMutableDao;

/**
 * Support class that provides an implementation of {@link GenericMutableDao}
 * where all methods calls are passed to an underlying wrapped instance of
 * {@link GenericMutableDao}. Useful for when you want to selectively override
 * the behavior of individual {@link GenericMutableDao} methods of an existing
 * instance.
 * 
 * @author bdferris
 * 
 */
public class GenericMutableDaoWrapper implements GenericMutableDao {

  protected GenericMutableDao _source;

  public GenericMutableDaoWrapper(GenericMutableDao source) {
    _source = source;
  }

  @Override
  public <T> void clearAllEntitiesForType(Class<T> type) {
    _source.clearAllEntitiesForType(type);
  }

  @Override
  public void close() {
    _source.close();
  }

  @Override
  public void flush() {
    _source.flush();
  }

  @Override
  public void open() {
    _source.open();
  }

  @Override
  public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(
      T entity) {
    _source.removeEntity(entity);
  }

  @Override
  public void updateEntity(Object entity) {
    _source.updateEntity(entity);
  }

  @Override
  public void saveEntity(Object entity) {
    _source.saveEntity(entity);
  }

  @Override
  public void saveOrUpdateEntity(Object entity) {
    _source.saveOrUpdateEntity(entity);
  }

  @Override
  public <T> Collection<T> getAllEntitiesForType(Class<T> type) {
    return _source.getAllEntitiesForType(type);
  }

  @Override
  public <T> T getEntityForId(Class<T> type, Serializable id) {
    return _source.getEntityForId(type, id);
  }
}

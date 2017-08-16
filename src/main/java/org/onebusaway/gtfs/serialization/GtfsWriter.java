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
package org.onebusaway.gtfs.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.csv_entities.CsvEntityWriter;
import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.gtfs.services.GtfsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtfsWriter extends CsvEntityWriter {

  private final Logger _log = LoggerFactory.getLogger(GtfsWriter.class);

  public static final String KEY_CONTEXT = GtfsWriter.class.getName()
      + ".context";

  private List<Class<?>> _entityClasses = new ArrayList<Class<?>>();

  private Map<Class<?>, Comparator<?>> _entityComparators = new HashMap<Class<?>, Comparator<?>>();

  public GtfsWriter() {

    /**
     * Prep the Entity Schema Factories
     */
    _entityClasses.addAll(GtfsEntitySchemaFactory.getEntityClasses());
    _entityComparators.putAll(GtfsEntitySchemaFactory.getEntityComparators());
    DefaultEntitySchemaFactory schemaFactory = createEntitySchemaFactory();
    setEntitySchemaFactory(schemaFactory);
  }

  public List<Class<?>> getEntityClasses() {
    return _entityClasses;
  }

  public Map<Class<?>, Comparator<?>> getEntityComparators() {
    return _entityComparators;
  }

  public void run(GtfsDao dao) throws IOException {

    List<Class<?>> classes = getEntityClasses();

    for (Class<?> entityClass : classes) {
      _log.info("writing entities: " + entityClass.getName());
      Collection<Object> entities = sortEntities(entityClass,
          dao.getAllEntitiesForType(entityClass));
      excludeOptionalAndMissingFields(entityClass, entities);
      for (Object entity : entities)
        handleEntity(entity);
      flush();
    }

    close();
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> sortEntities(Class<?> entityClass,
      Collection<?> entities) {

    Comparator<Object> comparator = (Comparator<Object>) _entityComparators.get(entityClass);

    if (comparator == null)
      return (Collection<Object>) entities;

    List<Object> sorted = new ArrayList<Object>();
    sorted.addAll(entities);
    Collections.sort(sorted, comparator);
    return sorted;
  }

  /****
   * Protected Methods
   ****/

  protected DefaultEntitySchemaFactory createEntitySchemaFactory() {
    return GtfsEntitySchemaFactory.createEntitySchemaFactory();
  }
}

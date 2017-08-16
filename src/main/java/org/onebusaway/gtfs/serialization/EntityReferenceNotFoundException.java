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

import org.onebusaway.csv_entities.exceptions.CsvEntityException;

/**
 * Indicates that a GTFS entity has a reference to another entity that has not
 * been seen before in the feed and that we don't know how to specify a default
 * agency id for that entity as result.
 * 
 * @author bdferris
 * 
 */
public class EntityReferenceNotFoundException extends CsvEntityException {

  private static final long serialVersionUID = 1L;

  public EntityReferenceNotFoundException(Class<?> entityType, String entityId) {
    super(entityType, "entity reference not found: type="
        + entityType.getName() + " id=" + entityId);
  }
}

/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2011 Google, Inc.
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
package org.onebusaway.gtfs.serialization.mappings;

import java.util.Map;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.schema.AbstractFieldMapping;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.serialization.GtfsReaderContext;

/**
 * {@link FieldMapping} capable of mapping a CSV string entity id to an entity
 * instance, and vice versa. Assumes field entity type subclasses
 * {@link IdentityBean} and the target entity can be found with
 * {@link GtfsReaderContext#getEntity(Class, java.io.Serializable)}.
 * 
 * @author bdferris
 * @see IdentityBean
 * @see GtfsReaderContext#getEntity(Class, java.io.Serializable)
 */
class EntityFieldMappingImpl extends AbstractFieldMapping implements
    ConverterFactory {

  private Class<?> _objFieldType;

  public EntityFieldMappingImpl(Class<?> entityType, String csvFieldName,
      String objFieldName, Class<?> objFieldType, boolean required) {
    super(entityType, csvFieldName, objFieldName, required);
    _objFieldType = objFieldType;
  }

  public void translateFromCSVToObject(CsvEntityContext context,
      Map<String, Object> csvValues, BeanWrapper object) {

    if (isMissingAndOptional(csvValues))
      return;

    Converter converter = create(context);
    String entityId = (String) csvValues.get(_csvFieldName);
    Object entity = converter.convert(_objFieldType, entityId);
    object.setPropertyValue(_objFieldName, entity);
  }

  @SuppressWarnings("unchecked")
  public void translateFromObjectToCSV(CsvEntityContext context,
      BeanWrapper object, Map<String, Object> csvValues) {

    IdentityBean<AgencyAndId> entity = (IdentityBean<AgencyAndId>) object.getPropertyValue(_objFieldName);

    if (isOptional() && entity == null)
      return;

    AgencyAndId id = entity.getId();

    csvValues.put(_csvFieldName, id.getId());
  }

  /****
   * {@link ConverterFactory}
   ****/

  @Override
  public Converter create(CsvEntityContext context) {
    GtfsReaderContext ctx = (GtfsReaderContext) context.get(GtfsReader.KEY_CONTEXT);
    return new ConverterImpl(ctx);
  }

  private class ConverterImpl implements Converter {

    private GtfsReaderContext _context;

    public ConverterImpl(GtfsReaderContext context) {
      _context = context;
    }

    @Override
    public Object convert(@SuppressWarnings("rawtypes") Class type, Object value) {
      if (type == String.class) {
        if (value instanceof String)
          return (String) value;
      } else if (type == _objFieldType) {
        String entityId = value.toString();
        String agencyId = _context.getAgencyForEntity(_objFieldType, entityId);
        AgencyAndId id = new AgencyAndId(agencyId, entityId);
        return _context.getEntity(_objFieldType, id);
      }
      throw new ConversionException("Could not convert " + value + " of type "
          + value.getClass() + " to " + type);
    }
  }
}
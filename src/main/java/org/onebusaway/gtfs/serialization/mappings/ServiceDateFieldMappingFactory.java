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

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.exceptions.InvalidValueEntityException;
import org.onebusaway.csv_entities.schema.AbstractFieldMapping;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.EntitySchemaFactory;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.FieldMappingFactory;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class ServiceDateFieldMappingFactory implements FieldMappingFactory {

  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new FieldMappingImpl(entityType, csvFieldName, objFieldName,
        required);
  }

  private static class FieldMappingImpl extends AbstractFieldMapping implements
      Converter {

    public FieldMappingImpl(Class<?> entityType, String csvFieldName,
        String objFieldName, boolean required) {
      super(entityType, csvFieldName, objFieldName, required);
    }

    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object) {

      if (isMissingAndOptional(csvValues))
        return;

      Object value = csvValues.get(_csvFieldName);

      try {
        ServiceDate date = ServiceDate.parseString(value.toString());
        object.setPropertyValue(_objFieldName, date);
      } catch (ParseException ex) {
        throw new InvalidValueEntityException(_entityType, _csvFieldName,
            value.toString());
      }
    }

    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues) {

      ServiceDate date = (ServiceDate) object.getPropertyValue(_objFieldName);
      String value = date.getAsString();
      csvValues.put(_csvFieldName, value);
    }

    @Override
    public Object convert(@SuppressWarnings("rawtypes")
    Class type, Object value) {
      if (type == ServiceDate.class) {
        try {
          return ServiceDate.parseString(value.toString());
        } catch (ParseException ex) {
          throw new InvalidValueEntityException(_entityType, _csvFieldName,
              value.toString());
        }
      }
      throw new ConversionException("Could not convert " + value + " of type "
          + value.getClass() + " to " + type);
    }
  }

}

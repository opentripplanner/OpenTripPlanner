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

import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.schema.AbstractFieldMapping;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.EntitySchemaFactory;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.FieldMappingFactory;

public class StopTimeFieldMappingFactory implements FieldMappingFactory {

  private static DecimalFormat _format = new DecimalFormat("00");

  private static Pattern _pattern = Pattern.compile("^(-{0,1}\\d+):(\\d{2}):(\\d{2})$");

  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {
    return new StopTimeFieldMapping(entityType, csvFieldName, objFieldName,
        required);
  }

  public static String getSecondsAsString(int t) {
    int seconds = positiveMod(t, 60);
    int hourAndMinutes = (t - seconds) / 60;
    int minutes = positiveMod(hourAndMinutes, 60);
    int hours = (hourAndMinutes - minutes) / 60;

    StringBuilder b = new StringBuilder();
    b.append(_format.format(hours));
    b.append(":");
    b.append(_format.format(minutes));
    b.append(":");
    b.append(_format.format(seconds));
    return b.toString();
  }

  private static final int positiveMod(int value, int modulo) {
    int m = value % modulo;
    if (m < 0) {
      m += modulo;
    }
    return m;
  }

  public static int getStringAsSeconds(String value) {
    Matcher m = _pattern.matcher(value);
    if (!m.matches())
      throw new InvalidStopTimeException(value);
    try {
      int hours = Integer.parseInt(m.group(1));
      int minutes = Integer.parseInt(m.group(2));
      int seconds = Integer.parseInt(m.group(3));

      return seconds + 60 * (minutes + 60 * hours);
    } catch (NumberFormatException ex) {
      throw new InvalidStopTimeException(value);
    }
  }

  private static class StopTimeFieldMapping extends AbstractFieldMapping
      implements Converter {

    public StopTimeFieldMapping(Class<?> entityType, String csvFieldName,
        String objFieldName, boolean required) {
      super(entityType, csvFieldName, objFieldName, required);
    }

    @Override
    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object) {

      if (isMissingAndOptional(csvValues))
        return;

      Object value = csvValues.get(_csvFieldName);
      object.setPropertyValue(_objFieldName, convert(Integer.TYPE, value));
    }

    @Override
    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues) {

      int t = (Integer) object.getPropertyValue(_objFieldName);

      if (t < 0) {
        csvValues.put(_csvFieldName, "");
        return;
      }

      String value = getSecondsAsString(t);
      csvValues.put(_csvFieldName, value);
    }

    @Override
    public Object convert(@SuppressWarnings("rawtypes")
    Class type, Object value) {
      if (type == Integer.class || type == Integer.TYPE) {
        String stringValue = value.toString();
        return getStringAsSeconds(stringValue);
      } else if (type == String.class) {
        return getSecondsAsString(((Integer) value).intValue());
      }
      throw new ConversionException("Could not convert " + value + " of type "
          + value.getClass() + " to " + type);
    }

  }
}

/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
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

import java.util.List;
import java.util.Map;

import org.onebusaway.csv_entities.CsvEntityContext;
import org.onebusaway.csv_entities.exceptions.MissingRequiredFieldException;
import org.onebusaway.csv_entities.schema.AbstractFieldMapping;
import org.onebusaway.csv_entities.schema.BeanWrapper;
import org.onebusaway.csv_entities.schema.EntitySchemaFactory;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.csv_entities.schema.FieldMappingFactory;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.serialization.GtfsReaderContext;

/**
 * Responsible for setting the {@link Route#setAgency(Agency)} from a csv
 * "agency_id" field in "routes.txt" and vice-versa.
 * 
 * @author bdferris
 * @see Route#setAgency(Agency)
 */
public class RouteAgencyFieldMappingFactory implements FieldMappingFactory {

  public FieldMapping createFieldMapping(EntitySchemaFactory schemaFactory,
      Class<?> entityType, String csvFieldName, String objFieldName,
      Class<?> objFieldType, boolean required) {

    return new RouteAgencyFieldMapping(entityType, csvFieldName, objFieldName,
        Agency.class, required);
  }

  private class RouteAgencyFieldMapping extends AbstractFieldMapping {

    public RouteAgencyFieldMapping(Class<?> entityType, String csvFieldName,
        String objFieldName, Class<?> objFieldType, boolean required) {
      super(entityType, csvFieldName, objFieldName, required);
    }

    public void translateFromCSVToObject(CsvEntityContext context,
        Map<String, Object> csvValues, BeanWrapper object) {

      GtfsReaderContext ctx = (GtfsReaderContext) context.get(GtfsReader.KEY_CONTEXT);
      String agencyId = (String) csvValues.get(_csvFieldName);

      Agency agency = null;

      if (isMissing(csvValues)) {
        List<Agency> agencies = ctx.getAgencies();
        if (agencies.isEmpty()) {
          throw new AgencyNotFoundForRouteException(Route.class,
              object.getWrappedInstance(Route.class));
        } else if (agencies.size() > 1) {
          throw new MissingRequiredFieldException(_entityType, _csvFieldName);
        }
        agency = agencies.get(0);
      } else {
        agencyId = ctx.getTranslatedAgencyId(agencyId);

        for (Agency testAgency : ctx.getAgencies()) {
          if (testAgency.getId().equals(agencyId)) {
            agency = testAgency;
            break;
          }
        }
        if (agency == null)
          throw new AgencyNotFoundForRouteException(Route.class,
              object.getWrappedInstance(Route.class), agencyId);
      }

      object.setPropertyValue(_objFieldName, agency);
    }

    public void translateFromObjectToCSV(CsvEntityContext context,
        BeanWrapper object, Map<String, Object> csvValues) {

      Agency agency = (Agency) object.getPropertyValue(_objFieldName);

      if (isOptional() && agency == null)
        return;

      csvValues.put(_csvFieldName, agency.getId());
    }

  }
}

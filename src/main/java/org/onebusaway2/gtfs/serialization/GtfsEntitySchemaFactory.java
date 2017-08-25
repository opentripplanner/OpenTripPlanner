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
package org.onebusaway2.gtfs.serialization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.onebusaway.csv_entities.schema.EntitySchemaFactoryHelper;
import org.onebusaway.csv_entities.schema.beans.CsvEntityMappingBean;
import org.onebusaway2.gtfs.model.Agency;
import org.onebusaway2.gtfs.model.AgencyAndId;
import org.onebusaway2.gtfs.model.FareAttribute;
import org.onebusaway2.gtfs.model.FareRule;
import org.onebusaway2.gtfs.model.FeedInfo;
import org.onebusaway2.gtfs.model.Frequency;
import org.onebusaway2.gtfs.model.IdentityBean;
import org.onebusaway2.gtfs.model.Pathway;
import org.onebusaway2.gtfs.model.Route;
import org.onebusaway2.gtfs.model.ServiceCalendar;
import org.onebusaway2.gtfs.model.ServiceCalendarDate;
import org.onebusaway2.gtfs.model.ShapePoint;
import org.onebusaway2.gtfs.model.Stop;
import org.onebusaway2.gtfs.model.StopTime;
import org.onebusaway2.gtfs.model.Transfer;
import org.onebusaway2.gtfs.model.Trip;
import org.onebusaway2.gtfs.serialization.comparators.ServiceCalendarComparator;
import org.onebusaway2.gtfs.serialization.comparators.ServiceCalendarDateComparator;
import org.onebusaway2.gtfs.serialization.comparators.ShapePointComparator;
import org.onebusaway2.gtfs.serialization.comparators.StopTimeComparator;

public class GtfsEntitySchemaFactory {

  public static List<Class<?>> getEntityClasses() {
    List<Class<?>> entityClasses = new ArrayList<Class<?>>();
    entityClasses.add(FeedInfo.class);
    entityClasses.add(Agency.class);
    entityClasses.add(ShapePoint.class);
    entityClasses.add(Route.class);
    entityClasses.add(Stop.class);
    entityClasses.add(Trip.class);
    entityClasses.add(StopTime.class);
    entityClasses.add(ServiceCalendar.class);
    entityClasses.add(ServiceCalendarDate.class);
    entityClasses.add(FareAttribute.class);
    entityClasses.add(FareRule.class);
    entityClasses.add(Frequency.class);
    entityClasses.add(Pathway.class);
    entityClasses.add(Transfer.class);
    return entityClasses;
  }

  public static Map<Class<?>, Comparator<?>> getEntityComparators() {
    Map<Class<?>, Comparator<?>> comparators = new HashMap<Class<?>, Comparator<?>>();
    comparators.put(Agency.class,
        getComparatorForIdentityBeanType(Agency.class));
    comparators.put(Route.class, getComparatorForIdentityBeanType(Route.class));
    comparators.put(Stop.class, getComparatorForIdentityBeanType(Stop.class));
    comparators.put(Trip.class, getComparatorForIdentityBeanType(Trip.class));
    comparators.put(StopTime.class, new StopTimeComparator());
    comparators.put(ShapePoint.class, new ShapePointComparator());
    comparators.put(ServiceCalendar.class, new ServiceCalendarComparator());
    comparators.put(ServiceCalendarDate.class,
        new ServiceCalendarDateComparator());
    return comparators;
  }

  public static DefaultEntitySchemaFactory createEntitySchemaFactory() {

    DefaultEntitySchemaFactory factory = new DefaultEntitySchemaFactory();
    EntitySchemaFactoryHelper helper = new EntitySchemaFactoryHelper(factory);

    CsvEntityMappingBean agencyId = helper.addEntity(AgencyAndId.class);
    helper.addIgnorableField(agencyId, "agencyId");

    return factory;
  }

  private static <T extends IdentityBean<?>> Comparator<T> getComparatorForIdentityBeanType(
      Class<T> entityType) {
    return new Comparator<T>() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(T o1, T o2) {
        Comparable<Object> a = (Comparable<Object>) o1.getId();
        Comparable<Object> b = (Comparable<Object>) o2.getId();
        return a.compareTo(b);
      }
    };
  }
}

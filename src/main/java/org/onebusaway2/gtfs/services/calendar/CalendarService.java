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
package org.onebusaway.gtfs.services.calendar;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.model.calendar.ServiceIdIntervals;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;

/**
 * While the set of {@link ServiceCalendar} and {@link ServiceCalendarDate}
 * entities for a given GTFS feed compactly represent the dates of service for a
 * particular service id, they are not particularly amenable to quick
 * calculations.
 * 
 * The {@link CalendarService} abstracts common date operations into a service
 * interface. The service is typically backed by an efficient data structure
 * that has scanned all the {@link ServiceCalendar} and
 * {@link ServiceCalendarDate} entities into an appropriate representation.
 * 
 * Many of the methods in {@link CalendarService} refer to "service dates". A
 * service date is a particular date when a particular service id is active. The
 * service date is represented by a {@link ServiceDate} object, which is a
 * locale-independent way of representing the year-month-date for a particular
 * service date.
 * 
 * A service date can be localized to a particular timezone (as specified for a
 * particular {@link Agency}) by calling the
 * {@link ServiceDate#getAsDate(java.util.TimeZone)} method to return an actual
 * {@link Date} object. Typically, the date is specified as midnight at the
 * start of the day in the specified timezone for the specified service date,
 * such that adding the arrival or departure time in seconds (as specified in a
 * {@link StopTime}) will return the actual arrival or departure time in UTC.
 * Note that on some days, like daylight saving time days, the time may not
 * actually be at midnight in order the make the {@link StopTime} calculation
 * correct, per the GTFS spec.
 * 
 * In many {@link CalendarService} methods, we refer to a
 * {@link LocalizedServiceId}, which is a service id that also includes timezone
 * information, such that localized service dates can be returned by a method.
 * It's important to note that a single timezone can't be attached to a
 * particular service id, since agencies with potentially different timezones
 * can refer to the same service id in a given GTFS feed.
 * 
 * @author bdferris
 */
public interface CalendarService {

  /**
   * @return the set of all service ids used in the data set
   */
  public Set<AgencyAndId> getServiceIds();

  /**
   * @param serviceId the target service id
   * @return the set of all service dates for which the specified service id is
   *         active
   */
  public Set<ServiceDate> getServiceDatesForServiceId(AgencyAndId serviceId);

  /**
   * Determine the set of service ids that are active on the specified service
   * date.
   * 
   * @param date the target service date
   * @return the set of service ids that are active on the specified service
   *         date
   */
  public Set<AgencyAndId> getServiceIdsOnDate(ServiceDate date);

  /**
   * Returns the instantiated {@link TimeZone} for the specified agency id
   * 
   * @param agencyId {@link Agency#getId()}
   * @return the time zone for the specified agency, or null if the agency was
   *         not found
   */
  public TimeZone getTimeZoneForAgencyId(String agencyId);

  /**
   * Given an agency id and a service id, we return a {@link LocalizedServiceId}
   * which is just a service id with timezone information attached. We use the
   * agencyId argument of the method call to lookup the timezone for requested
   * agency.
   * 
   * Note that the service id itself has an agencyId component as well, but we
   * don't use that for the timezone lookup. The service id's agencyId is more
   * of a prefix to guarantee dataset uniqueness. For example, multiple
   * {@link Trip} objects, representing different agencies, can all reference
   * the same service id. Its important that we look up agency timezone
   * information using the trip's agencyId and not the serviceId's agencyId.
   * 
   * @param agencyId the id of the Agency whose timezone info we will use
   * @param serviceId the service id to use
   * @return a localized service id with timezone info attached, or nul if the
   *         specified agency could not be found
   */
  public LocalizedServiceId getLocalizedServiceIdForAgencyAndServiceId(
      String agencyId, AgencyAndId serviceId);
  
  public List<Date> getDatesForLocalizedServiceId(LocalizedServiceId localizedServiceId);

  public boolean isLocalizedServiceIdActiveOnDate(
      LocalizedServiceId localizedServiceId, Date serviceDate);

  /**
   * Given the specified localized service id, which has a corresponding set of
   * localized service dates, determine the sublist of service dates that, when
   * extended with the specified {@link ServiceInterval}, overlap with the
   * specified from-to time range.
   * 
   * Let's consider a few concrete examples to describe what this method does.
   * 
   * Consider a single {@link StopTime} with an arrival and departure time. That
   * StopTime is part of a {@link Trip}, which means it has a service id
   * attached to it. We wish to answer the question, What service dates would
   * make the specified StopTime active in a particular time range? To answer
   * that question, we'd need to consider each of the localized service dates
   * for which the specified service id is active, add the StopTime arrival and
   * departure offset to each, and see if the resulting time falls within the
   * target range. This method can perform that comparison. By specifying the
   * target service id, a {@link ServiceInterval} whose min and max arrival and
   * departure times are set to the arrival and departure time of the
   * {@link StopTime}, and the target from-to time interval, the method will
   * return the set of localized service dates that would make the StopTime
   * arrival or departure times active in the specified target time range.
   * 
   * As slightly different example, consider answering that same question for
   * multiple {@link StopTime} objects, such as all the StopTimes for a
   * particular trip or all the StopTimes for a particular stop. The question is
   * the same, but the difference is now there are multiple arrival and
   * departure times. Here, an appropriate {@link ServiceInterval} can be
   * constructed that includes the min and max arrival and departure times for
   * the set of {@link StopTime} objects and the calculation will be done
   * looking for service dates that cause any part of the service interval to
   * overlap with the target from-to range.
   * 
   * Note that this method considers both the arrival and departure times when
   * looking for overlaps. To consider only arrival times, check out
   * {@link #getServiceDateArrivalsWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * and for departures, check out
   * {@link #getServiceDateDeparturesWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * 
   * @param serviceId the localized service id whose service dates we'll
   *          consider
   * @param interval a range of arrival and departure times to consider
   * @param from - the min portion of the target time interval
   * @param to - the max portion of the target time interval
   * @return the list of localized service dates that would make the specified
   *         service interval overlap with the specified from-to time range
   */
  public List<Date> getServiceDatesWithinRange(LocalizedServiceId serviceId,
      ServiceInterval interval, Date from, Date to);

  /**
   * See the description from
   * {@link #getServiceDatesWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * . This method does the same thing, except that it works with multiple
   * service ids and intervals at the same time.
   * 
   * @param serviceIdIntervals - a set of service ids and service intervals
   * @param from - time interval min
   * @param to - time interval max
   * @return the list of active service dates, keyed by service id
   */
  public Map<LocalizedServiceId, List<Date>> getServiceDatesWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to);

  /**
   * See the description from
   * {@link #getServiceDatesWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * . This method does the same thing, except that it only considers departure
   * times.
   * 
   * @param serviceId the localized service id whose service dates we'll
   *          consider
   * @param interval a range of departure times to consider
   * @param from - the min portion of the target time interval
   * @param to - the max portion of the target time interval
   * @return the list of active service dates
   */
  public List<Date> getServiceDateDeparturesWithinRange(
      LocalizedServiceId serviceId, ServiceInterval interval, Date from, Date to);

  /**
   * See the description from
   * {@link #getServiceDateDeparturesWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * . This method does the same thing, except that it works with multiple
   * service ids and intervals at the same time.
   * 
   * @param serviceIdIntervals - a set of service ids and service intervals
   * @param from - time interval min
   * @param to - time interval max
   * @return the list of active service dates, keyed by service id
   */
  public Map<LocalizedServiceId, List<Date>> getServiceDateDeparturesWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to);

  /**
   * See the description from
   * {@link #getServiceDatesWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * . This method does the same thing, except that it only considers arrival
   * times.
   * 
   * @param serviceId the localized service id whose service dates we'll
   *          consider
   * @param interval a range of arrival times to consider
   * @param from - the min portion of the target time interval
   * @param to - the max portion of the target time interval
   * @return the list of active service dates
   */
  public List<Date> getServiceDateArrivalsWithinRange(
      LocalizedServiceId serviceId, ServiceInterval interval, Date from, Date to);

  /**
   * See the description from
   * {@link #getServiceDateArrivalsWithinRange(LocalizedServiceId, ServiceInterval, Date, Date)}
   * . This method does the same thing, except that it works with multiple
   * service ids and intervals at the same time.
   * 
   * @param serviceIdIntervals - a set of service ids and service intervals
   * @param from - time interval min
   * @param to - time interval max
   * @return the list of active service dates, keyed by service id
   */
  public Map<LocalizedServiceId, List<Date>> getServiceDateArrivalsWithinRange(
      ServiceIdIntervals serviceIdIntervals, Date from, Date to);

  /**
   * Computes the list of service dates whose departure service interval (min to
   * max departure time) overlaps the specified target time. If no intervals
   * overlaps, then the next service date whose full service interval comes
   * immediately after (but does overlap) the target time is returned.
   * 
   * This method is useful for finding the next scheduled {@link StopTime} after
   * the target time at a particular stop . By calling this method with the
   * serviceId and service interval for the specified stop, we return the set of
   * service dates that overlap or the next service date if none do,
   * guaranteeing that at least one service date instantiates a StopTime that
   * occurs after the target time.
   * 
   * @param serviceId the localized service id whose service dates we'll
   *          consider
   * @param interval a range of departure times to consider
   * @param targetTime - time in UTC
   * @return the set of overlapping and next service dates
   */
  public List<Date> getNextDepartureServiceDates(LocalizedServiceId serviceId,
      ServiceInterval interval, long targetTime);

  /**
   * See the description for
   * {@link #getNextDepartureServiceDates(LocalizedServiceId, ServiceInterval, long)}
   * . This method does the same thing, except that it works with multiple
   * service ids and intervals at the same time.
   * 
   * @param serviceIdIntervals - set of service ids and service intervals
   * @param targetTime
   * @return the set of overlapping and next service dates
   */
  public Map<LocalizedServiceId, List<Date>> getNextDepartureServiceDates(
      ServiceIdIntervals serviceIdIntervals, long targetTime);

  /**
   * Computes the list of service dates whose arrival service interval (min to
   * max departure time) overlaps the specified target time. If not intervals
   * overlap, then the previous service date whose full service interval comes
   * immediately before (but does overlap) the target time is returned.
   * 
   * This method is useful for finding the previous scheduled {@link StopTime}
   * before the target time at a particular stop . By calling this method with
   * the serviceId and service interval for the specified stop, we return the
   * set of service dates that potentially overlap or the previous service date
   * if none do, guaranteeing that at least one service date instantiates a
   * StopTime that occurs before the target time.
   * 
   * @param serviceId the localized service id whose service dates we'll
   *          consider
   * @param interval a range of arrival times to consider
   * @param targetTime - time in UTC
   * @return the set of overlapping and previous service dates
   */
  public List<Date> getPreviousArrivalServiceDates(
      LocalizedServiceId serviceId, ServiceInterval interval, long targetTime);

  /**
   * See the description for
   * {@link #getPreviousArrivalServiceDates(LocalizedServiceId, ServiceInterval, long)}
   * . This method does the same thing, except that it works with multiple
   * service ids and intervals at the same time.
   * 
   * @param serviceIdIntervals - set of service ids and service intervals
   * @param targetTime
   * @return the set of overlapping and previous service dates
   */
  public Map<LocalizedServiceId, List<Date>> getPreviousArrivalServiceDates(
      ServiceIdIntervals serviceIdIntervals, long targetTime);
}

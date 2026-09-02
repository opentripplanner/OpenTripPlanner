package org.opentripplanner.transit.model.calendar;

import static java.util.stream.Collectors.groupingBy;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.calendar.build.MultipleCalendarsForServiceIdException;
import org.opentripplanner.transit.model.calendar.build.ServiceCalendar;
import org.opentripplanner.transit.model.calendar.build.ServiceCalendarDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mutable builder used during graph build to accumulate scheduled calendar data - typically one
 * {@link #addCalendars} call per feed - before producing an immutable {@link TripCalendars}.
 * <p>
 * Unlike {@link TripCalendars} itself, this builder is a plain mutable accumulator: it is meant to
 * be queried (see {@link #listServiceIds()}, {@link #listServiceDates}, {@link #startDate()},
 * {@link #endDate()}) while a graph build module is still adding data to it, one feed at a time.
 */
public class TripCalendarsBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(TripCalendarsBuilder.class);

  private final Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId = new HashMap<>();
  private final Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

  /** Use {@link TripCalendars#of()} to create a new instance. */
  TripCalendarsBuilder() {}

  /**
   * Expand the given GTFS/NeTEx calendars and calendar date exceptions into active service dates,
   * and add them to this builder. We perform this calculation in the timezone of the host jvm,
   * which may be different than the timezone of an agency with the specified service id. To my
   * knowledge, the calculation should work the same, which is to say I can't immediately think of
   * any cases where the service dates would be computed incorrectly.
   */
  public TripCalendarsBuilder addCalendars(
    Collection<ServiceCalendarDate> calendarDates,
    Collection<ServiceCalendar> serviceCalendars
  ) {
    Map<FeedScopedId, List<ServiceCalendarDate>> calendarDatesByServiceId = calendarDates
      .stream()
      .collect(groupingBy(ServiceCalendarDate::getServiceId));
    Map<FeedScopedId, List<ServiceCalendar>> calendarsByServiceId = serviceCalendars
      .stream()
      .collect(groupingBy(ServiceCalendar::getServiceId));

    Set<FeedScopedId> serviceIds = new HashSet<>();
    serviceIds.addAll(calendarDatesByServiceId.keySet());
    serviceIds.addAll(calendarsByServiceId.keySet());

    for (FeedScopedId serviceId : serviceIds) {
      Set<LocalDate> activeDates = new HashSet<>();

      ServiceCalendar calendar = findCalendarForServiceId(calendarsByServiceId, serviceId);
      if (calendar != null) {
        addDatesFromCalendar(calendar, activeDates);
      }

      List<ServiceCalendarDate> dates = calendarDatesByServiceId.get(serviceId);
      if (dates != null) {
        for (ServiceCalendarDate cd : dates) {
          addAndRemoveDatesFromCalendarDate(cd, activeDates);
        }
      }

      putServiceDates(serviceId, activeDates);
    }
    return this;
  }

  /**
   * @return all service ids added to this builder so far.
   */
  public Set<FeedScopedId> listServiceIds() {
    return serviceDatesByServiceId.keySet();
  }

  /**
   * @return the service dates registered so far for the given service id, sorted ascending.
   */
  public List<LocalDate> listServiceDates(FeedScopedId serviceId) {
    return serviceDatesByServiceId.getOrDefault(serviceId, List.of());
  }

  /**
   * @return the earliest service date registered so far, across all service ids.
   */
  public Optional<LocalDate> startDate() {
    return serviceIdsByDate.keySet().stream().min(LocalDate::compareTo);
  }

  /**
   * @return the latest service date registered so far, across all service ids.
   */
  public Optional<LocalDate> endDate() {
    return serviceIdsByDate.keySet().stream().max(LocalDate::compareTo);
  }

  /**
   * Register the given (already known) service dates directly for {@code serviceId}, bypassing
   * the calendar/calendar-date expansion done by {@link #addCalendars}.
   */
  public TripCalendarsBuilder putServiceDatesForServiceId(
    FeedScopedId serviceId,
    Collection<LocalDate> dates
  ) {
    putServiceDates(serviceId, dates);
    return this;
  }

  /**
   * Freeze the data accumulated so far into an immutable {@link TripCalendars} snapshot. No
   * service codes are registered yet - see {@link TripCalendars#withServiceCode} and
   * {@link TripCalendars#initializeServiceCodesRunningForDate}.
   */
  public TripCalendars build() {
    return new TripCalendars(serviceDatesByServiceId, serviceIdsByDate);
  }

  private void putServiceDates(FeedScopedId serviceId, Collection<LocalDate> activeDates) {
    List<LocalDate> sortedDates = List.copyOf(new TreeSet<>(activeDates));
    serviceDatesByServiceId.put(serviceId, sortedDates);
    for (LocalDate date : sortedDates) {
      serviceIdsByDate.computeIfAbsent(date, d -> new HashSet<>()).add(serviceId);
    }
  }

  private static ServiceCalendar findCalendarForServiceId(
    Map<FeedScopedId, List<ServiceCalendar>> calendarsByServiceId,
    FeedScopedId serviceId
  ) {
    List<ServiceCalendar> calendars = calendarsByServiceId.get(serviceId);
    if (calendars == null || calendars.isEmpty()) {
      return null;
    }
    if (calendars.size() == 1) {
      return calendars.getFirst();
    }
    throw new MultipleCalendarsForServiceIdException(serviceId);
  }

  private static void addDatesFromCalendar(ServiceCalendar calendar, Set<LocalDate> activeDates) {
    LocalDate startDate = calendar.getPeriod().getStartInclusive();
    LocalDate endDate = calendar.getPeriod().getEndInclusive();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (
        switch (date.getDayOfWeek()) {
          case MONDAY -> calendar.getMonday() == 1;
          case TUESDAY -> calendar.getTuesday() == 1;
          case WEDNESDAY -> calendar.getWednesday() == 1;
          case THURSDAY -> calendar.getThursday() == 1;
          case FRIDAY -> calendar.getFriday() == 1;
          case SATURDAY -> calendar.getSaturday() == 1;
          case SUNDAY -> calendar.getSunday() == 1;
        }
      ) {
        activeDates.add(date);
      }
    }
  }

  private static void addAndRemoveDatesFromCalendarDate(
    ServiceCalendarDate calendarDate,
    Set<LocalDate> activeDates
  ) {
    LocalDate serviceDate = calendarDate.getDate();
    switch (calendarDate.getExceptionType()) {
      case ServiceCalendarDate.EXCEPTION_TYPE_ADD -> activeDates.add(serviceDate);
      case ServiceCalendarDate.EXCEPTION_TYPE_REMOVE -> activeDates.remove(serviceDate);
      default -> LOG.warn(
        "Unknown CalendarDate exception type: {}",
        calendarDate.getExceptionType()
      );
    }
  }
}

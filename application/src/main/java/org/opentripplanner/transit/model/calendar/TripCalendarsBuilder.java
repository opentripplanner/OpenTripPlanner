package org.opentripplanner.transit.model.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;

/**
 * Mutable builder used during graph build to accumulate scheduled calendar data - typically one
 * {@link #addWeeklyCalendar} call per GTFS {@code calendar.txt} row, plus one
 * {@link #addServiceDate}/{@link #removeServiceDate} call per {@code calendar_dates.txt} row (or
 * NeTEx equivalent) - before producing an immutable {@link TripCalendars}.
 * <p>
 * Unlike {@link TripCalendars} itself, this builder is a plain mutable accumulator: it is meant to
 * be queried (see {@link #listServiceIds()}, {@link #listServiceDates}, {@link #startDate()},
 * {@link #endDate()}) while a graph build module is still adding data to it, one feed at a time.
 */
public class TripCalendarsBuilder {

  private final Map<FeedScopedId, WeeklyCalendar> weeklyCalendarsByServiceId = new HashMap<>();
  private final Map<FeedScopedId, Map<LocalDate, Boolean>> exceptionsByServiceId = new HashMap<>();

  private final Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId = new HashMap<>();
  private final Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

  /**
   * Service ids currently represented in {@link #serviceDatesByServiceId} that were derived from
   * {@link #weeklyCalendarsByServiceId}/{@link #exceptionsByServiceId} (as opposed to registered
   * directly via {@link #putServiceDatesForServiceId}). Used by {@link #resolvePendingIfNeeded} to
   * clean up stale entries for a service dropped entirely by {@link #limitToPeriod}.
   */
  private final Set<FeedScopedId> resolvedFromPending = new HashSet<>();

  private boolean dirty = false;

  /** Use {@link TripCalendars#of()} to create a new instance. */
  TripCalendarsBuilder() {}

  /**
   * Register a recurring weekly service pattern for {@code serviceId}: it runs on each of the
   * given days of week within {@code period}. At most one weekly calendar can be registered per
   * service id.
   *
   * @throws MultipleCalendarsForServiceIdException if a weekly calendar was already registered
   * for this service id.
   */
  public TripCalendarsBuilder addWeeklyCalendar(
    FeedScopedId serviceId,
    Set<DayOfWeek> daysOfWeek,
    LocalDateRange period
  ) {
    if (
      weeklyCalendarsByServiceId.putIfAbsent(
        serviceId,
        new WeeklyCalendar(Set.copyOf(daysOfWeek), period)
      ) != null
    ) {
      throw new MultipleCalendarsForServiceIdException(serviceId);
    }
    dirty = true;
    return this;
  }

  /**
   * Register a service date exception: {@code serviceId} runs on {@code date}, in addition to any
   * weekly calendar registered for it.
   */
  public TripCalendarsBuilder addServiceDate(FeedScopedId serviceId, LocalDate date) {
    exceptionsByServiceId.computeIfAbsent(serviceId, id -> new HashMap<>()).put(date, Boolean.TRUE);
    dirty = true;
    return this;
  }

  /**
   * Register a service date exception: {@code serviceId} does not run on {@code date}, overriding
   * any weekly calendar registered for it.
   */
  public TripCalendarsBuilder removeServiceDate(FeedScopedId serviceId, LocalDate date) {
    exceptionsByServiceId
      .computeIfAbsent(serviceId, id -> new HashMap<>())
      .put(date, Boolean.FALSE);
    dirty = true;
    return this;
  }

  /**
   * Register the given (already known) service dates directly for {@code serviceId}, bypassing
   * the weekly-calendar/exception expansion done by {@link #addWeeklyCalendar}.
   */
  public TripCalendarsBuilder putServiceDatesForServiceId(
    FeedScopedId serviceId,
    Collection<LocalDate> dates
  ) {
    putServiceDates(serviceId, dates);
    return this;
  }

  /**
   * Trim every registered weekly calendar and service date exception to {@code period}: weekly
   * calendars whose period does not overlap {@code period} at all are dropped, others have their
   * period intersected with it; exceptions outside {@code period} are dropped. A no-op if
   * {@code period} is unbounded.
   */
  public TripCalendarsBuilder limitToPeriod(LocalDateRange period) {
    if (period.isUnbounded()) {
      return this;
    }
    Iterator<Map.Entry<FeedScopedId, WeeklyCalendar>> weeklyIt = weeklyCalendarsByServiceId
      .entrySet()
      .iterator();
    while (weeklyIt.hasNext()) {
      Map.Entry<FeedScopedId, WeeklyCalendar> entry = weeklyIt.next();
      WeeklyCalendar weekly = entry.getValue();
      if (weekly.period().overlap(period)) {
        entry.setValue(
          new WeeklyCalendar(weekly.daysOfWeek(), weekly.period().intersection(period))
        );
      } else {
        weeklyIt.remove();
      }
    }
    for (Map<LocalDate, Boolean> exceptions : exceptionsByServiceId.values()) {
      exceptions.keySet().removeIf(date -> !period.contains(date));
    }
    exceptionsByServiceId.values().removeIf(Map::isEmpty);
    dirty = true;
    return this;
  }

  /**
   * @return all service ids added to this builder so far.
   */
  public Set<FeedScopedId> listServiceIds() {
    resolvePendingIfNeeded();
    return serviceDatesByServiceId.keySet();
  }

  /**
   * @return the service dates registered so far for the given service id, sorted ascending.
   */
  public List<LocalDate> listServiceDates(FeedScopedId serviceId) {
    resolvePendingIfNeeded();
    return serviceDatesByServiceId.getOrDefault(serviceId, List.of());
  }

  /**
   * @return the earliest service date registered so far, across all service ids.
   */
  public Optional<LocalDate> startDate() {
    resolvePendingIfNeeded();
    return serviceIdsByDate.keySet().stream().min(LocalDate::compareTo);
  }

  /**
   * @return the latest service date registered so far, across all service ids.
   */
  public Optional<LocalDate> endDate() {
    resolvePendingIfNeeded();
    return serviceIdsByDate.keySet().stream().max(LocalDate::compareTo);
  }

  /**
   * Freeze the data accumulated so far into an immutable {@link TripCalendars} snapshot. No
   * service codes are registered yet - see {@link TripCalendars#withServiceCode} and
   * {@link TripCalendars#initializeServiceCodesRunningForDate}.
   */
  public TripCalendars build() {
    resolvePendingIfNeeded();
    return new TripCalendars(serviceDatesByServiceId, serviceIdsByDate);
  }

  private void resolvePendingIfNeeded() {
    if (!dirty) {
      return;
    }
    Set<FeedScopedId> pendingServiceIds = new HashSet<>();
    pendingServiceIds.addAll(weeklyCalendarsByServiceId.keySet());
    pendingServiceIds.addAll(exceptionsByServiceId.keySet());

    // A service previously resolved from pending data (e.g. a weekly calendar since dropped by
    // limitToPeriod) is no longer represented in the pending maps at all - purge its stale entry.
    Set<FeedScopedId> droppedServiceIds = new HashSet<>(resolvedFromPending);
    droppedServiceIds.removeAll(pendingServiceIds);
    for (FeedScopedId serviceId : droppedServiceIds) {
      removeServiceDates(serviceId);
      resolvedFromPending.remove(serviceId);
    }

    for (FeedScopedId serviceId : pendingServiceIds) {
      // Clear any previously resolved dates for this service before recomputing, otherwise dates
      // that no longer apply (e.g. trimmed off by limitToPeriod) would linger in serviceIdsByDate.
      removeServiceDates(serviceId);

      Set<LocalDate> activeDates = new HashSet<>();
      WeeklyCalendar weekly = weeklyCalendarsByServiceId.get(serviceId);
      if (weekly != null) {
        addDatesFromWeeklyCalendar(weekly, activeDates);
      }
      Map<LocalDate, Boolean> exceptions = exceptionsByServiceId.get(serviceId);
      if (exceptions != null) {
        exceptions.forEach((date, added) -> {
          if (added) {
            activeDates.add(date);
          } else {
            activeDates.remove(date);
          }
        });
      }
      putServiceDates(serviceId, activeDates);
      resolvedFromPending.add(serviceId);
    }
    dirty = false;
  }

  private void putServiceDates(FeedScopedId serviceId, Collection<LocalDate> activeDates) {
    List<LocalDate> sortedDates = List.copyOf(new TreeSet<>(activeDates));
    serviceDatesByServiceId.put(serviceId, sortedDates);
    for (LocalDate date : sortedDates) {
      serviceIdsByDate.computeIfAbsent(date, d -> new HashSet<>()).add(serviceId);
    }
  }

  /** Remove any previously resolved dates for {@code serviceId} from both maps. */
  private void removeServiceDates(FeedScopedId serviceId) {
    List<LocalDate> oldDates = serviceDatesByServiceId.remove(serviceId);
    if (oldDates == null) {
      return;
    }
    for (LocalDate date : oldDates) {
      Set<FeedScopedId> ids = serviceIdsByDate.get(date);
      if (ids != null) {
        ids.remove(serviceId);
        if (ids.isEmpty()) {
          serviceIdsByDate.remove(date);
        }
      }
    }
  }

  private static void addDatesFromWeeklyCalendar(
    WeeklyCalendar weekly,
    Set<LocalDate> activeDates
  ) {
    if (weekly.daysOfWeek().isEmpty()) {
      return;
    }
    LocalDate startDate = weekly.period().getStartInclusive();
    LocalDate endDate = weekly.period().getEndInclusive();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      if (weekly.daysOfWeek().contains(date.getDayOfWeek())) {
        activeDates.add(date);
      }
    }
  }

  private record WeeklyCalendar(Set<DayOfWeek> daysOfWeek, LocalDateRange period) {}
}

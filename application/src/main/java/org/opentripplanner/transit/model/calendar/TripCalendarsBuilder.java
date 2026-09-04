package org.opentripplanner.transit.model.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * Every date added is clipped to the {@code periodLimit} given at construction time. A weekly
 * calendar whose period does not overlap {@code periodLimit} at all is dropped entirely; one that
 * partially overlaps has its period intersected with it. A service date exception outside
 * {@code periodLimit} is ignored.
 * <p>
 * Unlike {@link TripCalendars} itself, this builder is a plain mutable accumulator: it is meant to
 * be queried (see {@link #listServiceIds()}, {@link #listServiceDates}) while a graph build module
 * is still adding data to it, one feed at a time.
 */
public class TripCalendarsBuilder {

  private final LocalDateRange periodLimit;

  private final Map<FeedScopedId, WeeklyCalendar> weeklyCalendarsByServiceId = new HashMap<>();
  private final Map<FeedScopedId, Map<LocalDate, Boolean>> exceptionsByServiceId = new HashMap<>();

  private final Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId = new HashMap<>();
  private final Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate = new HashMap<>();

  private boolean dirty = false;

  /** Use {@link TripCalendars#of()}/{@link TripCalendars#of(LocalDateRange)} to create an instance. */
  TripCalendarsBuilder(LocalDateRange periodLimit) {
    this.periodLimit = periodLimit;
  }

  /**
   * Register a recurring weekly service pattern for {@code serviceId}: it runs on each of the
   * given days of week within {@code period}. At most one weekly calendar can be registered per
   * service id, even if it falls entirely outside this builder's period limit.
   *
   * @throws MultipleCalendarsForServiceIdException if a weekly calendar was already registered
   * for this service id.
   */
  public TripCalendarsBuilder addWeeklyCalendar(
    FeedScopedId serviceId,
    Set<DayOfWeek> daysOfWeek,
    LocalDateRange period
  ) {
    if (weeklyCalendarsByServiceId.containsKey(serviceId)) {
      throw new MultipleCalendarsForServiceIdException(serviceId);
    }
    if (period.overlap(periodLimit)) {
      weeklyCalendarsByServiceId.put(
        serviceId,
        new WeeklyCalendar(Set.copyOf(daysOfWeek), period.intersection(periodLimit))
      );
      dirty = true;
    }
    return this;
  }

  /**
   * Register a service date exception: {@code serviceId} runs on {@code date}, in addition to any
   * weekly calendar registered for it. A no-op if {@code date} is {@code null} or outside this
   * builder's period limit.
   */
  public TripCalendarsBuilder addServiceDate(FeedScopedId serviceId, LocalDate date) {
    if (date != null && periodLimit.contains(date)) {
      exceptionsByServiceId
        .computeIfAbsent(serviceId, id -> new HashMap<>())
        .put(date, Boolean.TRUE);
      dirty = true;
    }
    return this;
  }

  /**
   * Register a service date exception: {@code serviceId} does not run on {@code date}, overriding
   * any weekly calendar registered for it. A no-op if {@code date} is {@code null} or outside this
   * builder's period limit.
   */
  public TripCalendarsBuilder removeServiceDate(FeedScopedId serviceId, LocalDate date) {
    if (date != null && periodLimit.contains(date)) {
      exceptionsByServiceId
        .computeIfAbsent(serviceId, id -> new HashMap<>())
        .put(date, Boolean.FALSE);
      dirty = true;
    }
    return this;
  }

  /**
   * Register the given (already known) service dates directly for {@code serviceId}, bypassing
   * the weekly-calendar/exception expansion done by {@link #addWeeklyCalendar}. Dates outside this
   * builder's period limit are dropped; {@code serviceId} stays registered even if that leaves it
   * with zero dates (e.g. NeTEx's empty-calendar placeholder).
   */
  public TripCalendarsBuilder putServiceDatesForServiceId(
    FeedScopedId serviceId,
    Collection<LocalDate> dates
  ) {
    putServiceDates(serviceId, dates.stream().filter(periodLimit::contains).toList());
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

    for (FeedScopedId serviceId : pendingServiceIds) {
      // Clear any previously resolved dates for this service before recomputing, otherwise dates
      // that no longer apply (e.g. a since-overridden exception) would linger in serviceIdsByDate.
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

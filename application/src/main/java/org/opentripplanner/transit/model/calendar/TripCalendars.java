package org.opentripplanner.transit.model.calendar;

import gnu.trove.TCollections;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable snapshot of the trip service calendar: which service ids exist, which dates they run
 * on, and the small integer service codes used by Raptor.
 * <p>
 * There is no builder: every modification (see {@link #merge}, {@link #withServiceCode},
 * {@link #initializeServiceCodesRunningForDate}, {@link #getOrCreateServiceIdForDate}) returns a new instance
 * rather than mutating this one, so callers that hold onto a {@code TripCalendars} keep seeing a
 * stable value even while another reference is evolving. A caller that needs a long-lived,
 * continuously-updated view (e.g. {@code DefaultTimetableRepository}'s write buffer) just holds a
 * plain mutable field and reassigns it as each method returns its result.
 */
public class TripCalendars implements Serializable {

  private static final String CAL_SERVICE_FEED_ID = "CSID";
  private static final Logger LOG = LoggerFactory.getLogger(TripCalendars.class);

  private final Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId;
  private final Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate;
  private final Map<LocalDate, TIntSet> serviceCodesRunningForDate;
  private final Map<FeedScopedId, Integer> serviceCodes;

  @Nullable
  private final LocalDate startDate;

  @Nullable
  private final LocalDate endDate;

  private TripCalendars(
    Map<FeedScopedId, List<LocalDate>> serviceDatesByServiceId,
    Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate,
    Map<LocalDate, TIntSet> serviceCodesRunningForDate,
    Map<FeedScopedId, Integer> serviceCodes
  ) {
    this.serviceDatesByServiceId = serviceDatesByServiceId;
    this.serviceIdsByDate = serviceIdsByDate;
    this.serviceCodesRunningForDate = serviceCodesRunningForDate;
    this.serviceCodes = serviceCodes;
    this.startDate = serviceIdsByDate.keySet().stream().min(LocalDate::compareTo).orElse(null);
    this.endDate = serviceIdsByDate.keySet().stream().max(LocalDate::compareTo).orElse(null);
  }

  /** An empty trip calendar, with no service ids registered. */
  public static TripCalendars empty() {
    // Note: Collections.emptyMap(), not Map.of() — the latter throws NullPointerException on a
    // null probe key (see the constructor's note on withEntry/withAddedServiceId below).
    return new TripCalendars(
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyMap()
    );
  }

  /**
   * @return all service ids used in the data set.
   */
  public Set<FeedScopedId> listServiceIds() {
    return serviceDatesByServiceId.keySet();
  }

  /**
   * @param serviceId the target service id
   * @return the set of all service dates for which the specified service id is active
   */
  public Set<LocalDate> listServiceDates(FeedScopedId serviceId) {
    Set<LocalDate> dates = new HashSet<>();
    List<LocalDate> serviceDates = serviceDatesByServiceId.get(serviceId);
    if (serviceDates != null) {
      dates.addAll(serviceDates);
    }
    return dates;
  }

  /**
   * Determine the set of service ids that are active on the specified service date.
   *
   * @param serviceDate the target service date
   * @return the set of service ids that are active on the specified service date
   */
  public Set<FeedScopedId> listServiceIdsOnServiceDate(LocalDate serviceDate) {
    // Note: Collections.emptySet(), not Set.of() — the latter throws NullPointerException on a
    // null argument to contains(), and the given serviceId here can be null (e.g. flex trips).
    return serviceIdsByDate.getOrDefault(serviceDate, Collections.emptySet());
  }

  /**
   * Determine whether the given service id is active on the specified service date.
   *
   * @param serviceId the target service id
   * @param serviceDate the target service date
   * @return {@code true} if the service id is active on the given service date
   */
  public boolean isActiveOn(FeedScopedId serviceId, LocalDate serviceDate) {
    return listServiceIdsOnServiceDate(serviceDate).contains(serviceId);
  }

  /**
   * Return the integer service code assigned to the given service id, or {@code null} if the
   * service id is not registered.
   * <p>
   * Service codes are small integers (0, 1, 2, …) allocated during graph build to enable
   * compact BitSet-based lookups in the Raptor routing engine instead of object comparisons.
   * The relationship with service id is 1-to-1.
   *
   * @return the integer code, or {@code null} if not found
   */
  public Integer getServiceCode(FeedScopedId serviceId) {
    return serviceCodes.get(serviceId);
  }

  /**
   * Map from GTFS ServiceIds to integers close to 0. Allows using BitSets instead of
   * {@code Set<Object>}.
   */
  public Map<FeedScopedId, Integer> getServiceCodes() {
    return serviceCodes;
  }

  /**
   * For all dates in the system get the service codes that run on it.
   */
  public Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return serviceCodesRunningForDate;
  }

  public Optional<LocalDate> startDate() {
    return Optional.ofNullable(startDate);
  }

  public Optional<LocalDate> endDate() {
    return Optional.ofNullable(endDate);
  }

  public boolean isEmpty() {
    return serviceDatesByServiceId.isEmpty();
  }

  /**
   * Merge scheduled calendar data into this calendar. Used during graph build only.
   *
   * @return a new instance with {@code data} merged in.
   */
  public TripCalendars merge(CalendarServiceData data) {
    Map<FeedScopedId, List<LocalDate>> newServiceDatesByServiceId = new HashMap<>(
      serviceDatesByServiceId
    );
    Map<LocalDate, Set<FeedScopedId>> newServiceIdsByDate = deepCopy(serviceIdsByDate);
    for (FeedScopedId serviceId : data.getServiceIds()) {
      List<LocalDate> dates = data
        .getServiceDatesForServiceId(serviceId)
        .stream()
        .sorted()
        .toList();
      newServiceDatesByServiceId.put(serviceId, dates);
      for (LocalDate date : dates) {
        newServiceIdsByDate.computeIfAbsent(date, d -> new HashSet<>()).add(serviceId);
      }
    }
    return new TripCalendars(
      Collections.unmodifiableMap(newServiceDatesByServiceId),
      freezeServiceIds(newServiceIdsByDate),
      serviceCodesRunningForDate,
      serviceCodes
    );
  }

  /**
   * Register {@code code} as the service code for {@code serviceId}. Used during graph build only.
   *
   * @return a new instance with the service code registered, or this same instance if the service
   * id was already registered with this exact code.
   */
  public TripCalendars withServiceCode(FeedScopedId serviceId, int code) {
    if (Integer.valueOf(code).equals(serviceCodes.get(serviceId))) {
      return this;
    }
    Map<FeedScopedId, Integer> updated = new HashMap<>(serviceCodes);
    updated.put(serviceId, code);
    return new TripCalendars(
      serviceDatesByServiceId,
      serviceIdsByDate,
      serviceCodesRunningForDate,
      Collections.unmodifiableMap(updated)
    );
  }

  /**
   * Compute {@link #getServiceCodesRunningForDate()} from the currently registered service ids
   * and service codes. Used once, during graph build, after all scheduled calendar data has been
   * merged in and all service codes have been registered.
   *
   * @return a new instance with {@link #getServiceCodesRunningForDate()} computed.
   */
  public TripCalendars initializeServiceCodesRunningForDate() {
    Map<LocalDate, TIntSet> newServiceCodesRunningForDate = new HashMap<>();
    for (FeedScopedId serviceId : serviceDatesByServiceId.keySet()) {
      Integer code = serviceCodes.get(serviceId);
      if (code == null) {
        continue;
      }
      List<LocalDate> serviceDates = serviceDatesByServiceId.get(serviceId);
      for (LocalDate serviceDate : serviceDates) {
        newServiceCodesRunningForDate
          .computeIfAbsent(serviceDate, ignored -> new TIntHashSet())
          .add(code);
      }
    }
    return new TripCalendars(
      serviceDatesByServiceId,
      serviceIdsByDate,
      freezeServiceCodes(newServiceCodesRunningForDate),
      serviceCodes
    );
  }

  /**
   * Get or create a serviceId for a given date. This method is used when a new trip is added from
   * a realtime data update. It makes sure the date is in the existing transit service period, i.e.
   * within {@link #startDate()} and {@link #endDate()}.
   * <p>
   * Unlike the other modification methods, this one only produces (and passes to {@code onUpdate})
   * a new instance if the service id was not already registered — the common case, where the same
   * date has already been seen by an earlier call, is a pure read with no allocation.
   *
   * @param serviceDate service date for the added service id
   * @param onUpdate callback invoked with the new instance, if and only if a new service id was
   * registered
   * @return service-id for date if it exists or is created. If the given service date is outside
   * the service period {@code null} is returned.
   */
  @Nullable
  public FeedScopedId getOrCreateServiceIdForDate(
    LocalDate serviceDate,
    Consumer<TripCalendars> onUpdate
  ) {
    if (!isWithinServicePeriod(serviceDate)) {
      return null;
    }

    FeedScopedId serviceId = new FeedScopedId(
      CAL_SERVICE_FEED_ID,
      ServiceDateUtils.asCompactString(serviceDate)
    );
    if (serviceDatesByServiceId.containsKey(serviceId) && serviceCodes.containsKey(serviceId)) {
      return serviceId;
    }

    Map<FeedScopedId, List<LocalDate>> newServiceDatesByServiceId = serviceDatesByServiceId;
    Map<LocalDate, Set<FeedScopedId>> newServiceIdsByDate = serviceIdsByDate;
    if (!serviceDatesByServiceId.containsKey(serviceId)) {
      newServiceDatesByServiceId = withEntry(
        serviceDatesByServiceId,
        serviceId,
        List.of(serviceDate)
      );
      newServiceIdsByDate = withAddedServiceId(serviceIdsByDate, serviceDate, serviceId);
      LOG.info("Adding serviceId {} to trip calendar", serviceId);
    }

    Map<FeedScopedId, Integer> newServiceCodes = serviceCodes;
    Map<LocalDate, TIntSet> newServiceCodesRunningForDate = serviceCodesRunningForDate;
    if (!serviceCodes.containsKey(serviceId)) {
      // Calculating new unique serviceCode based on size (!)
      int serviceCode = serviceCodes.size();
      newServiceCodes = withEntry(serviceCodes, serviceId, serviceCode);
      newServiceCodesRunningForDate = withAddedServiceCode(
        serviceCodesRunningForDate,
        serviceDate,
        serviceCode
      );
    }

    onUpdate.accept(
      new TripCalendars(
        newServiceDatesByServiceId,
        newServiceIdsByDate,
        newServiceCodesRunningForDate,
        newServiceCodes
      )
    );
    return serviceId;
  }

  private boolean isWithinServicePeriod(LocalDate serviceDate) {
    if (startDate == null || endDate == null) {
      return false;
    }
    return !serviceDate.isBefore(startDate) && !serviceDate.isAfter(endDate);
  }

  private static Map<LocalDate, Set<FeedScopedId>> deepCopy(
    Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate
  ) {
    Map<LocalDate, Set<FeedScopedId>> copy = new HashMap<>();
    serviceIdsByDate.forEach((date, serviceIds) -> copy.put(date, new HashSet<>(serviceIds)));
    return copy;
  }

  private static Map<LocalDate, Set<FeedScopedId>> freezeServiceIds(
    Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate
  ) {
    Map<LocalDate, Set<FeedScopedId>> frozen = new HashMap<>();
    serviceIdsByDate.forEach((date, serviceIds) ->
      frozen.put(date, Collections.unmodifiableSet(serviceIds))
    );
    return Collections.unmodifiableMap(frozen);
  }

  private static Map<LocalDate, TIntSet> freezeServiceCodes(
    Map<LocalDate, TIntSet> serviceCodesRunningForDate
  ) {
    Map<LocalDate, TIntSet> frozen = new HashMap<>();
    serviceCodesRunningForDate.forEach((date, codes) ->
      frozen.put(date, TCollections.unmodifiableSet(codes))
    );
    return Collections.unmodifiableMap(frozen);
  }

  private static <K, V> Map<K, V> withEntry(Map<K, V> map, K key, V value) {
    Map<K, V> copy = new HashMap<>(map);
    copy.put(key, value);
    return Collections.unmodifiableMap(copy);
  }

  private static Map<LocalDate, Set<FeedScopedId>> withAddedServiceId(
    Map<LocalDate, Set<FeedScopedId>> serviceIdsByDate,
    LocalDate date,
    FeedScopedId serviceId
  ) {
    Map<LocalDate, Set<FeedScopedId>> copy = new HashMap<>(serviceIdsByDate);
    Set<FeedScopedId> existing = copy.get(date);
    Set<FeedScopedId> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
    updated.add(serviceId);
    copy.put(date, Collections.unmodifiableSet(updated));
    return Collections.unmodifiableMap(copy);
  }

  private static Map<LocalDate, TIntSet> withAddedServiceCode(
    Map<LocalDate, TIntSet> serviceCodesRunningForDate,
    LocalDate date,
    int code
  ) {
    Map<LocalDate, TIntSet> copy = new HashMap<>(serviceCodesRunningForDate);
    TIntSet existing = copy.get(date);
    TIntSet updated = existing == null ? new TIntHashSet() : new TIntHashSet(existing);
    updated.add(code);
    copy.put(date, TCollections.unmodifiableSet(updated));
    return Collections.unmodifiableMap(copy);
  }
}

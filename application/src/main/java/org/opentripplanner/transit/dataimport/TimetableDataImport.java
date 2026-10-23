package org.opentripplanner.transit.dataimport;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.repository.ScheduledTimetableData;

/**
 * The scheduled timetable data imported from the GTFS and NeTEx feeds: trip patterns, trips on
 * service date, flex trips and the scheduled trip calendars. This is mutable and serialized with
 * the graph.
 * <p>
 * The content is converted into the immutable {@link ScheduledTimetableData} with {@link
 * #toScheduledTimetableData()}. The dependency goes from this data-import class to the core, not
 * the other way around.
 * <p>
 * The replacements of each trip on service date are kept in the order the replacing trips are
 * added, which is the order returned by {@link
 * ScheduledTimetableData#getReplacedByTripOnServiceDate}.
 * <p>
 * Converting the content freezes the data import: later modifications would not be reflected in
 * the converted data, so they fail.
 */
public class TimetableDataImport {

  private final Map<FeedScopedId, TripPattern> tripPatternForId = new HashMap<>();
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById = new HashMap<>();
  private final ListMultimap<FeedScopedId, TripOnServiceDate> replacedByTripOnServiceDates =
    ArrayListMultimap.create();
  private final Map<FeedScopedId, FlexTrip<?, ?>> flexTripForId = new HashMap<>();
  private TripCalendars tripCalendars = TripCalendars.empty();

  /**
   * Set when the content is converted. Not serialized, so a deserialized data import can be
   * modified.
   */
  private transient boolean frozen = false;

  public TimetableDataImport() {}

  public void addTripPattern(FeedScopedId id, TripPattern tripPattern) {
    assertModificationsAllowed();
    tripPatternForId.put(id, tripPattern);
  }

  @Nullable
  public TripPattern getTripPatternForId(FeedScopedId id) {
    return tripPatternForId.get(id);
  }

  public Collection<TripPattern> getAllTripPatterns() {
    return Collections.unmodifiableCollection(tripPatternForId.values());
  }

  public void addTripOnServiceDate(TripOnServiceDate tripOnServiceDate) {
    assertModificationsAllowed();
    tripOnServiceDateById.put(tripOnServiceDate.getId(), tripOnServiceDate);
    for (var replacementFor : tripOnServiceDate.getReplacementFor()) {
      replacedByTripOnServiceDates.put(replacementFor.getId(), tripOnServiceDate);
    }
  }

  public Collection<TripOnServiceDate> getAllTripsOnServiceDate() {
    return Collections.unmodifiableCollection(tripOnServiceDateById.values());
  }

  public void addFlexTrip(FeedScopedId id, FlexTrip<?, ?> flexTrip) {
    assertModificationsAllowed();
    flexTripForId.put(id, flexTrip);
  }

  @Nullable
  public FlexTrip<?, ?> getFlexTrip(FeedScopedId id) {
    return flexTripForId.get(id);
  }

  public Collection<FlexTrip<?, ?>> getAllFlexTrips() {
    return Collections.unmodifiableCollection(flexTripForId.values());
  }

  public boolean hasFlexTrips() {
    return !flexTripForId.isEmpty();
  }

  public void updateCalendarServiceData(CalendarServiceData data) {
    assertModificationsAllowed();
    tripCalendars = tripCalendars.merge(data);
  }

  /** Register {@code code} as the service code for {@code serviceId}. */
  public void putServiceCode(FeedScopedId serviceId, int code) {
    assertModificationsAllowed();
    tripCalendars = tripCalendars.withServiceCode(serviceId, code);
  }

  /** The service code of each scheduled service id. */
  public Map<FeedScopedId, Integer> getServiceCodes() {
    return tripCalendars.getServiceCodes();
  }

  /**
   * The trip calendars built so far. The service codes running on each date are not computed
   * before the conversion into {@link ScheduledTimetableData}.
   */
  public TripCalendars getTripCalendars() {
    return tripCalendars;
  }

  /**
   * The stops used by scheduled trips that allow cars (e.g. car ferries), including the regular
   * stops of group stops.
   */
  public Set<StopLocation> getStopLocationsUsedForCarsAllowedTrips() {
    return getStopLocationsUsedByTripTimes(
      tt -> tt.getTrip().getCarsAllowed() == CarAccess.ALLOWED
    );
  }

  /**
   * The stops used by scheduled trips that allow bikes, including the regular stops of group
   * stops.
   */
  public Set<StopLocation> getStopLocationsUsedForBikesAllowedTrips() {
    return getStopLocationsUsedByTripTimes(
      tt -> tt.getTrip().getBikesAllowed() == BikeAccess.ALLOWED
    );
  }

  /**
   * Convert the content into the immutable scheduled data. The content is copied, so later
   * modifications would not be reflected in it: this freezes the data import, and any later
   * modification fails.
   */
  public ScheduledTimetableData toScheduledTimetableData() {
    frozen = true;
    return ScheduledTimetableData.of(
      tripPatternForId,
      tripOnServiceDateById,
      replacedByTripOnServiceDates,
      flexTripForId,
      tripCalendars
    );
  }

  private void assertModificationsAllowed() {
    if (frozen) {
      throw new IllegalStateException(
        "The timetable data import is frozen, it has already been converted into the " +
          "scheduled timetable data."
      );
    }
  }

  private Set<StopLocation> getStopLocationsUsedByTripTimes(
    Predicate<TripTimes<?>> tripTimesPredicate
  ) {
    Set<StopLocation> stopLocations = getAllTripPatterns()
      .stream()
      .filter(t -> t.getScheduledTimetable().getTripTimes().stream().anyMatch(tripTimesPredicate))
      .flatMap(t -> t.getStops().stream())
      .collect(Collectors.toSet());

    stopLocations.addAll(
      stopLocations
        .stream()
        .filter(GroupStop.class::isInstance)
        .map(GroupStop.class::cast)
        .flatMap(g -> g.getChildLocations().stream().filter(RegularStop.class::isInstance))
        .toList()
    );
    return stopLocations;
  }
}

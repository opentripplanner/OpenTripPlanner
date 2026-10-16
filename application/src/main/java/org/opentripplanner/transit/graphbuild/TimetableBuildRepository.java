package org.opentripplanner.transit.graphbuild;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * The scheduled timetable data while the graph is being built: trip patterns, trips on service
 * date, flex trips and the scheduled trip calendars. This is mutable, used by the graph build
 * modules only, and serialized with the graph.
 * <p>
 * When the server starts, the content is converted into the immutable {@link
 * ScheduledTimetableData} with {@link #toScheduledTimetableData()}, which is then owned by the
 * timetable repository. The dependency goes from this graph-build class to the core, not the
 * other way around.
 * <p>
 * Trips on service date are kept in insertion order, which is the order of the replacements
 * returned by {@link ScheduledTimetableData#getReplacedByTripOnServiceDate}.
 * <p>
 * Converting the content freezes the repository: the scheduled data wraps the maps of this
 * repository, so any later modification fails instead of changing data that must stay immutable.
 */
public class TimetableBuildRepository {

  private final Map<FeedScopedId, TripPattern> tripPatternForId = new HashMap<>();
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById = new LinkedHashMap<>();
  private final Map<FeedScopedId, FlexTrip<?, ?>> flexTripForId = new HashMap<>();
  private TripCalendars tripCalendars = TripCalendars.empty();

  /**
   * Set when the content is converted. Not serialized: a repository loaded from a graph file can
   * still be modified by a build.
   */
  private transient boolean frozen = false;

  public TimetableBuildRepository() {}

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
   * stops of group stops. They need to be connected to the road network.
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
   * Convert the content into the immutable scheduled data used at runtime. The scheduled data
   * wraps the maps of this repository instead of copying them, so this freezes the repository:
   * the graph build must be complete, and any later modification fails.
   */
  public ScheduledTimetableData toScheduledTimetableData() {
    frozen = true;
    return ScheduledTimetableData.of(
      tripPatternForId,
      tripOnServiceDateById,
      flexTripForId,
      tripCalendars
    );
  }

  private void assertModificationsAllowed() {
    if (frozen) {
      throw new IllegalStateException(
        "The timetable build repository is frozen, it has already been converted into the " +
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

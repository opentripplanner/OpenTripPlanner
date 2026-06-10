package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;

public class FlexIndex {

  private final Multimap<FeedScopedId, FlexTrip<?, ?>> flexTripsByStopId = HashMultimap.create();

  private final Map<FeedScopedId, Route> routeById = new HashMap<>();

  private final Map<FeedScopedId, FlexTrip<?, ?>> tripById = new HashMap<>();

  private final Map<LocalDate, List<FlexTripForDate>> flexTripsRunningOnDate = new HashMap<>();

  private final Multimap<StopLocation, Route> routeByStop;

  public FlexIndex(TimetableRepository timetableRepository) {
    var routeByStopBuilder = ImmutableSetMultimap.<StopLocation, Route>builder();

    for (FlexTrip<?, ?> flexTrip : timetableRepository.getAllFlexTrips()) {
      var route = flexTrip.getTrip().getRoute();
      routeById.put(route.getId(), route);
      tripById.put(flexTrip.getTrip().getId(), flexTrip);
      for (StopLocation stop : flexTrip.getStops()) {
        if (stop instanceof GroupStop groupStop) {
          for (StopLocation stopElement : groupStop.getChildLocations()) {
            flexTripsByStopId.put(stopElement.getId(), flexTrip);
            routeByStopBuilder.put(stopElement, route);
          }
        } else {
          flexTripsByStopId.put(stop.getId(), flexTrip);
          routeByStopBuilder.put(stop, route);
        }
      }

      timetableRepository
        .getCalendarService()
        .getServiceDatesForServiceId(flexTrip.getTrip().getServiceId())
        .forEach(serviceDate -> {
          LocalDate maxDate = serviceDate.plusDays(flexTrip.maxSpanDays());
          FlexTripForDate flexTripForDate = new FlexTripForDate(serviceDate, maxDate, flexTrip);

          serviceDate
            .datesUntil(maxDate.plusDays(1))
            .forEach(runningDate -> {
              flexTripsRunningOnDate
                .computeIfAbsent(runningDate, d -> new ArrayList<>())
                .add(flexTripForDate);
            });
        });
    }

    routeByStop = routeByStopBuilder.build();
  }

  public Collection<FlexTrip<?, ?>> getFlexTripsByStopId(FeedScopedId stopLocationId) {
    return flexTripsByStopId.get(stopLocationId);
  }

  public boolean contains(Route route) {
    return routeById.containsKey(route.getId());
  }

  public Collection<Route> getAllFlexRoutes() {
    return routeById.values();
  }

  public FlexTrip<?, ?> getTripById(FeedScopedId id) {
    return tripById.get(id);
  }

  public Collection<FlexTrip<?, ?>> getAllFlexTrips() {
    return tripById.values();
  }

  /**
   * For a given stop location returns the flex routes that visit it, taking care to resolve
   * members of group stops (which are visited "transitively", ie. not directly but by
   * them being members of the group stop).
   */
  public Collection<Route> findRoutes(StopLocation stop) {
    return routeByStop.get(stop);
  }

  /**
   * Returns flex trips for the given running date. Running date is not necessarily the same as the
   * service date. A Trip "runs through" a date if any of its arrivals or departures is happening on
   * that date. Flex trips can have multiple running dates.
   */
  public Collection<FlexTripForDate> getFlexTripsForRunningDate(LocalDate date) {
    return flexTripsRunningOnDate.getOrDefault(date, List.of());
  }
}

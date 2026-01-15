package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TimetableRepository;

public class FlexIndex {

  private final Multimap<StopLocation, FlexTrip<?, ?>> flexTripsByStop = HashMultimap.create();

  private final Map<FeedScopedId, Route> routeById = new HashMap<>();

  private final Map<FeedScopedId, FlexTrip<?, ?>> tripById = new HashMap<>();

  public FlexIndex(TimetableRepository timetableRepository) {
    for (FlexTrip<?, ?> flexTrip : timetableRepository.getAllFlexTrips()) {
      routeById.put(flexTrip.getTrip().getRoute().getId(), flexTrip.getTrip().getRoute());
      tripById.put(flexTrip.getTrip().getId(), flexTrip);
      for (StopLocation stop : flexTrip.getStops()) {
        if (stop instanceof GroupStop groupStop) {
          for (StopLocation stopElement : groupStop.getChildLocations()) {
            flexTripsByStop.put(stopElement, flexTrip);
          }
        } else {
          flexTripsByStop.put(stop, flexTrip);
        }
      }
    }
  }

  public Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation) {
    return flexTripsByStop.get(stopLocation);
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
}

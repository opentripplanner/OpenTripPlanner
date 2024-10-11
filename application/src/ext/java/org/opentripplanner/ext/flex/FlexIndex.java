package org.opentripplanner.ext.flex;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;

public class FlexIndex {

  private final Multimap<StopLocation, PathTransfer> transfersToStop = ArrayListMultimap.create();

  private final Multimap<StopLocation, FlexTrip<?, ?>> flexTripsByStop = HashMultimap.create();

  private final Map<FeedScopedId, Route> routeById = new HashMap<>();

  private final Map<FeedScopedId, FlexTrip<?, ?>> tripById = new HashMap<>();

  public FlexIndex(TransitModel transitModel) {
    for (PathTransfer transfer : transitModel.getAllPathTransfers()) {
      transfersToStop.put(transfer.to, transfer);
    }
    for (FlexTrip<?, ?> flexTrip : transitModel.getAllFlexTrips()) {
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

  public Collection<PathTransfer> getTransfersToStop(StopLocation stopLocation) {
    return transfersToStop.get(stopLocation);
  }

  public Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation) {
    return flexTripsByStop.get(stopLocation);
  }

  public Route getRouteById(FeedScopedId id) {
    return routeById.get(id);
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

package org.opentripplanner.ext.flex;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitModel;

public class FlexIndex {

  public Multimap<StopLocation, PathTransfer> transfersToStop = ArrayListMultimap.create();

  public Multimap<StopLocation, FlexTrip> flexTripsByStop = HashMultimap.create();

  public Map<FeedScopedId, Route> routeById = new HashMap<>();

  public Map<FeedScopedId, FlexTrip> tripById = new HashMap<>();

  public FlexIndex(TransitModel transitModel) {
    for (PathTransfer transfer : transitModel.transfersByStop.values()) {
      transfersToStop.put(transfer.to, transfer);
    }
    for (FlexTrip flexTrip : transitModel.flexTripsById.values()) {
      routeById.put(flexTrip.getTrip().getRoute().getId(), flexTrip.getTrip().getRoute());
      tripById.put(flexTrip.getTrip().getId(), flexTrip);
      for (StopLocation stop : flexTrip.getStops()) {
        if (stop instanceof FlexLocationGroup) {
          for (StopLocation stopElement : ((FlexLocationGroup) stop).getLocations()) {
            flexTripsByStop.put(stopElement, flexTrip);
          }
        } else {
          flexTripsByStop.put(stop, flexTrip);
        }
      }
    }
  }

  Stream<FlexTrip> getFlexTripsByStop(StopLocation stopLocation) {
    return flexTripsByStop.get(stopLocation).stream();
  }
}

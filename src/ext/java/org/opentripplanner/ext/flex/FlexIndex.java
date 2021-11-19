package org.opentripplanner.ext.flex;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graph.Graph;

public class FlexIndex {
  public Multimap<StopLocation, PathTransfer> transfersToStop = ArrayListMultimap.create();

  public Multimap<StopLocation, FlexTrip> flexTripsByStop = HashMultimap.create();

  public Multimap<StopLocation, FlexLocationGroup> locationGroupsByStop = ArrayListMultimap.create();

  public HashGridSpatialIndex<FlexStopLocation> locationIndex = new HashGridSpatialIndex<FlexStopLocation>();

  public Map<FeedScopedId, Route> routeById = new HashMap<>();

  public Map<FeedScopedId, FlexTrip> tripById = new HashMap<>();

  public FlexIndex(Graph graph) {
    for (PathTransfer transfer : graph.transfersByStop.values()) {
      transfersToStop.put(transfer.to, transfer);
    }
    for (FlexTrip flexTrip : graph.flexTripsById.values()) {
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
    for (FlexLocationGroup flexLocationGroup : graph.locationGroupsById.values()) {
      for (StopLocation stop : flexLocationGroup.getLocations()) {
        locationGroupsByStop.put(stop, flexLocationGroup);
      }
    }
    for (FlexStopLocation flexStopLocation : graph.locationsById.values()) {
      locationIndex.insert(flexStopLocation.getGeometry().getEnvelopeInternal(), flexStopLocation);
    }
  }

  Stream<FlexTrip> getFlexTripsByStop(StopLocation stopLocation) {
    return flexTripsByStop.get(stopLocation).stream();
  }
}

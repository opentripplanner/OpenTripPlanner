package org.opentripplanner.ext.carpooling.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private final Map<FeedScopedId, CarpoolTrip> trips = new ConcurrentHashMap<>();

  @Override
  public Collection<CarpoolTrip> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  public void addCarpoolTrip(CarpoolTrip trip) {
    trips.put(trip.getId(), trip);
  }

  @Override
  public void removeCarpoolTrip(FeedScopedId tripId) {
    trips.remove(tripId);
  }

  @Override
  public CarpoolTrip getCarpoolTrip(FeedScopedId tripId) {
    return trips.get(tripId);
  }
}

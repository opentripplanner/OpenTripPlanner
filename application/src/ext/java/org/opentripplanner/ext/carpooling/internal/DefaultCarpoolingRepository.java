package org.opentripplanner.ext.carpooling.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCarpoolingRepository implements CarpoolingRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingRepository.class);

  private final Map<FeedScopedId, CarpoolTrip> trips = new ConcurrentHashMap<>();

  @Override
  public Collection<CarpoolTrip> getCarpoolTrips() {
    return trips.values();
  }

  @Override
  public void upsertCarpoolTrip(CarpoolTrip trip) {
    CarpoolTrip existingTrip = trips.put(trip.getId(), trip);
    if (existingTrip != null) {
      LOG.info("Updated carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    } else {
      LOG.info("Added new carpool trip {} with {} stops", trip.getId(), trip.stops().size());
    }
  }
}

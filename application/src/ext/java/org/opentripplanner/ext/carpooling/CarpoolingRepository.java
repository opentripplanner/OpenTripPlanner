package org.opentripplanner.ext.carpooling;

import java.util.Collection;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * The CarpoolingRepository interface allows for the management and retrieval of carpooling trips.
 */
public interface CarpoolingRepository {
  Collection<CarpoolTrip> getCarpoolTrips();
  void addCarpoolTrip(CarpoolTrip trip);
}

package org.opentripplanner.ext.carpooling;

import java.util.Collection;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The CarpoolingRepository interface allows for the management and retrieval of carpooling trips.
 */
public interface CarpoolingRepository {
  /**
   * Get all available carpool trips for routing.
   */
  Collection<CarpoolTrip> getCarpoolTrips();

  /**
   * Add a carpool trip to the repository.
   */
  void addCarpoolTrip(CarpoolTrip trip);

  /**
   * Remove a carpool trip from the repository.
   */
  void removeCarpoolTrip(FeedScopedId tripId);

  /**
   * Get a specific carpool trip by ID.
   */
  CarpoolTrip getCarpoolTrip(FeedScopedId tripId);
}

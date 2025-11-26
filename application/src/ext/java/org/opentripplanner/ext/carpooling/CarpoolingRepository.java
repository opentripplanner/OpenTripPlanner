package org.opentripplanner.ext.carpooling;

import java.util.Collection;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Repository for managing carpooling trip ({@link CarpoolTrip}) data.
 * <p>
 * This repository maintains an in-memory index of driver trips.
 *
 * @see CarpoolTrip for trip data model
 * @see org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater for real-time updates
 */
public interface CarpoolingRepository {
  /**
   * Returns all currently carpooling trips.
   * <p>
   * The returned collection includes all driver trips that have been added via {@link #upsertCarpoolTrip}
   * and not yet removed or expired. The collection is typically used by the routing service to find
   * compatible trips for passengers.
   */
  Collection<CarpoolTrip> getCarpoolTrips();

  /**
   * Inserts a new carpooling trip or updates an existing trip with the same ID.
   * <p>
   * This method is the primary mechanism for adding driver trip data to the repository. It is
   * typically called by real-time updaters when receiving trip information from external systems,
   * or when passenger bookings modify trip capacity.
   *
   * <h3>Validation</h3>
   * <p>
   * The method does not validate trip data beyond basic null checks. It is the caller's
   * responsibility to ensure the trip is valid (has stops, positive capacity, etc.). Invalid
   * trips may cause routing failures later.
   *
   * @param trip the carpool trip to insert or update, must not be null. If a trip with the same
   *        ID exists, it will be completely replaced.
   * @throws IllegalArgumentException if trip is null
   */
  void upsertCarpoolTrip(CarpoolTrip trip);
}

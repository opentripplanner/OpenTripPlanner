package org.opentripplanner.service.vehiclerental;

import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

/**
 * The read-only service for getting information about rental vehicles.
 * <p>
 * For writing data see {@link VehicleRentalRepository}
 * <p>
 * Extends {@link GeofencingZoneService} so consumers that only need zone queries can depend on the
 * narrower interface — geofencing zones are part of rental data (registered per network by the
 * GBFS updater) and the rental service is their natural owner.
 */
public interface VehicleRentalService extends GeofencingZoneService {
  Collection<VehicleRentalPlace> getVehicleRentalPlaces();

  VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id);

  List<VehicleRentalVehicle> getVehicleRentalVehicles();

  VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id);

  List<VehicleRentalStation> getVehicleRentalStations();

  VehicleRentalStation getVehicleRentalStation(FeedScopedId id);

  boolean hasRentalBikes();

  /**
   * The vehicle rental networks known to OTP, in alphabetical order.
   * <p>
   * Rental places and geofencing zones are unioned because a network can have one without the
   * other: zones applied during the graph build are present before any updater has reported a
   * vehicle, and a network may equally publish vehicles but no zones.
   */
  List<String> listNetworks();

  /**
   * Gets all the vehicle rental stations inside the envelope. This is currently done by iterating
   * over a set, but we could use a spatial index if the number of vehicle rental stations is high
   * enough for performance to be a concern.
   */
  List<VehicleRentalStation> getVehicleRentalStationForEnvelope(
    double minLon,
    double minLat,
    double maxLon,
    double maxLat
  );

  /**
   * Gets all vehicle rental places inside an envelope.
   */
  List<VehicleRentalPlace> getVehicleRentalPlacesForEnvelope(Envelope envelope);
}

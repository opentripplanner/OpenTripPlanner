package org.opentripplanner.service.vehiclerental;

import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The read-only service for getting information about rental vehicles.
 * <p>
 * For writing data see {@link VehicleRentalRepository}
 */
public interface VehicleRentalService {
  Collection<VehicleRentalPlace> getVehicleRentalPlaces();

  VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id);

  List<VehicleRentalVehicle> getVehicleRentalVehicles();

  VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id);

  List<VehicleRentalStation> getVehicleRentalStations();

  VehicleRentalStation getVehicleRentalStation(FeedScopedId id);

  boolean hasRentalBikes();

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

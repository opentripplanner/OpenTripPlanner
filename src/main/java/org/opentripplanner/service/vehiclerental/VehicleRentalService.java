package org.opentripplanner.service.vehiclerental;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public interface VehicleRentalService {
  Collection<VehicleRentalPlace> getVehicleRentalPlaces();

  VehicleRentalPlace getVehicleRentalPlace(FeedScopedId id);

  List<VehicleRentalVehicle> getVehicleRentalVehicles();

  VehicleRentalVehicle getVehicleRentalVehicle(FeedScopedId id);

  List<VehicleRentalStation> getVehicleRentalStations();

  VehicleRentalStation getVehicleRentalStation(FeedScopedId id);

  boolean hasRentalBikes();

  List<VehicleRentalPlace> getVehicleRentalStationForEnvelope(
    double minLon,
    double minLat,
    double maxLon,
    double maxLat
  );
}

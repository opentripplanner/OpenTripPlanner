package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.List;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.updater.DataSource;

public interface VehicleRentalDatasource extends DataSource<VehicleRentalPlace> {
  default List<GeofencingZone> getGeofencingZones() {
    return List.of();
  }
}

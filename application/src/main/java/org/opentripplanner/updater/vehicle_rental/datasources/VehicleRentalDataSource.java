package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.List;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.updater.spi.DataSource;

public interface VehicleRentalDataSource extends DataSource<VehicleRentalPlace> {
  default List<GeofencingZone> getGeofencingZones() {
    return List.of();
  }
}

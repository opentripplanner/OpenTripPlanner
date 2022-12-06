package org.opentripplanner.updater.vehicle_rental.datasources;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.updater.DataSource;

public interface VehicleRentalDatasource extends DataSource<VehicleRentalPlace> {
  default List<Geometry> getGeofencingZones() { return List.of();}

}

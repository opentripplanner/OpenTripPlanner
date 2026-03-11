package org.opentripplanner.gbfs;

import java.util.List;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

public interface GbfsFeedMapper {
  List<VehicleRentalPlace> getUpdates();

  List<GeofencingZone> getGeofencingZones();
}

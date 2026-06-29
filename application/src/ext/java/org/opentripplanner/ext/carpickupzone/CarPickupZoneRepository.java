package org.opentripplanner.ext.carpickupzone;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZone;

/**
 * Repository for car pickup zone data.
 */
public interface CarPickupZoneRepository extends Serializable {
  /**
   * Add car pickup zones to the repository.
   */
  void addZones(List<CarPickupZone> zones);

  /**
   * Return all stored car pickup zones.
   */
  List<CarPickupZone> getZones();
}

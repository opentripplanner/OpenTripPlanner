package org.opentripplanner.ext.taxizone;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.ext.taxizone.model.TaxiZone;

/**
 * Repository for taxi zone data.
 */
public interface TaxiZoneRepository extends Serializable {
  /**
   * Add taxi zones to the repository.
   */
  void addZones(List<TaxiZone> zones);

  /**
   * Return all stored taxi zones.
   */
  List<TaxiZone> getZones();
}

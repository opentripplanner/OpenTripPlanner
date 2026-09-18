package org.opentripplanner.ext.taxi;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.ext.taxi.model.TaxiZone;

/**
 * Repository for taxi zone data.
 */
public interface TaxiRepository extends Serializable {
  /**
   * Add taxi zones to the repository.
   */
  void addZones(List<TaxiZone> zones);

  /**
   * Return all stored taxi zones.
   */
  List<TaxiZone> getZones();
}

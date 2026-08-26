package org.opentripplanner.ext.taxizone.internal;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.model.TaxiZone;

public class DefaultTaxiZoneRepository implements TaxiZoneRepository {

  private final List<TaxiZone> zones = new ArrayList<>();

  @Override
  public void addZones(List<TaxiZone> zones) {
    this.zones.addAll(zones);
  }

  @Override
  public List<TaxiZone> getZones() {
    return List.copyOf(zones);
  }
}

package org.opentripplanner.ext.taxi.internal;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.ext.taxi.model.TaxiZone;

public class DefaultTaxiRepository implements TaxiRepository {

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

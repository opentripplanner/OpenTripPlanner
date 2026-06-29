package org.opentripplanner.ext.carpickupzone.internal;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZone;

public class DefaultCarPickupZoneRepository implements CarPickupZoneRepository {

  private final List<CarPickupZone> zones = new ArrayList<>();

  @Override
  public void addZones(List<CarPickupZone> zones) {
    this.zones.addAll(zones);
  }

  @Override
  public List<CarPickupZone> getZones() {
    return List.copyOf(zones);
  }
}

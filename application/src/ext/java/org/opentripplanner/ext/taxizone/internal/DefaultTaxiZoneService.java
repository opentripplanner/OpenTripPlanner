package org.opentripplanner.ext.taxizone.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.TaxiZoneService;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.street.geometry.WgsCoordinate;

public class DefaultTaxiZoneService implements TaxiZoneService {

  private final TaxiZoneIndex zoneIndex;

  public DefaultTaxiZoneService(List<TaxiZone> zones) {
    this.zoneIndex = new TaxiZoneIndex(zones);
  }

  @Override
  public Optional<TaxiZone> findZone(WgsCoordinate pickup, WgsCoordinate dropoff, LocalDate date) {
    return zoneIndex.findFirstZone(pickup, dropoff, date);
  }
}

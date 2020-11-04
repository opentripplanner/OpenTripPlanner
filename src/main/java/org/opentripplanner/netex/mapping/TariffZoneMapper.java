package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FareZone;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;

class TariffZoneMapper {

  private final FeedScopedIdFactory idFactory;

  TariffZoneMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Map Netex TariffZone to OTP TariffZone
   */
  FareZone mapTariffZone(org.rutebanken.netex.model.TariffZone tariffZone) {
    FeedScopedId id = idFactory.createId(tariffZone.getId());
    String name = tariffZone.getName().getValue();
    return new FareZone(id, name);
  }
}

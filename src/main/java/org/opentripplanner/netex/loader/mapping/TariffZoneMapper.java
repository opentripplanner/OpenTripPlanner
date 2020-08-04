package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TariffZone;

public class TariffZoneMapper {

  private final FeedScopedIdFactory idFactory;

  public TariffZoneMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Map Netex TariffZone to OTP TariffZone
   */
  public TariffZone mapTariffZone(org.rutebanken.netex.model.TariffZone tariffZone) {
    FeedScopedId id = idFactory.createId(tariffZone.getId());
    String name = tariffZone.getName().getValue();
    return new TariffZone(id, name);
  }
}

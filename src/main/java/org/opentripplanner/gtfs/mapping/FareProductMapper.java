package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FareProduct;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.WrappedCurrency;

public class FareProductMapper {

  public FareProduct map(org.onebusaway.gtfs.model.FareProduct fareProduct) {
    return new FareProduct(
      AgencyAndIdMapper.mapAgencyAndId(fareProduct.getId()),
      fareProduct.getName(),
      new Money(
        new WrappedCurrency(fareProduct.getCurrency()),
        (int) (fareProduct.getAmount() * 100)
      )
    );
  }
}

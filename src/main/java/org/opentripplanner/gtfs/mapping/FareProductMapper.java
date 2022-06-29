package org.opentripplanner.gtfs.mapping;

import java.util.Currency;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.routing.core.Money;

public class FareProductMapper {

  public FareProduct map(org.onebusaway.gtfs.model.FareProduct fareProduct) {
    return new FareProduct(
      AgencyAndIdMapper.mapAgencyAndId(fareProduct.getId()),
      fareProduct.getName(),
      new Money(
        Currency.getInstance(fareProduct.getCurrency()),
        (int) (fareProduct.getAmount() * 100)
      )
    );
  }
}

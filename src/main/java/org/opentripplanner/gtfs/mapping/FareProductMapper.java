package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import org.opentripplanner.model.FareProduct;
import org.opentripplanner.routing.core.Money;

public class FareProductMapper {

  public static int NOT_SET = -999;

  public FareProduct map(org.onebusaway.gtfs.model.FareProduct fareProduct) {
    var id = AgencyAndIdMapper.mapAgencyAndId(fareProduct.getId());
    var price = new Money(
      Currency.getInstance(fareProduct.getCurrency()),
      (int) (fareProduct.getAmount() * 100)
    );

    Duration duration = null;
    if (fareProduct.getDurationUnit() != NOT_SET) {
      duration = toTemporalUnit(fareProduct.getDurationUnit(), fareProduct.getDurationAmount());
    }
    return new FareProduct(id, fareProduct.getName(), price, duration);
  }

  private static Duration toTemporalUnit(int unit, int amount) {
    var timeUnit =
      switch (unit) {
        case 0 -> ChronoUnit.SECONDS;
        case 1 -> ChronoUnit.MINUTES;
        case 2 -> ChronoUnit.HOURS;
        case 3 -> ChronoUnit.DAYS;
        case 4 -> ChronoUnit.WEEKS;
        case 5 -> ChronoUnit.MONTHS;
        case 6 -> ChronoUnit.YEARS;
        default -> throw new IllegalStateException("Unexpected value: " + unit);
      };
    return Duration.of(amount, timeUnit);
  }
}

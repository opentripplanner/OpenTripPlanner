package org.opentripplanner.gtfs.mapping;

import java.time.Duration;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FareProductMapper {

  public static int NOT_SET = -999;

  private final IdFactory idFactory;
  private final Set<FareProduct> mappedFareProducts = new HashSet<>();

  public FareProductMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  public FareProduct map(org.onebusaway.gtfs.model.FareProduct rhs) {
    var currency = Currency.getInstance(rhs.getCurrency());
    var price = Money.ofFractionalAmount(currency, rhs.getAmount());

    Duration duration = null;
    if (rhs.getDurationUnit() != NOT_SET) {
      duration = toDuration(rhs.getDurationUnit(), rhs.getDurationAmount());
    }
    var fp = FareProduct.of(idFactory.createId(rhs.getFareProductId()), rhs.getName(), price)
      .withValidity(duration)
      .withCategory(toInternalModel(rhs.getRiderCategory()))
      .withMedium(toInternalModel(rhs.getFareMedium()))
      .build();
    mappedFareProducts.add(fp);
    return fp;
  }

  public Collection<FareProduct> map(
    Collection<org.onebusaway.gtfs.model.FareProduct> allFareProducts
  ) {
    return allFareProducts.stream().map(this::map).toList();
  }

  public Collection<FareProduct> getByFareProductId(FeedScopedId fareProductId) {
    return mappedFareProducts.stream().filter(p -> p.id().equals(fareProductId)).toList();
  }

  private RiderCategory toInternalModel(org.onebusaway.gtfs.model.RiderCategory riderCategory) {
    if (riderCategory == null) {
      return null;
    } else {
      return new RiderCategory(
        idFactory.createId(riderCategory.getId()),
        riderCategory.getName(),
        riderCategory.getEligibilityUrl()
      );
    }
  }

  private static Duration toDuration(int unit, int amount) {
    // TODO: this isn't totally correct since we need to check if we go, for example, past the
    // end of the business day. the correct solution would be to also take duration_type into account.
    return switch (unit) {
      case 0 -> Duration.ofSeconds(amount);
      case 1 -> Duration.ofMinutes(amount);
      case 2 -> Duration.ofHours(amount);
      case 3 -> Duration.ofDays(amount);
      case 4 -> Duration.ofDays(amount * 7L);
      case 5 -> Duration.ofDays(amount * 31L); // not totally right but good enough
      case 6 -> Duration.ofDays(amount * 365L);
      default -> throw new IllegalStateException("Unexpected value: " + unit);
    };
  }

  private FareMedium toInternalModel(org.onebusaway.gtfs.model.FareMedium c) {
    if (c == null) {
      return null;
    } else {
      return new FareMedium(idFactory.createId(c.getId()), c.getName());
    }
  }
}

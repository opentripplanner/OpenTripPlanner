package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;

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

    var fp = FareProduct.of(
      idFactory.createId(rhs.getFareProductId(), "fare product"),
      rhs.getName(),
      price
    )
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

  public Collection<FareProduct> findFareProducts(FeedScopedId fareProductId) {
    return mappedFareProducts
      .stream()
      .filter(p -> p.id().equals(fareProductId))
      .toList();
  }

  @Nullable
  private RiderCategory toInternalModel(
    @Nullable org.onebusaway.gtfs.model.RiderCategory riderCategory
  ) {
    if (riderCategory == null) {
      return null;
    } else {
      return RiderCategory.of(idFactory.createId(riderCategory.getId(), "rider category"))
        .withName(riderCategory.getName())
        .withIsDefault(mapIsDefaultCategory(riderCategory))
        .build();
    }
  }

  private static boolean mapIsDefaultCategory(
    org.onebusaway.gtfs.model.RiderCategory riderCategory
  ) {
    return riderCategory.getIsDefaultFareCategory() == 1;
  }

  @Nullable
  private FareMedium toInternalModel(@Nullable org.onebusaway.gtfs.model.FareMedium c) {
    if (c == null) {
      return null;
    } else {
      return new FareMedium(idFactory.createId(c.getId(), "fare medium"), c.getName());
    }
  }
}

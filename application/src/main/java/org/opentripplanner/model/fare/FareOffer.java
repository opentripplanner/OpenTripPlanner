package org.opentripplanner.model.fare;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public sealed interface FareOffer
  permits FareOffer.DefaultFareProduct, FareOffer.DependentFareProduct {
  default String uniqueInstanceId(ZonedDateTime zonedDateTime) {
    return fareProduct().uniqueInstanceId(zonedDateTime);
  }

  FareProduct fareProduct();

  record DefaultFareProduct(FareProduct fareProduct) implements FareOffer {}

  record DependentFareProduct(FareProduct fareProduct, Set<FareOffer> dependencies)
    implements FareOffer {
    public Collection<FareOffer> dependenciesMatchingCategoryAndMedium() {
      return dependencies
        .stream()
        .filter(
          fp ->
            Objects.equals(fp.fareProduct().category(), fareProduct.category()) &&
            Objects.equals(fp.fareProduct().medium(), fareProduct.medium())
        )
        .toList();
    }
  }
}

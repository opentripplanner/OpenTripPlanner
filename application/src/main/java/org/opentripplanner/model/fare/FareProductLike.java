package org.opentripplanner.model.fare;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public sealed interface FareProductLike
  permits FareProductLike.DefaultFareProduct, FareProductLike.DependentFareProduct {
  default String uniqueInstanceId(ZonedDateTime zonedDateTime) {
    return fareProduct().uniqueInstanceId(zonedDateTime);
  }

  FareProduct fareProduct();

  record DefaultFareProduct(FareProduct fareProduct) implements FareProductLike {}

  record DependentFareProduct(FareProduct fareProduct, Set<FareProductLike> dependencies)
    implements FareProductLike {
    public DependentFareProduct(FareProduct fp, Collection<FareProductLike> dependencies) {
      this(fp, Set.copyOf(dependencies));
    }

    public Collection<FareProductLike> dependenciesMatchingCategoryAndMedium() {
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

package org.opentripplanner.model.fare;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * An interface for expressing the fares offered to a passengers. They can be straightforward,
 * like a single ticket, but also more complicated like tickets that are only valid when purchased
 *  together with another one.
 */
@Sandbox
public sealed interface FareOffer permits FareOffer.DefaultFareOffer, FareOffer.DependentFareOffer {
  static FareOffer of(FareProduct product, Collection<FareProduct> dependencies) {
    if (dependencies.isEmpty()) {
      return new DefaultFareOffer(product);
    } else {
      return new DependentFareOffer(
        product,
        dependencies.stream().map(DefaultFareOffer::new).collect(Collectors.toSet())
      );
    }
  }

  static FareOffer of(FareProduct product) {
    return new DefaultFareOffer(product);
  }

  default String uniqueInstanceId(ZonedDateTime zonedDateTime) {
    return fareProduct().uniqueInstanceId(zonedDateTime);
  }

  FareProduct fareProduct();

  /**
   * A fare product that has no dependencies on others and can be purchased on its own.
   */
  record DefaultFareOffer(FareProduct fareProduct) implements FareOffer {}

  /**
   * A fare product that is only valid together when purchased together with any of the products
   * in {@code dependencies}. These dependencies can also have dependencies on their own.
   */
  record DependentFareOffer(FareProduct fareProduct, Set<FareOffer> dependencies)
    implements FareOffer {
    public DependentFareOffer {
      Objects.requireNonNull(fareProduct);
      Objects.requireNonNull(dependencies);
      if (dependencies.isEmpty()) {
        throw new IllegalArgumentException("Dependencies must not be empty");
      }
    }

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

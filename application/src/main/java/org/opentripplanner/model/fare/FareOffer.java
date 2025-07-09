package org.opentripplanner.model.fare;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * An interface for expressing the fares offered to a passenger. They can be straightforward,
 * like a single ticket, but also more complicated like tickets that are only valid when purchased
 * together with another one.
 */
@Sandbox
public sealed interface FareOffer permits FareOffer.DefaultFareOffer, FareOffer.DependentFareOffer {
  static FareOffer of(
    ZonedDateTime startTime,
    FareProduct product,
    Collection<FareProduct> dependencies
  ) {
    if (dependencies.isEmpty()) {
      return FareOffer.of(startTime, product);
    } else {
      return new DependentFareOffer(
        startTime,
        product,
        dependencies.stream().map(fp -> FareOffer.of(startTime, fp)).collect(Collectors.toSet())
      );
    }
  }

  static FareOffer of(ZonedDateTime startTime, FareProduct product) {
    return new DefaultFareOffer(startTime, product);
  }

  String uniqueId();

  FareProduct fareProduct();

  ZonedDateTime startTime();

  /**
   * A fare product that has no dependencies on others and can be purchased on its own.
   */
  record DefaultFareOffer(ZonedDateTime startTime, FareProduct fareProduct) implements FareOffer {
    @Override
    public String uniqueId() {
      return fareProduct().uniqueInstanceId(startTime);
    }
  }

  /**
   * A fare product that is only valid together when purchased together with any of the products
   * in {@code dependencies}. These dependencies can also have dependencies on their own.
   */
  record DependentFareOffer(
    ZonedDateTime startTime,
    FareProduct fareProduct,
    Set<FareOffer> dependencies
  )
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

    @Override
    public String uniqueId() {
      return fareProduct().uniqueInstanceId(startTime);
    }
  }
}

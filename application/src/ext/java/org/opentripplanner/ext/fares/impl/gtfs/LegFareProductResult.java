package org.opentripplanner.ext.fares.impl.gtfs;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.model.fare.FareProduct;

public record LegFareProductResult(FareProduct transferProduct, Set<FareProduct> dependencies) {
  public LegFareProductResult(FareProduct transferProduct, Collection<FareProduct> dependencies) {
    this(transferProduct, Set.copyOf(dependencies));
  }
  public LegFareProductResult(FareProduct transferProduct) {
    this(transferProduct, Set.of());
  }
}

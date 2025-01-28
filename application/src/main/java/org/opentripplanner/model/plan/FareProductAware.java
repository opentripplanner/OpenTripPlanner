package org.opentripplanner.model.plan;

import java.util.List;
import org.opentripplanner.model.fare.FareProductUse;

/**
 * An interface to signal that an entity (typically a leg) can have fares attached to it.
 */
public interface FareProductAware<T> {
  /**
   * Returns a copy of the entity with fares added to it.
   */
  T decorateWithFareProducts(List<FareProductUse> fares);
}

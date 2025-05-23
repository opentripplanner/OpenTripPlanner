package org.opentripplanner.model.fare;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public record FareProductLike(FareProduct fareProduct, Collection<FareProductLike> dependencies) {
  public String uniqueInstanceId(ZonedDateTime zonedDateTime) {
    return fareProduct.uniqueInstanceId(zonedDateTime);
  }

  public boolean hasDependencies() {
    return !dependencies.isEmpty();
  }

  public Collection<FareProductLike> dependenciesMatchingCategoryAndMedium() {
    return dependencies
      .stream()
      .filter(
        fp ->
          Objects.equals(fp.fareProduct.category(), fareProduct.category()) &&
          Objects.equals(fp.fareProduct.medium(), fareProduct.medium())
      )
      .toList();
  }
}

package org.opentripplanner.ext.fares.service.gtfs.v2;

import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;

/// A rule for transferring from one leg to another one.
public record TransferMatch(
  FareTransferRule transferRule,
  FareLegRule fromLegRule,
  FareLegRule toLegRule
) {
  public boolean isFree() {
    return transferRule.isFree();
  }

  /// Is there a product in the transfer products that matches the given product's category and
  /// medium?
  /// If the list of transfer products is empty, then the transfer is eligible for all products.
  public boolean matchesEligibility(FareProduct product) {
    if (transferRule.fareProducts().isEmpty()) {
      return true;
    }
    return transferRule.fareProducts().stream().anyMatch(product::equalEligibility);
  }
}

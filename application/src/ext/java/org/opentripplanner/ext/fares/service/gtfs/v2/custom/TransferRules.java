package org.opentripplanner.ext.fares.service.gtfs.v2.custom;

import org.opentripplanner.ext.fares.service.gtfs.v2.FreeTransferEligibility;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.RiderCategory;

/// Special predicate for when free transfers apply in the HOP fare calculator. They apply when
/// the transfer rule has no category or medium specified, or when the ID (ignoring feed id) matches.
class TransferRules {

  static FreeTransferEligibility transferEligibility() {
    return (transferMatch, product) -> {
      if (transferMatch.transferRule().fareProducts().isEmpty()) {
        return true;
      } else {
        return transferMatch
          .transferRule()
          .fareProducts()
          .stream()
          .anyMatch(transferProduct -> {
            if (
              transferProduct.category() == null &&
              transferProduct.medium() == null &&
              transferProduct.isFree()
            ) {
              return true;
            } else {
              return (
                mediaMatches(transferProduct.medium(), product.medium()) &&
                categoryMatches(transferProduct.category(), product.category())
              );
            }
          });
      }
    };
  }

  private static boolean categoryMatches(RiderCategory cat1, RiderCategory cat2) {
    if (cat1 == null && cat2 == null) {
      return true;
    } else if (cat1 == null || cat2 == null) {
      return false;
    }
    return cat1.id().getId().equals(cat2.id().getId());
  }

  private static boolean mediaMatches(FareMedium medium, FareMedium medium1) {
    if (medium == null && medium1 == null) {
      return true;
    } else if (medium == null || medium1 == null) {
      return false;
    }
    return medium.id().getId().equals(medium1.id().getId());
  }
}

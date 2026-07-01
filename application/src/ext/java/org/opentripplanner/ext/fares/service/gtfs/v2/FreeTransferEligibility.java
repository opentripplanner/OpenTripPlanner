package org.opentripplanner.ext.fares.service.gtfs.v2;

import org.opentripplanner.model.fare.FareProduct;

/// Determines whether a free transfer applies to a given fare product.
///
/// The GTFS Fares V2 spec is underspecified about which fare products a free transfer should cover.
/// This interface allows custom fare services to plug in their own eligibility logic while the
/// default implementation ([FareLookupService#DEFAULT_FREE_TRANSFER_MATCH_PREDICATE]) matches
/// on equal rider category and fare medium.
///
/// @see <a href="https://github.com/google/transit/pull/423">GTFS spec discussion</a>
@FunctionalInterface
public interface FreeTransferEligibility {
  /// Returns `true` if the free transfer described by `transfer` applies to
  /// `product`.
  boolean test(TransferMatch transfer, FareProduct product);
}

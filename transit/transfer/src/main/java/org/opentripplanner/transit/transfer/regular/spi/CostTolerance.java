package org.opentripplanner.transit.transfer.regular.spi;

/**
 * Cost-only equivalence tolerance used when deciding whether a dependent profile's own path can
 * be deduplicated against its {@code base} profile's path (see {@link RegularTransferParameters#base()}).
 * {@code raptor-data} has no dependency on OTP's domain-model cost-function types, so this stays a
 * plain function of the base cost - the application layer supplies an implementation backed by
 * whatever config-driven cost function it likes (e.g. a linear {@code a + b*x} tolerance).
 */
@FunctionalInterface
public interface CostTolerance {
  /**
   * @param baseCost the base profile's path cost (Raptor cost units), re-costed under the
   *                 dependent profile's own {@code (profileId, preferences)}
   * @return the maximum allowed {@code |candidateCost - baseCost|} for the two to be treated as
   * equivalent
   */
  int toleranceFor(int baseCost);
}

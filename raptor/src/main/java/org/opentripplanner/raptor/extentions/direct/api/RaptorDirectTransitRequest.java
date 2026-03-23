package org.opentripplanner.raptor.direct.api;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/// All input parameters to do a direct search.
public final class RaptorDirectTransitRequest {

  private final int earliestDepartureTime;
  private final int searchWindowInSeconds;
  private final RelaxFunction relaxC1;
  private final Collection<RaptorAccessEgress> accessPaths;
  private final Collection<RaptorAccessEgress> egressPaths;

  private RaptorDirectTransitRequest() {
    this.earliestDepartureTime = RaptorConstants.TIME_NOT_SET;
    this.searchWindowInSeconds = RaptorConstants.NOT_SET;
    this.relaxC1 = GeneralizedCostRelaxFunction.NORMAL;
    this.accessPaths = List.of();
    this.egressPaths = List.of();
  }

  public RaptorDirectTransitRequest(
    int earliestDepartureTime,
    int searchWindowInSeconds,
    RelaxFunction relaxC1,
    Collection<RaptorAccessEgress> accessPaths,
    Collection<RaptorAccessEgress> egressPaths
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.searchWindowInSeconds = searchWindowInSeconds;
    this.relaxC1 = Objects.requireNonNull(relaxC1);
    this.accessPaths = Objects.requireNonNull(accessPaths);
    this.egressPaths = Objects.requireNonNull(egressPaths);
    verify();
  }

  public static RaptorDirectTransitRequest defaults() {
    return new RaptorDirectTransitRequest();
  }

  public static RaptorDirectTransitRequestBuilder of() {
    return new RaptorDirectTransitRequestBuilder(defaults());
  }

  /// The earliest a journey can depart from the origin. The unit is seconds since midnight.
  /// Inclusive.
  ///
  /// In the case of a 'depart after' search this is a required. In the case of a 'arrive by' search
  /// this is optional, but it will improve performance if it is set.
  public int earliestDepartureTime() {
    return earliestDepartureTime;
  }

  public boolean isEarliestDepartureTimeSet() {
    return earliestDepartureTime != RaptorConstants.TIME_NOT_SET;
  }

  /// The time window used to search. The unit is seconds.
  ///
  /// For a *depart-by-search*, this is added to the 'earliestDepartureTime' to find the
  /// 'latestDepartureTime'.
  ///
  /// For an *arrive-by-search* this is used to calculate the 'earliestArrivalTime'. The algorithm
  /// will find all optimal travels within the given time window.
  ///
  /// Set the search window to 0 (zero) to run 1 iteration.
  ///
  /// Required. Must be a positive integer or 0(zero).
  public int searchWindowInSeconds() {
    return searchWindowInSeconds;
  }

  /// The relax function specifies which paths to include.
  ///
  /// A relax function of `2x + 10m` will include paths that have a c1 cost up to 2 times plus 10
  /// minutes compared to the cheapest path. I.e. if the cheapest path has a cost of 100 min the
  /// results will include paths with a cost 210 min.
  public RelaxFunction relaxC1() {
    return relaxC1;
  }

  /// List of access paths from the origin to all transit stops using the street network.
  ///
  /// Required, at least one access path must exist.
  public Collection<RaptorAccessEgress> accessPaths() {
    return accessPaths;
  }

  /// List of all possible egress paths to reach the destination using the street network.
  ///
  /// NOTE! The {@link RaptorTransfer#stop()} is the stop where the egress path start, NOT the
  /// destination - think of it as a reversed path.
  ///
  /// Required, at least one egress path must exist.
  public Collection<RaptorAccessEgress> egressPaths() {
    return egressPaths;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      earliestDepartureTime,
      searchWindowInSeconds,
      relaxC1,
      accessPaths,
      egressPaths
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof RaptorDirectTransitRequest that) {
      return (
        earliestDepartureTime == that.earliestDepartureTime &&
        searchWindowInSeconds == that.searchWindowInSeconds &&
        relaxC1.equals(that.relaxC1) &&
        accessPaths.equals(that.accessPaths) &&
        egressPaths.equals(that.egressPaths)
      );
    }
    return false;
  }

  @Override
  public String toString() {
    var dft = defaults();
    return ToStringBuilder.of(RaptorDirectTransitRequest.class)
      .addServiceTime("earliestDepartureTime", earliestDepartureTime, dft.earliestDepartureTime)
      .addDurationSec("searchWindow", searchWindowInSeconds, dft.searchWindowInSeconds)
      .addObj("relaxC1", relaxC1, dft.relaxC1)
      .addCollection("accessPaths", accessPaths, 5, RaptorAccessEgress::defaultToString)
      .addCollection("egressPaths", egressPaths, 5, RaptorAccessEgress::defaultToString)
      .toString();
  }

  /* private methods */
  private void verify() {
    assertProperty(isEarliestDepartureTimeSet(), "'earliestDepartureTime' is required.");
    assertProperty(!accessPaths.isEmpty(), "At least one 'accessPath' is required.");
    assertProperty(!egressPaths.isEmpty(), "At least one 'egressPath' is required.");
  }

  private static void assertProperty(boolean predicate, String message) {
    if (!predicate) {
      throw new IllegalArgumentException(message);
    }
  }
}

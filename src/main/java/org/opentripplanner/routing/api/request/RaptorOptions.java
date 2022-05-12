package org.opentripplanner.routing.api.request;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.util.lang.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;

/**
 * Set of optimizations to use with Raptor. These are available here for testing purposes.
 */
public class RaptorOptions implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Set<Optimization> optimizations = EnumSet.of(
    Optimization.PARETO_CHECK_AGAINST_DESTINATION
  );

  private RaptorProfile profile = RaptorProfile.MULTI_CRITERIA;

  private SearchDirection searchDirection = SearchDirection.FORWARD;

  /**
   * If set this is used to set the latest-arrival-time in Raptor for arriveBy=false, and to set the
   * earliest-departure-time in Raptor for arriveBy=true.
   */
  private Instant timeLimit = null;

  public RaptorOptions() {}

  public RaptorOptions(RaptorOptions other) {
    withOptimizations(other.optimizations);
    withProfile(other.profile);
    withSearchDirection(other.searchDirection);
    withTimeLimit(other.timeLimit);
  }

  public Set<Optimization> getOptimizations() {
    return optimizations;
  }

  public RaptorOptions withOptimizations(Collection<Optimization> optimizations) {
    Objects.requireNonNull(optimizations);
    this.optimizations.clear();
    this.optimizations.addAll(optimizations);
    return this;
  }

  public RaptorProfile getProfile() {
    return profile;
  }

  public RaptorOptions withProfile(RaptorProfile profile) {
    Objects.requireNonNull(profile);
    this.profile = profile;
    return this;
  }

  public SearchDirection getSearchDirection() {
    return searchDirection;
  }

  public RaptorOptions withSearchDirection(SearchDirection searchDirection) {
    Objects.requireNonNull(searchDirection);
    this.searchDirection = searchDirection;
    return this;
  }

  public Instant getTimeLimit() {
    return timeLimit;
  }

  public RaptorOptions withTimeLimit(Instant timeLimit) {
    this.timeLimit = timeLimit;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(RaptorOptions.class)
      .addCol("optimizations", optimizations)
      .addEnum("profile", profile)
      .addEnum("searchDirection", searchDirection)
      .addTime("timeLimit", timeLimit)
      .toString();
  }
}

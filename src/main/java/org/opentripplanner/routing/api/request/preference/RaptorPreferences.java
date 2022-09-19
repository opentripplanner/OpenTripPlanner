package org.opentripplanner.routing.api.request.preference;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.transit.SearchDirection;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * Set of optimizations to use with Raptor. These are available here for testing purposes.
 */
public class RaptorPreferences implements Serializable {

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

  public RaptorPreferences() {}

  public RaptorPreferences(RaptorPreferences other) {
    withOptimizations(other.optimizations);
    withProfile(other.profile);
    withSearchDirection(other.searchDirection);
    withTimeLimit(other.timeLimit);
  }

  public Set<Optimization> getOptimizations() {
    return optimizations;
  }

  public RaptorPreferences withOptimizations(Collection<Optimization> optimizations) {
    Objects.requireNonNull(optimizations);
    this.optimizations.clear();
    this.optimizations.addAll(optimizations);
    return this;
  }

  public RaptorProfile getProfile() {
    return profile;
  }

  public RaptorPreferences withProfile(RaptorProfile profile) {
    Objects.requireNonNull(profile);
    this.profile = profile;
    return this;
  }

  public SearchDirection getSearchDirection() {
    return searchDirection;
  }

  public RaptorPreferences withSearchDirection(SearchDirection searchDirection) {
    Objects.requireNonNull(searchDirection);
    this.searchDirection = searchDirection;
    return this;
  }

  public Instant getTimeLimit() {
    return timeLimit;
  }

  public RaptorPreferences withTimeLimit(Instant timeLimit) {
    this.timeLimit = timeLimit;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(RaptorPreferences.class)
      .addCol("optimizations", optimizations)
      .addEnum("profile", profile)
      .addEnum("searchDirection", searchDirection)
      .addDateTime("timeLimit", timeLimit)
      .toString();
  }
}

package org.opentripplanner.raptor.api.request;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * This is a Request builder to help construct valid requests. Se the request classes for
 * documentation on each parameter.
 * <p/>
 * <ul>
 *     <li>{@link RaptorRequest}
 *     <li>{@link DebugRequest}
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorRequestBuilder<T extends RaptorTripSchedule> {

  // Search
  private final SearchParamsBuilder<T> searchParams;
  private final Set<Optimization> optimizations = EnumSet.noneOf(Optimization.class);

  // Debug
  private final DebugRequestBuilder debug;
  private SearchDirection searchDirection;
  private RaptorSlackProvider slackProvider;

  // Performance monitoring
  private RaptorTimers performanceTimers;

  // Algorithm
  private RaptorProfile profile;

  /**
   * A flag used to prevent fields part of the alias to be modified after the alias is generated.
   */
  private boolean freezeAliasFields = false;

  public RaptorRequestBuilder() {
    this(RaptorRequest.defaults());
  }

  RaptorRequestBuilder(RaptorRequest<T> defaults) {
    this.searchParams = new SearchParamsBuilder<>(this, defaults.searchParams());
    this.searchDirection = defaults.searchDirection();
    this.slackProvider = defaults.slackProvider();

    // Algorithm
    this.profile = defaults.profile();
    this.optimizations.addAll(defaults.optimizations());

    // Timer
    this.performanceTimers = defaults.performanceTimers();

    // Debug
    this.debug = new DebugRequestBuilder(defaults.debug());
  }

  public SearchParamsBuilder<T> searchParams() {
    return searchParams;
  }

  public RaptorProfile profile() {
    return profile;
  }

  public RaptorRequestBuilder<T> profile(RaptorProfile profile) {
    verifyAliasNotGeneratedYet();
    this.profile = profile;
    return this;
  }

  public SearchDirection searchDirection() {
    return searchDirection;
  }

  public RaptorRequestBuilder<T> searchDirection(SearchDirection searchDirection) {
    verifyAliasNotGeneratedYet();
    this.searchDirection = searchDirection;
    return this;
  }

  public RaptorSlackProvider slackProvider() {
    return slackProvider;
  }

  public void slackProvider(@Nonnull RaptorSlackProvider slackProvider) {
    this.slackProvider = slackProvider;
  }

  public Collection<Optimization> optimizations() {
    return optimizations;
  }

  public RaptorRequestBuilder<T> enableOptimization(Optimization optimization) {
    verifyAliasNotGeneratedYet();
    this.optimizations.add(optimization);
    return this;
  }

  public RaptorRequestBuilder<T> clearOptimizations() {
    this.optimizations.clear();
    return this;
  }

  public RaptorRequestBuilder<T> disableOptimization(Optimization optimization) {
    this.optimizations.remove(optimization);
    return this;
  }

  public RaptorTimers performanceTimers() {
    return performanceTimers;
  }

  public RaptorRequestBuilder<T> performanceTimers(RaptorTimers performanceTimers) {
    this.performanceTimers = performanceTimers;
    return this;
  }

  public DebugRequestBuilder debug() {
    return this.debug;
  }

  public RaptorRequest<T> build() {
    return new RaptorRequest<>(this);
  }

  /**
   * Generate a name to the RaptorRouting request that can be used in debugging, logging
   * and/or performance monitoring.
   * <p>
   * Note! The {@code profile}, {@code searchDirection}, {@code optimizations} is used to
   * make a unique alias - so set them before calling this method, if not an exception is thrown.
   */
  public String generateAlias() {
    this.freezeAliasFields = true;
    return generateRequestAlias(profile, searchDirection, optimizations);
  }

  static String generateRequestAlias(
    RaptorProfile profile,
    SearchDirection searchDirection,
    Collection<Optimization> optimizations
  ) {
    String name = profile.abbreviation();

    if (searchDirection.isInReverse()) {
      name += "-Rev";
    }
    if (Optimization.PARALLEL.isOneOf(optimizations)) {
      // Run search in parallel
      name += "-LL";
    }
    if (Optimization.PARETO_CHECK_AGAINST_DESTINATION.isOneOf(optimizations)) {
      // Heuristic to prune on pareto optimal Destination arrivals
      name += "-DP";
    }
    return name;
  }

  private void verifyAliasNotGeneratedYet() {
    if (freezeAliasFields) {
      throw new IllegalStateException(
        "The alias is generated before one of the fileds it relay on is set."
      );
    }
  }
}

package org.opentripplanner.raptor.api.request;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.raptor.api.debug.RaptorTimers;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All input parameters to RangeRaptor that is specific to a routing request.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorRequest<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorRequest.class);

  private final String alias;
  private final SearchParams searchParams;
  private final RaptorProfile profile;
  private final SearchDirection searchDirection;
  private final Set<Optimization> optimizations;
  private final MultiCriteriaRequest<T> multiCriteria;
  private final DebugRequest debug;
  private final RaptorTimers performanceTimers;

  private RaptorRequest() {
    searchParams = SearchParams.defaults();
    profile = RaptorProfile.MULTI_CRITERIA;
    searchDirection = SearchDirection.FORWARD;
    optimizations = Collections.emptySet();
    multiCriteria = MultiCriteriaRequest.<T>of().build();
    performanceTimers = RaptorTimers.NOOP;
    debug = DebugRequest.defaults();
    alias = RaptorRequestBuilder.generateRequestAlias(profile, searchDirection, optimizations);
  }

  RaptorRequest(RaptorRequestBuilder<T> builder) {
    this.alias = builder.generateAlias();
    this.searchParams = builder.searchParams().buildSearchParam();
    this.profile = builder.profile();
    this.searchDirection = builder.searchDirection();
    this.optimizations = Set.copyOf(builder.optimizations());
    this.multiCriteria = builder.multiCriteria();
    this.performanceTimers = builder.performanceTimers();
    this.debug = builder.debug().build();
    verify();
  }

  public RaptorRequestBuilder<T> mutate() {
    return new RaptorRequestBuilder<>(this);
  }

  /**
   * A unique short name for the request based on {@code profile}, {@code searchDirection},
   * {@code optimizations} for use in logging, debugging and performance monitoring. It is
   * not ment to identify a request, but it is used to group requests of the same "type".
   */
  public String alias() {
    return alias;
  }

  /**
   * Required travel search parameters.
   */
  public SearchParams searchParams() {
    return searchParams;
  }

  /**
   * Return true is Raptor will use constrained transfers. Raptor will only use constrained
   * transfers if the {@link RaptorProfile} allowes it and the
   * {@link SearchParams#constrainedTransfers()} is turned on.
   */
  public boolean useConstrainedTransfers() {
    return profile.supportsConstrainedTransfers() && searchParams.constrainedTransfers();
  }

  /**
   * A dynamic search is a search which uses heuristics to resolve search parameters as
   * earliest-departure-time, latest-arrival-time and search-window. This is an aggregated value:
   * <ul>
   *     <li>A multi-criteria search is a dynamic search.</li>
   *     <li>A standard range-raptor search with more than one iteration.</li>
   * </ul>
   * In principle any search could be run using dynamic resolving of EDT, LAT and search-window,
   * but for other "simpler" searches we would rather have it fail than magically run, if
   * configured wrong.
   */
  public boolean isDynamicSearch() {
    if (profile().is(RaptorProfile.MULTI_CRITERIA)) {
      return true;
    }
    if (profile.is(RaptorProfile.STANDARD)) {
      return !searchParams().searchOneIterationOnly();
    }
    return false;
  }

  /**
   * The profile/algorithm to use for this request.
   * <p/>
   * The default value is {@link RaptorProfile#MULTI_CRITERIA}
   */
  public RaptorProfile profile() {
    return profile;
  }

  public SearchDirection searchDirection() {
    return searchDirection;
  }

  /**
   * Return list of enabled optimizations.
   */
  public Collection<Optimization> optimizations() {
    return optimizations;
  }

  /**
   * TRUE if the given optimization is enabled.
   */
  public boolean optimizationEnabled(Optimization optimization) {
    return optimization.isOneOf(optimizations);
  }

  public boolean useDestinationPruning() {
    return optimizationEnabled(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
  }

  public boolean runInParallel() {
    return optimizationEnabled(Optimization.PARALLEL);
  }

  public MultiCriteriaRequest<T> multiCriteria() {
    return multiCriteria;
  }

  public RaptorTimers performanceTimers() {
    return performanceTimers;
  }

  /**
   * Specify what to debug in the debug request.
   * <p/>
   * This feature is optional, by default debugging is turned off.
   */
  public DebugRequest debug() {
    return debug;
  }

  @Override
  public int hashCode() {
    return Objects.hash(profile, searchParams, multiCriteria, debug);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaptorRequest<?> that = (RaptorRequest<?>) o;
    return (
      profile == that.profile &&
      Objects.equals(searchParams, that.searchParams) &&
      Objects.equals(multiCriteria, that.multiCriteria) &&
      Objects.equals(debug, that.debug)
    );
  }

  @Override
  public String toString() {
    var defaults = RaptorRequest.defaults();
    return ToStringBuilder.of(RaptorRequest.class)
      .addEnum("profile", profile)
      .addBoolIfTrue("reverse", searchDirection.isInReverse())
      .addCol("optimizations", optimizations, defaults.optimizations())
      .addObj("searchParams", searchParams)
      .addObj("multiCriteria", multiCriteria, defaults.multiCriteria())
      .addObj("debug", debug, defaults.debug())
      .addBoolIfTrue("withPerformanceTimers", performanceTimers != RaptorTimers.NOOP)
      .toString();
  }

  static <T extends RaptorTripSchedule> RaptorRequest<T> defaults() {
    return new RaptorRequest<>();
  }

  static void assertProperty(boolean predicate, String message) {
    if (!predicate) {
      throw new IllegalArgumentException(message);
    }
  }

  /* private methods */

  private void verify() {
    searchParams.verify();
    if (!profile.is(RaptorProfile.MULTI_CRITERIA)) {
      if (useDestinationPruning()) {
        LOG.warn("Destination pruning is only supported using McRangeRaptor");
      }
    }
  }
}

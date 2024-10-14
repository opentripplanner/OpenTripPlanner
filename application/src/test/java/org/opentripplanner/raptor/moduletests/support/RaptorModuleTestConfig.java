package org.opentripplanner.raptor.moduletests.support;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;

/**
 * This enum is a list of all relevant ways to configure Raptor with
 * <ol>
 *   <li>{@link RaptorProfile}</li>
 *   <li>Run one iteration(just Raptor) or many iterations(Range Raptor)</li>
 *   <li>Search forward from origin to destination, or in reverse from destination to origin</li>
 *   <li>Optimization (Multi-criteria destination pruning only)</li>
 * </ol>
 * Not all combinations are allowed, and this list only contains allowed configurations.
 */
public enum RaptorModuleTestConfig {
  TC_STANDARD(STANDARD, false, false),
  TC_STANDARD_ONE(STANDARD, true, false),
  TC_STANDARD_REV(STANDARD, false, true),
  TC_STANDARD_REV_ONE(STANDARD, true, true),
  TC_MIN_DURATION(MIN_TRAVEL_DURATION, true, false),
  TC_MIN_DURATION_REV(MIN_TRAVEL_DURATION, true, true),
  TC_MULTI_CRITERIA(MULTI_CRITERIA, false, false),
  TC_MULTI_CRITERIA_DEST_PRUNING(MULTI_CRITERIA, false, false);

  private final RaptorProfile profile;
  private final boolean oneIteration;
  private final boolean reverse;

  public static final List<RaptorModuleTestConfig> STANDARD_LIST = List.of(
    TC_STANDARD,
    TC_STANDARD_ONE,
    TC_STANDARD_REV,
    TC_STANDARD_REV_ONE
  );
  public static final List<RaptorModuleTestConfig> MIN_DURATION_LIST = List.of(
    TC_MIN_DURATION,
    TC_MIN_DURATION_REV
  );
  public static final List<RaptorModuleTestConfig> MULTI_CRITERIA_LIST = List.of(
    TC_MULTI_CRITERIA,
    TC_MULTI_CRITERIA_DEST_PRUNING
  );

  RaptorModuleTestConfig(RaptorProfile profile, boolean oneIteration, boolean reverse) {
    this.profile = profile;
    this.oneIteration = oneIteration;
    this.reverse = reverse;
  }

  RaptorProfile profile() {
    return profile;
  }

  boolean withOneIteration() {
    return oneIteration;
  }

  boolean withManyIterations() {
    return !oneIteration;
  }

  boolean isReverse() {
    return reverse;
  }

  boolean isForward() {
    return !isReverse();
  }

  public static RaptorModuleTestConfigSetBuilder standard() {
    return new RaptorModuleTestConfigSetBuilder(STANDARD_LIST);
  }

  public static RaptorModuleTestConfigSetBuilder minDuration() {
    return new RaptorModuleTestConfigSetBuilder(MIN_DURATION_LIST);
  }

  public static RaptorModuleTestConfigSetBuilder multiCriteria() {
    return new RaptorModuleTestConfigSetBuilder(MULTI_CRITERIA_LIST);
  }

  public <T extends RaptorTripSchedule> RaptorRequestBuilder<T> apply(
    RaptorRequestBuilder<T> builder
  ) {
    builder.profile(profile);
    if (oneIteration) {
      builder.searchParams().searchOneIterationOnly();
    }
    if (reverse) {
      builder.searchDirection(SearchDirection.REVERSE);
    }
    if (this == TC_MULTI_CRITERIA_DEST_PRUNING) {
      builder.enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    }
    return builder;
  }
}

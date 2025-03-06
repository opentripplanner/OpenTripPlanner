package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Set of optimizations to use with Raptor. These are available here for testing purposes.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class RaptorPreferences implements Serializable {

  public static final RaptorPreferences DEFAULT = new RaptorPreferences();
  private static final double MIN_RELAX_COST_AT_DESTINATION = 1.0;
  private static final double MAX_RELAX_COST_AT_DESTINATION = 2.0;

  private final Set<Optimization> optimizations;

  private final RaptorProfile profile;

  private final SearchDirection searchDirection;

  private final Instant timeLimit;
  private final Double relaxGeneralizedCostAtDestination;

  private RaptorPreferences() {
    this.optimizations = EnumSet.of(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    this.profile = RaptorProfile.MULTI_CRITERIA;
    this.searchDirection = SearchDirection.FORWARD;
    this.timeLimit = null;
    this.relaxGeneralizedCostAtDestination = null;
  }

  private RaptorPreferences(RaptorPreferences.Builder builder) {
    this.optimizations = Collections.unmodifiableSet(builder.copyOptimizations());
    this.profile = Objects.requireNonNull(builder.profile);
    this.searchDirection = Objects.requireNonNull(builder.searchDirection);
    this.timeLimit = builder.timeLimit;
    this.relaxGeneralizedCostAtDestination = Units.normalizedOptionalFactor(
      builder.relaxGeneralizedCostAtDestination,
      MIN_RELAX_COST_AT_DESTINATION,
      MAX_RELAX_COST_AT_DESTINATION
    );
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public Set<Optimization> optimizations() {
    return optimizations;
  }

  public RaptorProfile profile() {
    return profile;
  }

  public SearchDirection searchDirection() {
    return searchDirection;
  }

  /**
   * If set this is used to set the latest-arrival-time in Raptor for arriveBy=false, and to set the
   * earliest-departure-time in Raptor for arriveBy=true.
   */
  @Nullable
  public Instant timeLimit() {
    return timeLimit;
  }

  /**
   * See {@link SearchParams#relaxCostAtDestination()} for documentation.
   */
  @Deprecated
  public Optional<Double> relaxGeneralizedCostAtDestination() {
    return Optional.ofNullable(relaxGeneralizedCostAtDestination);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaptorPreferences that = (RaptorPreferences) o;

    return (
      optimizations.equals(that.optimizations) &&
      profile == that.profile &&
      searchDirection == that.searchDirection &&
      Objects.equals(timeLimit, that.timeLimit) &&
      Objects.equals(relaxGeneralizedCostAtDestination, that.relaxGeneralizedCostAtDestination)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      optimizations,
      profile,
      searchDirection,
      timeLimit,
      relaxGeneralizedCostAtDestination
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RaptorPreferences.class)
      .addCol("optimizations", optimizations, DEFAULT.optimizations)
      .addEnum("profile", profile, DEFAULT.profile)
      .addEnum("searchDirection", searchDirection, DEFAULT.searchDirection)
      // Ignore time limit if null (default value)
      .addDateTime("timeLimit", timeLimit)
      .addNum(
        "relaxGeneralizedCostAtDestination",
        relaxGeneralizedCostAtDestination,
        DEFAULT.relaxGeneralizedCostAtDestination
      )
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final RaptorPreferences original;
    private RaptorProfile profile;
    private SearchDirection searchDirection;
    private Set<Optimization> optimizations;
    private Instant timeLimit;
    private Double relaxGeneralizedCostAtDestination;

    public Builder(RaptorPreferences original) {
      this.original = original;
      this.profile = original.profile;
      this.searchDirection = original.searchDirection;
      this.optimizations = null;
      this.timeLimit = original.timeLimit;
      this.relaxGeneralizedCostAtDestination = original.relaxGeneralizedCostAtDestination;
    }

    public Builder withOptimizations(Collection<Optimization> optimizations) {
      this.optimizations = optimizations.isEmpty()
        ? EnumSet.noneOf(Optimization.class)
        : EnumSet.copyOf(optimizations);
      return this;
    }

    public Builder withProfile(RaptorProfile profile) {
      this.profile = profile;
      return this;
    }

    public Builder withSearchDirection(SearchDirection searchDirection) {
      this.searchDirection = searchDirection;
      return this;
    }

    public Builder withTimeLimit(Instant timeLimit) {
      this.timeLimit = timeLimit;
      return this;
    }

    public Builder withRelaxGeneralizedCostAtDestination(
      @Nullable Double relaxGeneralizedCostAtDestination
    ) {
      this.relaxGeneralizedCostAtDestination = relaxGeneralizedCostAtDestination;
      return this;
    }

    public RaptorPreferences build() {
      var value = new RaptorPreferences(this);
      return original.equals(value) ? original : value;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    private Set<Optimization> copyOptimizations() {
      return optimizations == null ? original.optimizations : optimizations;
    }
  }
}

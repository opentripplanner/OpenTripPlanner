package org.opentripplanner.routing.api.request.preference;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Preferences for transit routing.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TransitPreferences implements Serializable {

  public static final TransitPreferences DEFAULT = new TransitPreferences();

  private final DurationForEnum<TransitMode> boardSlack;
  private final DurationForEnum<TransitMode> alightSlack;
  private final Map<TransitMode, Double> reluctanceForMode;
  private final Cost otherThanPreferredRoutesPenalty;
  private final CostLinearFunction unpreferredCost;
  private final CostLinearFunction relaxTransitGroupPriority;
  private final boolean ignoreRealtimeUpdates;
  private final boolean includePlannedCancellations;
  private final boolean includeRealtimeCancellations;
  private final RaptorPreferences raptor;

  private TransitPreferences() {
    this.boardSlack = this.alightSlack = DurationForEnum.of(TransitMode.class).build();
    this.reluctanceForMode = Map.of();
    this.otherThanPreferredRoutesPenalty = Cost.costOfMinutes(5);
    this.unpreferredCost = CostLinearFunction.NORMAL;
    this.relaxTransitGroupPriority = CostLinearFunction.NORMAL;
    this.ignoreRealtimeUpdates = false;
    this.includePlannedCancellations = false;
    this.includeRealtimeCancellations = false;
    this.raptor = RaptorPreferences.DEFAULT;
  }

  private TransitPreferences(Builder builder) {
    this.boardSlack = requireNonNull(builder.boardSlack);
    this.alightSlack = requireNonNull(builder.alightSlack);
    this.reluctanceForMode = Map.copyOf(requireNonNull(builder.reluctanceForMode));
    this.otherThanPreferredRoutesPenalty = builder.otherThanPreferredRoutesPenalty;
    this.unpreferredCost = requireNonNull(builder.unpreferredCost);
    this.relaxTransitGroupPriority = Objects.requireNonNull(builder.relaxTransitGroupPriority);
    this.ignoreRealtimeUpdates = builder.ignoreRealtimeUpdates;
    this.includePlannedCancellations = builder.includePlannedCancellations;
    this.includeRealtimeCancellations = builder.includeRealtimeCancellations;
    this.raptor = requireNonNull(builder.raptor);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Has information how much time boarding a vehicle takes; The number of seconds to add before
   * boarding a transit leg. Can be significant for airplanes or ferries. It is recommended to use
   * the `boardTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Board-slack can be configured per mode, if not set for a given mode it falls back to the
   * default value. This enables configuring the board-slack for airplane boarding to be 30 minutes
   * and a 2 minutes slack for everything else.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  public DurationForEnum<TransitMode> boardSlack() {
    return boardSlack;
  }

  /**
   * Has information how much time alighting a vehicle takes; The number of seconds to add after
   * alighting a transit leg. Can be significant for airplanes or ferries.  It is recommended to
   * use the `alightTimes` in the `router-config.json` to set this for each mode.
   * <p>
   * Alight-slack can be configured per mode. The default value is used if not set for a given mode.
   * This enables configuring the alight-slack for train alighting to be 4 minutes and a bus alight
   * slack to be 0 minutes.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  public DurationForEnum<TransitMode> alightSlack() {
    return alightSlack;
  }

  /**
   * Transit reluctance per mode. Use this to add an advantage(<1.0) to specific modes, or to add a
   * penalty to other modes (> 1.0). The type used here is the internal model {@link TransitMode}
   * make sure to create a mapping for this before using it on the API.
   * <p>
   * If set, it overrides the default value {@code 1.0}.
   * <p>
   * This is a scalar multiplied with the time in second on-board the transit vehicle. Default value
   * is not-set(empty map).
   * <p>
   * The returned map is READ-ONLY and IMMUTABLE. The map is not an EnumMap(mutable), so convert
   * the type into something more performant if needed.
   */
  public Map<TransitMode, Double> reluctanceForMode() {
    return reluctanceForMode;
  }

  /**
   * Penalty added for using every route that is not preferred if user set any route as preferred.
   * We return number of seconds that we are willing to wait for preferred route.
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  public int otherThanPreferredRoutesPenalty() {
    return otherThanPreferredRoutesPenalty.toSeconds();
  }

  public CostLinearFunction unpreferredCost() {
    return unpreferredCost;
  }

  /**
   * This is used to relax the cost when comparing transit-groups. The default is the
   * NORMAL function({@code f(t) = t}. This is the same as not using priority-groups. The
   * coefficient must be in range {@code [1.0 to 4.0]} and the constant must be in range
   * {@code [$0 to $1440(4h)]}.
   */
  public CostLinearFunction relaxTransitGroupPriority() {
    return relaxTransitGroupPriority;
  }

  public boolean isRelaxTransitGroupPrioritySet() {
    return !relaxTransitGroupPriority.isNormal();
  }

  /**
   * When true, real-time updates are ignored during this search.
   */
  public boolean ignoreRealtimeUpdates() {
    return ignoreRealtimeUpdates;
  }

  /**
   * When true, trips cancelled in scheduled data are included in this search.
   */
  public boolean includePlannedCancellations() {
    return includePlannedCancellations;
  }

  /**
   * When true, trips cancelled in by real-time updates are included in this search.
   */
  public boolean includeRealtimeCancellations() {
    return includeRealtimeCancellations;
  }

  /**
   * Set of options to use with Raptor. These are available here for testing purposes.
   */
  public RaptorPreferences raptor() {
    return raptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TransitPreferences that = (TransitPreferences) o;
    return (
      boardSlack.equals(that.boardSlack) &&
      alightSlack.equals(that.alightSlack) &&
      reluctanceForMode.equals(that.reluctanceForMode) &&
      Objects.equals(otherThanPreferredRoutesPenalty, that.otherThanPreferredRoutesPenalty) &&
      unpreferredCost.equals(that.unpreferredCost) &&
      Objects.equals(relaxTransitGroupPriority, that.relaxTransitGroupPriority) &&
      ignoreRealtimeUpdates == that.ignoreRealtimeUpdates &&
      includePlannedCancellations == that.includePlannedCancellations &&
      includeRealtimeCancellations == that.includeRealtimeCancellations &&
      raptor.equals(that.raptor)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      boardSlack,
      alightSlack,
      reluctanceForMode,
      otherThanPreferredRoutesPenalty,
      unpreferredCost,
      relaxTransitGroupPriority,
      ignoreRealtimeUpdates,
      includePlannedCancellations,
      includeRealtimeCancellations,
      raptor
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransitPreferences.class)
      .addObj("boardSlack", boardSlack, DEFAULT.boardSlack)
      .addObj("alightSlack", alightSlack, DEFAULT.alightSlack)
      .addObj("reluctanceForMode", reluctanceForMode, DEFAULT.reluctanceForMode)
      .addObj(
        "otherThanPreferredRoutesPenalty",
        otherThanPreferredRoutesPenalty,
        DEFAULT.otherThanPreferredRoutesPenalty
      )
      .addObj("unpreferredCost", unpreferredCost, DEFAULT.unpreferredCost)
      .addObj("relaxTransitGroupPriority", relaxTransitGroupPriority, CostLinearFunction.NORMAL)
      .addBoolIfTrue(
        "ignoreRealtimeUpdates",
        ignoreRealtimeUpdates != DEFAULT.ignoreRealtimeUpdates
      )
      .addBoolIfTrue(
        "includePlannedCancellations",
        includePlannedCancellations != DEFAULT.includePlannedCancellations
      )
      .addBoolIfTrue(
        "includeRealtimeCancellations",
        includeRealtimeCancellations != DEFAULT.includeRealtimeCancellations
      )
      .addObj("raptor", raptor, DEFAULT.raptor)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final TransitPreferences original;

    private DurationForEnum<TransitMode> boardSlack;
    private DurationForEnum<TransitMode> alightSlack;
    private Map<TransitMode, Double> reluctanceForMode;
    private Cost otherThanPreferredRoutesPenalty;
    private CostLinearFunction unpreferredCost;
    private CostLinearFunction relaxTransitGroupPriority;
    private boolean ignoreRealtimeUpdates;
    private boolean includePlannedCancellations;
    private boolean includeRealtimeCancellations;
    private RaptorPreferences raptor;

    public Builder(TransitPreferences original) {
      this.original = original;
      this.boardSlack = original.boardSlack;
      this.alightSlack = original.alightSlack;
      this.reluctanceForMode = original.reluctanceForMode;
      this.otherThanPreferredRoutesPenalty = original.otherThanPreferredRoutesPenalty;
      this.unpreferredCost = original.unpreferredCost;
      this.relaxTransitGroupPriority = original.relaxTransitGroupPriority;
      this.ignoreRealtimeUpdates = original.ignoreRealtimeUpdates;
      this.includePlannedCancellations = original.includePlannedCancellations;
      this.includeRealtimeCancellations = original.includeRealtimeCancellations;
      this.raptor = original.raptor;
    }

    public TransitPreferences original() {
      return original;
    }

    public Builder withBoardSlack(Consumer<DurationForEnum.Builder<TransitMode>> body) {
      this.boardSlack = this.boardSlack.copyOf().apply(body).build();
      return this;
    }

    public Builder withDefaultBoardSlackSec(int defaultValue) {
      return withBoardSlack(it -> it.withDefaultSec(defaultValue));
    }

    public Builder withAlightSlack(Consumer<DurationForEnum.Builder<TransitMode>> body) {
      this.alightSlack = this.alightSlack.copyOf().apply(body).build();
      return this;
    }

    public Builder withDefaultAlightSlackSec(int defaultValue) {
      return withAlightSlack(it -> it.withDefaultSec(defaultValue));
    }

    public Builder setReluctanceForMode(Map<TransitMode, Double> reluctanceForMode) {
      this.reluctanceForMode = reluctanceForMode;
      return this;
    }

    @Deprecated
    public Builder setOtherThanPreferredRoutesPenalty(int otherThanPreferredRoutesPenalty) {
      this.otherThanPreferredRoutesPenalty = Cost.costOfSeconds(otherThanPreferredRoutesPenalty);
      return this;
    }

    public Builder setUnpreferredCost(CostLinearFunction unpreferredCost) {
      this.unpreferredCost = unpreferredCost;
      return this;
    }

    public Builder setUnpreferredCostString(String constFunction) {
      return setUnpreferredCost(CostLinearFunction.of(constFunction));
    }

    public Builder withRelaxTransitGroupPriority(CostLinearFunction value) {
      this.relaxTransitGroupPriority = value;
      return this;
    }

    public Builder setIgnoreRealtimeUpdates(boolean ignoreRealtimeUpdates) {
      this.ignoreRealtimeUpdates = ignoreRealtimeUpdates;
      return this;
    }

    public Builder setIncludePlannedCancellations(boolean includePlannedCancellations) {
      this.includePlannedCancellations = includePlannedCancellations;
      return this;
    }

    public Builder setIncludeRealtimeCancellations(boolean includeRealtimeCancellations) {
      this.includeRealtimeCancellations = includeRealtimeCancellations;
      return this;
    }

    public Builder withRaptor(Consumer<RaptorPreferences.Builder> body) {
      this.raptor = raptor.copyOf().apply(body).build();
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    TransitPreferences build() {
      var value = new TransitPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}

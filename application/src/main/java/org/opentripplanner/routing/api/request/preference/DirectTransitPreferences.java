package org.opentripplanner.routing.api.request.preference;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.utils.tostring.ToStringBuilder;

///  All preferences related to the Direct Transit Search
public class DirectTransitPreferences {

  // The next constants are package-local to be readable in the unit-test.
  static final double DEFAULT_RELUCTANCE = 1.0;
  static final CostLinearFunction DEFAULT_COST_RELAX_FUNCTION = CostLinearFunction.of(
    Cost.costOfMinutes(15),
    1.5
  );

  public static final DirectTransitPreferences DEFAULT = new DirectTransitPreferences(
    false,
    DEFAULT_COST_RELAX_FUNCTION,
    DEFAULT_RELUCTANCE,
    null
  );

  private final boolean enabled;
  private final CostLinearFunction costRelaxFunction;
  private final double extraAccessEgressReluctance;

  @Nullable
  private final Duration maxAccessEgressDuration;

  private DirectTransitPreferences(
    boolean enabled,
    CostLinearFunction costRelaxFunction,
    double extraAccessEgressReluctance,
    @Nullable Duration maxAccessEgressDuration
  ) {
    this.enabled = enabled;
    this.costRelaxFunction = Objects.requireNonNull(costRelaxFunction);
    this.extraAccessEgressReluctance = extraAccessEgressReluctance;
    this.maxAccessEgressDuration = maxAccessEgressDuration;
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /// Whether to enable direct transit search
  public boolean enabled() {
    return enabled;
  }

  /// This is used to limit the results from the search. Paths are compared with the cheapest path
  /// in the search window and are included in the result if they fall within the limit given by the
  /// costRelaxFunction.
  public CostLinearFunction costRelaxFunction() {
    return costRelaxFunction;
  }

  /// An extra cost that is used to increase the cost of the access/egress legs for this search.
  public double extraAccessEgressReluctance() {
    return extraAccessEgressReluctance;
  }

  ///  Whether there is any extra access/egress reluctance
  public boolean isExtraReluctanceAddedToAccessAndEgress() {
    return extraAccessEgressReluctance != DEFAULT_RELUCTANCE;
  }

  /// A limit on the duration for access/egress for this search. Setting this to 0 will only include
  /// results that require no access or egress. I.e. a stop-to-stop search.
  public Optional<Duration> maxAccessEgressDuration() {
    return Optional.ofNullable(maxAccessEgressDuration);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DirectTransitPreferences that = (DirectTransitPreferences) o;
    return (
      enabled == that.enabled &&
      Double.compare(extraAccessEgressReluctance, that.extraAccessEgressReluctance) == 0 &&
      Objects.equals(maxAccessEgressDuration, that.maxAccessEgressDuration) &&
      Objects.equals(costRelaxFunction, that.costRelaxFunction)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      enabled,
      costRelaxFunction,
      extraAccessEgressReluctance,
      maxAccessEgressDuration
    );
  }

  @Override
  public String toString() {
    if (!enabled) {
      return "DirectTransitPreferences{not enabled}";
    }
    return ToStringBuilder.of(DirectTransitPreferences.class)
      .addObj("costRelaxFunction", costRelaxFunction, DEFAULT.costRelaxFunction)
      .addNum(
        "extraAccessEgressReluctance",
        extraAccessEgressReluctance,
        DEFAULT.extraAccessEgressReluctance
      )
      .addDuration(
        "maxAccessEgressDuration",
        maxAccessEgressDuration,
        DEFAULT.maxAccessEgressDuration
      )
      .toString();
  }

  public static class Builder {

    private boolean enabled;
    private CostLinearFunction costRelaxFunction;
    private double extraAccessEgressReluctance;
    private Duration maxAccessEgressDuration;
    public DirectTransitPreferences original;

    public Builder(DirectTransitPreferences original) {
      this.original = original;
      this.enabled = original.enabled;
      this.costRelaxFunction = original.costRelaxFunction;
      this.extraAccessEgressReluctance = original.extraAccessEgressReluctance;
      this.maxAccessEgressDuration = original.maxAccessEgressDuration;
    }

    public Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder withCostRelaxFunction(CostLinearFunction costRelaxFunction) {
      this.costRelaxFunction = costRelaxFunction;
      return this;
    }

    public Builder withExtraAccessEgressReluctance(double extraAccessEgressReluctance) {
      this.extraAccessEgressReluctance = extraAccessEgressReluctance;
      return this;
    }

    public Builder withMaxAccessEgressDuration(Duration maxAccessEgressDuration) {
      this.maxAccessEgressDuration = maxAccessEgressDuration;
      return this;
    }

    public DirectTransitPreferences build() {
      var value = new DirectTransitPreferences(
        enabled,
        costRelaxFunction,
        extraAccessEgressReluctance,
        maxAccessEgressDuration
      );
      return original.equals(value) ? original : value;
    }
  }
}

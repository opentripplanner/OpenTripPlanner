package org.opentripplanner.routing.api.request.preference;

import java.util.Objects;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class RelaxedLimitedTransferPreferences {

  public static final RelaxedLimitedTransferPreferences DEFAULT =
    new RelaxedLimitedTransferPreferences();

  private final boolean enabled;
  private final CostLinearFunction costRelaxFunction;
  private final double extraAccessEgressCostFactor;
  private final boolean disableAccessEgress;

  private RelaxedLimitedTransferPreferences() {
    this.enabled = false;
    this.costRelaxFunction = CostLinearFunction.of(Cost.costOfMinutes(15), 1.5);
    this.extraAccessEgressCostFactor = 1.0;
    this.disableAccessEgress = false;
  }

  RelaxedLimitedTransferPreferences(Builder builder) {
    this.enabled = builder.enabled;
    this.costRelaxFunction = builder.costRelaxFunction;
    this.extraAccessEgressCostFactor = builder.extraAccessEgressCostFactor;
    this.disableAccessEgress = builder.disableAccessEgress;
  }

  public static Builder of() {
    return new Builder(new RelaxedLimitedTransferPreferences());
  }

  /// Whether to enable Direct transit search
  public boolean enabled() {
    return enabled;
  }

  /// This is used to limit the results from the direct transit search. Paths are compared with the
  /// cheapest path in the search window and are included in the result if they fall within the
  /// limit given by the costRelaxFunction.
  public CostLinearFunction costRelaxFunction() {
    return costRelaxFunction;
  }

  /// An extra cost that is used to increase the cost of the access/egress legs for direct transit
  /// search.
  public double extraAccessEgressCostFactor() {
    return extraAccessEgressCostFactor;
  }

  /// If access egress is disabled the direct transit search will only include results that require
  /// no access or egress. I.e. a stop-to-stop search.
  public boolean disableAccessEgress() {
    return disableAccessEgress;
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    RelaxedLimitedTransferPreferences that = (RelaxedLimitedTransferPreferences) o;
    return (
      enabled == that.enabled &&
      Double.compare(extraAccessEgressCostFactor, that.extraAccessEgressCostFactor) == 0 &&
      disableAccessEgress == that.disableAccessEgress &&
      Objects.equals(costRelaxFunction, that.costRelaxFunction)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      enabled,
      costRelaxFunction,
      extraAccessEgressCostFactor,
      disableAccessEgress
    );
  }

  public static class Builder {

    private boolean enabled;
    private CostLinearFunction costRelaxFunction;
    private double extraAccessEgressCostFactor;
    private boolean disableAccessEgress;

    public Builder(RelaxedLimitedTransferPreferences original) {
      this.enabled = original.enabled;
      this.costRelaxFunction = original.costRelaxFunction;
      this.extraAccessEgressCostFactor = original.extraAccessEgressCostFactor;
      this.disableAccessEgress = original.disableAccessEgress;
    }

    public Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder withCostRelaxFunction(CostLinearFunction costRelaxFunction) {
      this.costRelaxFunction = costRelaxFunction;
      return this;
    }

    public Builder withExtraAccessEgressCostFactor(double extraAccessEgressCostFactor) {
      this.extraAccessEgressCostFactor = extraAccessEgressCostFactor;
      return this;
    }

    public Builder withDisableAccessEgress(boolean disableAccessEgress) {
      this.disableAccessEgress = disableAccessEgress;
      return this;
    }

    public RelaxedLimitedTransferPreferences build() {
      return new RelaxedLimitedTransferPreferences(this);
    }
  }
}

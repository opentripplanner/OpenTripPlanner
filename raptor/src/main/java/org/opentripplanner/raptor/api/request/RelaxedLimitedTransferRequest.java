package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;

public class RelaxedLimitedTransferRequest {

  private final boolean enabled;
  private final RelaxFunction costRelaxFunction;
  private final double extraAccessEgressCostFactor;
  private final boolean disableAccessEgress;

  private RelaxedLimitedTransferRequest() {
    this.enabled = false;
    this.costRelaxFunction = GeneralizedCostRelaxFunction.of(2, 20 * 60 * 100);
    this.extraAccessEgressCostFactor = 1.0;
    this.disableAccessEgress = false;
  }

  RelaxedLimitedTransferRequest(Builder builder) {
    this.enabled = builder.enabled;
    this.costRelaxFunction = builder.costRelaxFunction;
    this.extraAccessEgressCostFactor = builder.extraAccessEgressCostFactor;
    this.disableAccessEgress = builder.disableAccessEgress;
  }

  public static Builder of() {
    return new Builder(new RelaxedLimitedTransferRequest());
  }

  /// Whether to enable Direct transit search
  public boolean enabled() {
    return enabled;
  }

  /// This is used to limit the results from the direct transit search. Paths are compared with the
  /// cheapest path in the search window and are included in the result if they fall within the
  /// limit given by the costRelaxFunction.
  public RelaxFunction costRelaxFunction() {
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

  public static class Builder {

    private boolean enabled;
    private RelaxFunction costRelaxFunction;
    private double extraAccessEgressCostFactor;
    private boolean disableAccessEgress;

    public Builder(RelaxedLimitedTransferRequest original) {
      this.enabled = original.enabled;
      this.costRelaxFunction = original.costRelaxFunction;
      this.extraAccessEgressCostFactor = original.extraAccessEgressCostFactor;
      this.disableAccessEgress = original.disableAccessEgress;
    }

    public Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder withCostRelaxFunction(RelaxFunction costRelaxFunction) {
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

    public RelaxedLimitedTransferRequest build() {
      return new RelaxedLimitedTransferRequest(this);
    }
  }
}

package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.lang.ToStringBuilder;

public class TransferOptimizationPreferences
  implements Serializable, TransferOptimizationParameters {

  private final boolean optimizeTransferWaitTime;
  private final double minSafeWaitTimeFactor;
  private final double backTravelWaitTimeFactor;
  private final double extraStopBoardAlightCostsFactor;

  public static final TransferOptimizationPreferences DEFAULT = new TransferOptimizationPreferences();

  public TransferOptimizationPreferences() {
    this.optimizeTransferWaitTime = true;
    this.minSafeWaitTimeFactor = 5.0;
    this.backTravelWaitTimeFactor = 1.0;
    this.extraStopBoardAlightCostsFactor = 0.0;
  }

  public TransferOptimizationPreferences(Builder builder) {
    this.optimizeTransferWaitTime = builder.optimizeTransferWaitTime;
    this.minSafeWaitTimeFactor = builder.minSafeWaitTimeFactor;
    this.backTravelWaitTimeFactor = builder.backTravelWaitTimeFactor;
    this.extraStopBoardAlightCostsFactor = builder.extraStopBoardAlightCostsFactor;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public boolean optimizeTransferWaitTime() {
    return optimizeTransferWaitTime;
  }

  @Override
  public double minSafeWaitTimeFactor() {
    return minSafeWaitTimeFactor;
  }

  @Override
  public double backTravelWaitTimeFactor() {
    return backTravelWaitTimeFactor;
  }

  @Override
  public double extraStopBoardAlightCostsFactor() {
    return extraStopBoardAlightCostsFactor;
  }

  @Override
  public boolean optimizeTransferPriority() {
    return OTPFeature.TransferConstraints.isOn();
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(TransferOptimizationPreferences.class)
      .addBoolIfTrue("skipOptimizeWaitTime", !optimizeTransferWaitTime)
      .addNum("minSafeWaitTimeFactor", minSafeWaitTimeFactor, DEFAULT.minSafeWaitTimeFactor)
      .addNum(
        "backTravelWaitTimeFactor",
        backTravelWaitTimeFactor,
        DEFAULT.backTravelWaitTimeFactor
      )
      .addNum(
        "extraStopBoardAlightCostsFactor",
        extraStopBoardAlightCostsFactor,
        DEFAULT.extraStopBoardAlightCostsFactor
      )
      .toString();
  }

  public static class Builder {

    private final TransferOptimizationPreferences original;
    private boolean optimizeTransferWaitTime;
    private double minSafeWaitTimeFactor;
    private double backTravelWaitTimeFactor;
    private double extraStopBoardAlightCostsFactor;

    public Builder(TransferOptimizationPreferences original) {
      this.original = original;
      this.optimizeTransferWaitTime = original.optimizeTransferWaitTime;
      this.minSafeWaitTimeFactor = original.minSafeWaitTimeFactor;
      this.backTravelWaitTimeFactor = original.backTravelWaitTimeFactor;
      this.extraStopBoardAlightCostsFactor = original.extraStopBoardAlightCostsFactor;
    }

    public Builder withOptimizeTransferWaitTime(boolean optimizeTransferWaitTime) {
      this.optimizeTransferWaitTime = optimizeTransferWaitTime;
      return this;
    }

    public Builder withMinSafeWaitTimeFactor(double minSafeWaitTimeFactor) {
      this.minSafeWaitTimeFactor = minSafeWaitTimeFactor;
      return this;
    }

    public Builder withBackTravelWaitTimeFactor(double backTravelWaitTimeFactor) {
      this.backTravelWaitTimeFactor = backTravelWaitTimeFactor;
      return this;
    }

    public Builder withExtraStopBoardAlightCostsFactor(double extraStopBoardAlightCostsFactor) {
      this.extraStopBoardAlightCostsFactor = extraStopBoardAlightCostsFactor;
      return this;
    }

    public TransferOptimizationPreferences build() {
      var value = new TransferOptimizationPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}

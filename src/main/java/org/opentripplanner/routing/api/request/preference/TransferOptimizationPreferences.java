package org.opentripplanner.routing.api.request.preference;

import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.util.OTPFeature;
import org.opentripplanner.util.lang.ToStringBuilder;

public record TransferOptimizationPreferences(
  boolean optimizeTransferWaitTime,
  double minSafeWaitTimeFactor,
  double backTravelWaitTimeFactor,
  double extraStopBoardAlightCostsFactor
)
  implements TransferOptimizationParameters {
  public static final TransferOptimizationPreferences DEFAULT = new TransferOptimizationPreferences(
    true,
    5.0,
    1.0,
    0.0
  );

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
}

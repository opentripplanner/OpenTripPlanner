package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.util.OTPFeature;

public class TransferOptimizationRequest implements TransferOptimizationParameters {
  public boolean optimizeTransferWaitTime = true;
  public double minSafeWaitTimeFactor = 5.0;

  @Override
  public boolean optimizeTransferPriority() {
    return OTPFeature.TransferConstraints.isOn();
  }

  @Override
  public boolean optimizeTransferWaitTime() {
    return optimizeTransferWaitTime;
  }

  @Override
  public double minSafeWaitTimeFactor() {
    return minSafeWaitTimeFactor;
  }
}

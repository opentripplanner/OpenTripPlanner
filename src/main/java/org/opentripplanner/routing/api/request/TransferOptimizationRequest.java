package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.util.OTPFeature;

public class TransferOptimizationRequest implements TransferOptimizationParameters {
  private final RoutingRequest parent;
  public boolean optimizeTransferWaitTime = true;
  public double minSafeWaitTimeFactor = 5.0;
  public double inverseWaitReluctance = 1.0;

  public TransferOptimizationRequest(RoutingRequest parent) {
    this.parent = parent;
  }

  @Override
  public boolean optimizeTransferPriority() {
    return OTPFeature.GuaranteedTransfers.isOn();
  }

  @Override
  public boolean optimizeTransferWaitTime() {
    return optimizeTransferWaitTime;
  }

  @Override
  public double waitReluctanceRouting() {
    return parent.waitReluctance;
  }

  @Override
  public double inverseWaitReluctance() {
    return inverseWaitReluctance;
  }

  @Override
  public double minSafeWaitTimeFactor() {
    return minSafeWaitTimeFactor;
  }
}

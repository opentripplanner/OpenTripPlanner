package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;

public class TransferOptimizationRequest implements TransferOptimizationParameters {
  private final RoutingRequest parent;
  public boolean useOptimizeTransferCostFunction = true;
  public double minSafeWaitTimeFactor = 5.0;
  public double inverseWaitReluctance = 1.0;

  public TransferOptimizationRequest(RoutingRequest parent) {
    this.parent = parent;
  }

  @Override
  public boolean useOptimizeTransferCostFunction() {
    return useOptimizeTransferCostFunction;
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

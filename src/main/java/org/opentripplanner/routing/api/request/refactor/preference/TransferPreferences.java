package org.opentripplanner.routing.api.request.refactor.preference;

import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.routing.api.request.TransferOptimizationRequest;

public class TransferPreferences {
  int cost = 0;
  int slack = 120;
  int nonpreferredCost = 180;
  double waitReluctance = 1.0;
  double waitAtBeginningFactor = 0.4;
  TransferOptimizationParameters optimization = new TransferOptimizationRequest();
  Integer maxTransfers = 12;
}

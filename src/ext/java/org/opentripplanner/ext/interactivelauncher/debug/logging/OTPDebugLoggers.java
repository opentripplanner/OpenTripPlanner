package org.opentripplanner.ext.interactivelauncher.debug.logging;

import java.util.List;

record OTPDebugLoggers(String label, String logger) {
  static List<OTPDebugLoggers> list() {
    return List.of(
      of("Data import issues", "DATA_IMPORT_ISSUES"),
      of("All OTP debuggers", "org.opentripplanner"),
      of("OTP request/response", "org.opentripplanner.routing.service.DefaultRoutingService"),
      of("Raptor request/response", "org.opentripplanner.raptor.RaptorService"),
      of("Transfer Optimization", "org.opentripplanner.routing.algorithm.transferoptimization")
    );
  }

  private static OTPDebugLoggers of(String label, String logger) {
    return new OTPDebugLoggers(label, logger);
  }
}

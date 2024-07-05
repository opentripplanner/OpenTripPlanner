package org.opentripplanner.ext.interactivelauncher.debug.logging;

import java.util.List;

class DebugLoggers {

  static List<Entry> list() {
    return List.of(
      of("Data import issues", "DATA_IMPORT_ISSUES"),
      of("All OTP debuggers", "org.opentripplanner"),
      of("OTP request/response", "org.opentripplanner.routing.service.DefaultRoutingService"),
      of("Raptor request/response", "org.opentripplanner.raptor.RaptorService"),
      of("Transfer Optimization", "org.opentripplanner.routing.algorithm.transferoptimization")
    );
  }

  static List<String> listLoggers() {
    return list().stream().map(Entry::logger).toList();
  }

  private static Entry of(String label, String logger) {
    return new Entry(label, logger);
  }

  record Entry(String label, String logger) {}
}

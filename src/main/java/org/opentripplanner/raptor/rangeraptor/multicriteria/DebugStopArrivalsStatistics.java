package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static java.util.Locale.ROOT;

import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.debug.DebugTopic;

/**
 * Utility class to print some statistics about stop arrivals.
 */
class DebugStopArrivalsStatistics {

  private final DebugLogger debugLogger;

  DebugStopArrivalsStatistics(DebugLogger debugLogger) {
    this.debugLogger = debugLogger;
  }

  void debugStatInfo(StopArrivalParetoSet<?>[] stops) {
    if (!debugLogger.isEnabled()) {
      return;
    }

    long total = 0;
    long arrayLen = 0;
    long numOfStops = 0;
    int max = 0;

    for (StopArrivalParetoSet<?> stop : stops) {
      if (stop != null) {
        ++numOfStops;
        total += stop.size();
        max = Math.max(stop.size(), max);
        arrayLen += stop.internalArrayLength();
      }
    }
    double avg = ((double) total) / numOfStops;
    double arrayLenAvg = ((double) arrayLen) / numOfStops;

    // Debug stop arrivals statistics.
    //  - For each stop the number of arrivals logged with:
    //    - Avarage number of arrivals for stops visited.
    //    - The maximum numbers of arrivals for any stop.
    //    - The total number of stop arrivals.
    //  - The capacity(array length) used:
    //    - The average array length for stops visited.
    //    - The total array length allocated.
    //  - The number of stops:
    //    - The number of stops visited.
    //    - The total number of stops.
    debugLogger.debug(
      DebugTopic.STOP_ARRIVALS_STATISTICS,
      "Arrivals %5s %3s %6s (avg/max/tot)  -  Array Length: %5s %5s (avg/tot) -  Stops: %5s %5s (visited/tot)",
      toStr(avg),
      toStr(max),
      toStr(total),
      toStr(arrayLenAvg),
      toStr(arrayLen),
      toStr(numOfStops),
      toStr(stops.length)
    );
  }

  private static String toStr(long number) {
    if (number > 1_000_000) {
      return toStr(number / 1_000_000.0) + "\"";
    }
    if (number > 1_000) {
      return toStr(number / 1_000.0) + "'";
    }
    return Long.toString(number);
  }

  private static String toStr(double number) {
    if (number > 10) {
      return String.format(ROOT, "%.0f", number);
    }
    return String.format(ROOT, "%.1f", number);
  }
}

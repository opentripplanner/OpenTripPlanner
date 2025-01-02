package org.opentripplanner.routing.algorithm.transferoptimization.model;

/**
 * A Stop time represent an arrival-time or a departure-time at a given stop.
 */
public interface StopTime {
  static StopTime stopTime(final int stop, final int time) {
    return new BasicStopTime(stop, time);
  }

  int stop();

  int time();

  default int duration(StopTime later) {
    return later.time() - time();
  }
}

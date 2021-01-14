package org.opentripplanner.transit.raptor._data;

import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;
import static org.opentripplanner.transit.raptor.util.TimeUtils.parseDuration;

public interface RaptorTestConstants {

  // Time duration(D) constants, all values are in seconds
  int D1s = 1;
  int D10s = 10;
  int D20s = 20;
  int D30s = 30;
  int D1m = parseDuration("1m");
  int D2m = parseDuration("2m");
  int D2m1s = parseDuration("2m1s");
  int D3m = parseDuration("3m");
  int D5m = parseDuration("5m");
  int D6m = parseDuration("6m");
  int D7m = parseDuration("7m");
  int D10m = parseDuration("10m");

  // Time constants, all values are in seconds
  int T00_00 = hm2time(0, 0);
  int T00_04 = hm2time(0, 4);
  int T00_10 = hm2time(0, 10);
  int T00_30 = hm2time(0, 30);

  // Stop indexes
  int STOP_0 = 0;
  int STOP_1 = 1;
  int STOP_2 = 2;
  int STOP_3 = 3;
  int STOP_4 = 4;
  int STOP_5 = 5;
  int STOP_6 = 6;
  int STOP_7 = 7;

  // Trip indexes - Raptor do not use these. The indexes are
  // only used locally in tests to index a set of trips.
  int TRIP_A = 0;
  int TRIP_B = 1;
  int TRIP_C = 2;
}

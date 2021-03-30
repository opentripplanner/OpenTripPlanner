package org.opentripplanner.transit.raptor._data;

import static org.opentripplanner.util.time.DurationUtils.duration;
import static org.opentripplanner.util.time.TimeUtils.hm2time;

public interface RaptorTestConstants {

  // Time duration(D) constants, all values are in seconds
  int D1s = 1;
  int D10s = 10;
  int D20s = 20;
  int D30s = 30;
  int D1m = duration("1m");
  int D2m = duration("2m");
  int D2m1s = duration("2m1s");
  int D3m = duration("3m");
  int D5m = duration("5m");
  int D6m = duration("6m");
  int D7m = duration("7m");
  int D10m = duration("10m");

  // Time constants, all values are in seconds
  int T00_00 = hm2time(0, 0);
  int T00_04 = hm2time(0, 4);
  int T00_10 = hm2time(0, 10);
  int T00_30 = hm2time(0, 30);

  // Stop indexes - Note! There is no stop defined for index 0(zero)! You must
  // account for that in the test if you uses a stop index.
  int STOP_A = 1;
  int STOP_B = 2;
  int STOP_C = 3;
  int STOP_D = 4;
  int STOP_E = 5;
  int STOP_F = 6;
  int STOP_G = 7;
  int STOP_H = 8;

  // Stop position in pattern
  int STOP_POS_0 = 0;
  int STOP_POS_1 = 1;
  int STOP_POS_2 = 2;
  int STOP_POS_3 = 3;
  int STOP_POS_4 = 4;
  int STOP_POS_5 = 5;
  int STOP_POS_6 = 6;
  int STOP_POS_7 = 7;

  // Trip indexes - Raptor do not use these. The indexes are
  // only used locally in tests to index a set of patterns/trips.
  int LINE_11 = 0;
  int LINE_21 = 1;
  int LINE_31 = 2;
}

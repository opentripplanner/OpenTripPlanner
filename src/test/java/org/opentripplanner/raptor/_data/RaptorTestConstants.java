package org.opentripplanner.raptor._data;

import static org.opentripplanner.framework.time.DurationUtils.durationInSeconds;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;

public interface RaptorTestConstants {
  // Time duration(D) constants, all values are in seconds
  int D0s = 0;
  int D1s = 1;
  int D10s = 10;
  int D11s = 11;
  int D20s = 20;
  int D30s = 30;
  int D40s = 40;
  int D1m = durationInSeconds("1m");
  int D2m = durationInSeconds("2m");
  int D3m = durationInSeconds("3m");
  int D4m = durationInSeconds("4m");
  int D5m = durationInSeconds("5m");
  int D7m = durationInSeconds("7m");
  int D8m = durationInSeconds("8m");
  int D10m = durationInSeconds("10m");
  int D11m = durationInSeconds("11m");
  int D20m = durationInSeconds("20m");
  int D24h = durationInSeconds("24h");

  // Time constants, all values are in seconds
  int T00_00 = hm2time(0, 0);
  int T00_01 = hm2time(0, 1);
  int T00_02 = hm2time(0, 2);
  int T00_10 = hm2time(0, 10);
  int T00_30 = hm2time(0, 30);
  int T00_40 = hm2time(0, 40);
  int T01_00 = hm2time(1, 0);

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

  // Slack
  int BOARD_SLACK = 45;
  int ALIGHT_SLACK = 15;
  int TRANSFER_SLACK = 60;

  // FLEX
  int ONE_RIDE = 1;
  int TWO_RIDES = 2;

  default String stopIndexToName(int index) {
    return Character.toString('A' + index - 1);
  }
}

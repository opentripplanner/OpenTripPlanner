package org.opentripplanner.raptorlegacy._data;

import static org.opentripplanner.utils.time.DurationUtils.durationInSeconds;
import static org.opentripplanner.utils.time.TimeUtils.hm2time;

import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public interface RaptorTestConstants {
  // Time duration(D) constants, all values are in seconds
  int D0s = 0;
  int D1s = 1;
  int D10s = 10;
  int D20s = 20;
  int D30s = 30;
  int D40s = 40;
  int D1m = durationInSeconds("1m");
  int D2m = durationInSeconds("2m");
  int D3m = durationInSeconds("3m");
  int D4m = durationInSeconds("4m");
  int D5m = durationInSeconds("5m");
  int D24h = durationInSeconds("24h");

  /**
   * There are 86400 seconds in a "normal" day(24 * 60 * 60).
   */
  int SECONDS_IN_A_DAY = (int) D24h;

  // Time constants, all values are in seconds
  int T00_00 = hm2time(0, 0);

  // Stop indexes - Note! There is no stop defined for index 0(zero)! You must
  // account for that in the test if you use the stop index.
  int STOP_A = 1;
  int STOP_B = 2;
  int STOP_C = 3;
  int STOP_D = 4;
  int STOP_E = 5;
  int STOP_F = 6;
  int STOP_G = 7;
  int STOP_H = 8;
  int STOP_I = 9;
  int STOP_J = 10;
  int STOP_K = 11;
  int STOP_L = 12;
  int STOP_M = 13;

  int NUM_STOPS = 14;

  // Stop position in pattern
  int STOP_POS_0 = 0;
  int STOP_POS_1 = 1;

  // Slack
  int BOARD_SLACK = 45;
  int ALIGHT_SLACK = 15;
  int TRANSFER_SLACK = 60;

  // COST_CALCULATION
  int BOARD_COST = 60;
  int TRANSFER_COST = 120;
  double WAIT_RELUCTANCE = 0.8;

  RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_COST,
    TRANSFER_COST,
    WAIT_RELUCTANCE,
    null,
    null
  );

  default String stopIndexToName(int index) {
    return Character.toString('A' + index - 1);
  }
}

package org.opentripplanner.model.plan;

import static org.opentripplanner.framework.time.TimeUtils.time;

import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public interface PlanTestConstants {
  int NOT_SET = -999_999;
  int BOARD_COST = 120;
  float WALK_RELUCTANCE_FACTOR = 2.0f;
  float BICYCLE_RELUCTANCE_FACTOR = 1.0f;
  float CAR_RELUCTANCE_FACTOR = 1.0f;
  float WAIT_RELUCTANCE_FACTOR = 0.8f;
  float WALK_SPEED = 1.4f;
  float BICYCLE_SPEED = 5.0f;
  float BUS_SPEED = 12.5f;
  float RAIL_SPEED = 25.0f;
  float CAR_SPEED = 25.0f;

  // Time duration(D) constants, all values are in seconds
  int D1m = DurationUtils.durationInSeconds("1m");
  int D2m = DurationUtils.durationInSeconds("2m");
  int D3m = DurationUtils.durationInSeconds("3m");
  int D4m = DurationUtils.durationInSeconds("4m");
  int D5m = DurationUtils.durationInSeconds("5m");
  int D10m = DurationUtils.durationInSeconds("10m");
  int D12m = DurationUtils.durationInSeconds("12m");

  // Time constants, all values are in seconds
  int T11_00 = time("11:00");
  int T11_01 = time("11:01");
  int T11_02 = time("11:02");
  int T11_03 = time("11:03");
  int T11_04 = time("11:04");
  int T11_05 = time("11:05");
  int T11_06 = time("11:06");
  int T11_07 = time("11:07");
  int T11_08 = time("11:08");
  int T11_09 = time("11:09");
  int T11_10 = time("11:10");
  int T11_12 = time("11:12");
  int T11_14 = time("11:14");
  int T11_15 = time("11:15");
  int T11_16 = time("11:16");
  int T11_20 = time("11:20");
  int T11_23 = time("11:23");
  int T11_25 = time("11:25");
  int T11_27 = time("11:27");
  int T11_28 = time("11:28");
  int T11_30 = time("11:30");
  int T11_32 = time("11:32");
  int T11_33 = time("11:33");
  int T11_50 = time("11:50");
  int T11_55 = time("11:55");

  /**
   * @deprecated Create the TransitModelForTest per test, do not share between tests - it has state
   */
  @Deprecated
  TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place A = Place.forStop(TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place B = Place.forStop(TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place C = Place.forStop(TEST_MODEL.stop("C").withCoordinate(7.0, 9.0).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place D = Place.forStop(TEST_MODEL.stop("D").withCoordinate(8.0, 9.5).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place E = Place.forStop(TEST_MODEL.stop("E").withCoordinate(9.0, 10.0).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place F = Place.forStop(TEST_MODEL.stop("F").withCoordinate(9.0, 10.5).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place G = Place.forStop(TEST_MODEL.stop("G").withCoordinate(9.5, 11.0).build());

  /**
   * @deprecated Depend on TransitModelForTest. Create per test, do not share between tests - it has state
   */
  @Deprecated
  Place H = Place.forStop(TEST_MODEL.stop("H").withCoordinate(10.0, 11.5).build());
}

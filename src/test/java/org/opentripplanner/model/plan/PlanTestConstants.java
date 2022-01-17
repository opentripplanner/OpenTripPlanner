package org.opentripplanner.model.plan;

import static org.opentripplanner.util.time.TimeUtils.time;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.util.time.DurationUtils;

public interface PlanTestConstants {
  ServiceDate SERVICE_DATE = new ServiceDate(2020, 9, 21);
  String FEED_ID = "F";

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
  int D1m = DurationUtils.duration("1m");
  int D2m = DurationUtils.duration("2m");
  int D3m = DurationUtils.duration("3m");
  int D5m = DurationUtils.duration("5m");
  int D6m = DurationUtils.duration("6m");
  int D7m = DurationUtils.duration("7m");
  int D10m = DurationUtils.duration("10m");
  int D12m = DurationUtils.duration("12m");
  int D24m = DurationUtils.duration("24m");
  int D40m = DurationUtils.duration("40m");

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

  // Stop/Places
  Place A = place("A", 5.0, 8.0 );
  Place B = place("B", 6.0, 8.5);
  Place C = place("C", 7.0, 9.0);
  Place D = place("D", 8.0, 9.5);
  Place E = place("E", 9.0, 10.0);
  Place F = place("F", 9.0, 10.5);
  Place G = place("G", 9.5, 11.0);
  Place H = place("H", 10.0, 11.5);

  private static Place place(String name, double lat, double lon) {
    var stop = new Stop(
            new FeedScopedId(FEED_ID, name),
            name,
            null,
            null,
            WgsCoordinate.creatOptionalCoordinate(lat, lon),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );
    return Place.forStop(
            stop, null, null
    );
  }
}

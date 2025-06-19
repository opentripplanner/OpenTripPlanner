package org.opentripplanner.routing.trippattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

public class FrequencyEntryTest {

  private static final int STOP_NUM = 8;
  private static final ScheduledTripTimes tripTimes;

  static {
    Trip trip = TimetableRepositoryForTest.trip("testtrip").build();

    List<StopTime> stopTimes = new ArrayList<>();

    int time = 0;
    for (int i = 0; i < STOP_NUM; ++i) {
      FeedScopedId id = TimetableRepositoryForTest.id(i + "");

      RegularStop stop = TimetableRepositoryForTest.of().stop(id.getId(), 0.0, 0.0).build();

      StopTime stopTime = new StopTime();
      stopTime.setStop(stop);
      stopTime.setArrivalTime(time);
      if (i != 0 && i != STOP_NUM - 1) {
        time += 10;
      }
      stopTime.setDepartureTime(time);
      time += 90;
      stopTime.setStopSequence(i);
      stopTimes.add(stopTime);
    }

    tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
  }

  @Test
  public void testExactFrequencyProperEnd() {
    FrequencyEntry fe = make(100000, 150000, 100, true);
    assertEquals(149900, fe.nextDepartureTime(0, 149900));
    assertEquals(-1, fe.nextDepartureTime(0, 150000));
  }

  @Test
  public void testExactFrequencyStopOffset() {
    FrequencyEntry fe = make(100000, 150001, 100, true);

    // testing first trip departure
    assertEquals(100000, fe.nextDepartureTime(0, 100000)); // first stop, on begin
    assertEquals(100500, fe.nextDepartureTime(5, 100000)); // 6th stop, before begin

    // testing last trip departure
    assertEquals(150000, fe.nextDepartureTime(0, 150000)); // 1st stop, on end
    assertEquals(-1, fe.nextDepartureTime(0, 150100)); // 1st stop, after end
    assertEquals(150100, fe.nextDepartureTime(1, 150100)); // 2nd stop, on end
    assertEquals(150500, fe.nextDepartureTime(5, 150500)); // 6th stop, on end
    assertEquals(-1, fe.nextDepartureTime(5, 150600)); // 6th stop, after end

    // testing first trip arrival
    assertEquals(-1, fe.prevArrivalTime(4, 100300)); // 5th stop, before begin
    assertEquals(100390, fe.prevArrivalTime(4, 100400)); // 5th stop, after begin
    assertEquals(-1, fe.prevArrivalTime(7, 100600)); // 8th stop, before begin
    assertEquals(100690, fe.prevArrivalTime(7, 100700)); // 8th stop, after begin

    // testing last trip arrival
    assertEquals(150390, fe.prevArrivalTime(4, 150700)); // 5th stop
    assertEquals(150690, fe.prevArrivalTime(7, 150700)); // 8th stop, on end
    assertEquals(150690, fe.prevArrivalTime(7, 150750)); // 8th stop, after end
  }

  @Test
  public void testInexactFrequencyStopOffset() {
    FrequencyEntry fe = make(100000, 150000, 100, false);

    // testing last trip departure
    assertEquals(149900, fe.nextDepartureTime(0, 149800)); // 1st stop, before end
    assertEquals(-1, fe.nextDepartureTime(0, 150100)); // 1st stop, after end
    assertEquals(150400, fe.nextDepartureTime(5, 150300)); // 6th stop, before end
    assertEquals(-1, fe.nextDepartureTime(5, 150600)); // 6th stop, after end

    // testing first trip arrival
    assertEquals(-1, fe.prevArrivalTime(4, 100300)); // 5th stop, before begin
    assertEquals(100400, fe.prevArrivalTime(4, 100500)); // 5th stop, after begin
    assertEquals(-1, fe.prevArrivalTime(7, 100600)); // 8th stop, before begin
    assertEquals(100700, fe.prevArrivalTime(7, 100800)); // 8th stop, after begin
  }

  private static FrequencyEntry make(int startTime, int endTime, int headwaySecs, boolean exact) {
    Frequency f = new Frequency();
    f.setStartTime(startTime);
    f.setEndTime(endTime);
    f.setHeadwaySecs(headwaySecs);
    f.setExactTimes(exact ? 1 : 0);

    return new FrequencyEntry(f, tripTimes);
  }
}

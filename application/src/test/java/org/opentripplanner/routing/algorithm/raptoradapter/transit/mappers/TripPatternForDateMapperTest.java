package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

public class TripPatternForDateMapperTest {

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final LocalDate SERVICE_DATE = LocalDate.of(2009, 8, 7);
  private static final int SERVICE_CODE = 555;
  private static final Map<LocalDate, TIntSet> serviceCodesRunningForDate = Map.of(
    SERVICE_DATE,
    tintHashSet(SERVICE_CODE)
  );
  private static Timetable timetable;

  @BeforeAll
  public static void setUp() throws Exception {
    var pattern = TEST_MODEL.pattern(BUS).build();
    var trip = TimetableRepositoryForTest.trip("1").build();
    var tripTimes = TripTimesFactory.tripTimes(
      trip,
      TEST_MODEL.stopTimesEvery5Minutes(5, trip, "11:00"),
      new Deduplicator()
    ).withServiceCode(SERVICE_CODE);
    timetable = Timetable.of().withTripPattern(pattern).addTripTimes(tripTimes).build();
  }

  /**
   * Tests that when there are no service codes for the specified service date, the mapper returns
   * null.
   */
  @Test
  void testTimetableWithNoServiceCodesRunningForDateShouldReturnNull() {
    TripPatternForDateMapper mapper = new TripPatternForDateMapper(serviceCodesRunningForDate);

    //Invalid service date
    LocalDate invalidDate = LocalDate.of(2999, 1, 1);

    assertNull(mapper.map(timetable, invalidDate));
  }

  /**
   * Tests that when there are service codes for the specified service date, the mapper returns a
   * TripPatternForDate object with the correct trip times. And the pattern is not null.
   */
  @Test
  void testTimetableWithServiceCodesRunningForDateShouldReturnTripPatternForDate() {
    TripPatternForDateMapper mapper = new TripPatternForDateMapper(serviceCodesRunningForDate);

    TripPatternForDate mappedPattern = mapper.map(timetable, SERVICE_DATE);

    assertNotNull(mappedPattern);

    //Each of the mapped trip times should be in the original timetable. (For this specific date)
    //Can't simply do a check by index, as the trip times for date are sorted during mapping.
    for (TripTimes times : mappedPattern.tripTimes()) {
      boolean timeTableContainsTimes = timetable.getTripTimes().contains(times);
      assertTrue(timeTableContainsTimes);
    }
  }

  /**
   * Tests that when there are service codes for the specified service date, but the timetable does
   * not contain any trips for those service codes, the mapper returns null.
   */
  @Test
  void testTimeTableWithServiceCodesRunningNotMatchingShouldReturnNull() {
    //ServiceCodesRunningForDate has the date, but it should not include a service code of any of the trips in the timetable.
    Map<LocalDate, TIntSet> invalidServiceCodesRunningForDate = new HashMap<>();

    //Add an service code that is not used by any of the trips in the timetable.
    TIntSet serviceCodes = new TIntHashSet();
    serviceCodes.add(999);

    invalidServiceCodesRunningForDate.put(SERVICE_DATE, serviceCodes);

    TripPatternForDateMapper mapper = new TripPatternForDateMapper(
      invalidServiceCodesRunningForDate
    );
    assertNull(mapper.map(timetable, SERVICE_DATE));
  }

  private static TIntHashSet tintHashSet(int... numbers) {
    var set = new TIntHashSet();
    set.addAll(numbers);
    return set;
  }
}

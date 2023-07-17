package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitModel;

public class TripPatternForDateMapperTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2009, 8, 7);
  private static final String TRIP_ID = "1.1";
  private static Map<LocalDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();
  private static Map<FeedScopedId, TripPattern> patternIndex;
  private static Timetable timetable;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);
    TransitModel transitModel = model.transitModel();

    //Add the service codes running for the date
    serviceCodesRunningForDate =
      transitModel.getTransitModelIndex().getServiceCodesRunningForDate();

    String feedId = transitModel.getFeedIds().stream().findFirst().get();
    patternIndex = new HashMap<>();

    for (TripPattern pattern : transitModel.getAllTripPatterns()) {
      pattern.scheduledTripsAsStream().forEach(trip -> patternIndex.put(trip.getId(), pattern));
    }

    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, TRIP_ID));
    timetable = pattern.getScheduledTimetable();
  }

  /**
   * Tests that when there are no service codes for the specified service date,
   * the mapper returns null.
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
   * Tests that when there are service codes for the specified service date, but the timetable
   * does not contain any trips for those service codes, the mapper returns null.
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
}

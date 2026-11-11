package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.apis.gtfs.service.ApiTransitService;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

class StopCallImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 6, 3);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    SERVICE_DATE,
    TIME_ZONE
  ).toInstant();
  private static final String TRIP_ID = "Trip1";
  private static final String FLEX_TRIP_ID = "FlexTrip1";
  private static final String MODIFIED_TRIP_ID = "ModifiedTrip1";
  private static final String MODIFIED_TOSD_ID = "ModifiedTosd1";
  private static final ZonedDateTime ANY_TIME = ZonedDateTime.parse("2023-06-03T12:00:00+02:00");
  private static final double TOLERANCE = 1e-6;

  private static final OffsetDateTime NOON = OffsetDateTime.parse("2023-06-03T12:00+02:00");
  private static final OffsetDateTime TEN_AM = NOON.minusHours(2);
  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");
  private final AreaStop STOP_D = envBuilder.areaStop("D");
  private final AreaStop STOP_E = envBuilder.areaStop("E");

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .addStop(STOP_A, "12:00:00", "12:00:00")
    .addStop(STOP_B, "12:30:00", "12:30:00")
    .addStop(STOP_C, "13:00:00", "13:00:00");

  private final TripInput FLEX_TRIP_INPUT = TripInput.flex(FLEX_TRIP_ID)
    .addStop(STOP_D, "10:00", "10:30")
    .addStop(STOP_E, "11:00", "11:30");

  @Test
  void fixedTrip() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var tripTimes = tripData.tripTimes();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

    var impl = new StopCallImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      call,
      Map.of(),
      realtimeEnv.transitService()
    );

    var schedule = impl.schedule().get(env);
    assertEquals(new CallSchedule(new ArrivalDepartureTime(NOON, NOON, false)), schedule);
  }

  @Test
  void geometryToNextCall() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var tripTimes = tripData.tripTimes();
    var pattern = tripData.tripPattern();

    var impl = new StopCallImpl();
    var stops = List.of(STOP_A, STOP_B, STOP_C);
    for (int stopPos = 0; stopPos < 2; stopPos++) {
      var call = new TripTimeOnDate(tripTimes, stopPos, pattern, SERVICE_DATE, MIDNIGHT);
      var env = DataFetchingSupport.dataFetchingEnvironment(
        call,
        Map.of(),
        realtimeEnv.transitService()
      );
      // Without a shape, the path is a straight line from this stop to the next one
      var coordinates = impl.geometryToNextCall().get(env).getCoordinates();
      assertEquals(2, coordinates.length);
      assertCoordinate(coordinate(stops.get(stopPos)), coordinates[0]);
      assertCoordinate(coordinate(stops.get(stopPos + 1)), coordinates[1]);
    }

    var lastCall = new TripTimeOnDate(tripTimes, 2, pattern, SERVICE_DATE, MIDNIGHT);
    var env = DataFetchingSupport.dataFetchingEnvironment(
      lastCall,
      Map.of(),
      realtimeEnv.transitService()
    );
    assertNull(impl.geometryToNextCall().get(env));
  }

  /**
   * A real-time update replaces the last two stops of the trip: the first hop is unchanged, the
   * second ends at another stop of the same station and the third ends at a stop that is not in the
   * schedule. The geometries must be the same for the calls of the trip on service date and the
   * calls of a leg on the modified trip.
   */
  @Test
  void geometryToNextCallOfRealTimeModifiedTrip() throws Exception {
    var stopC1 = envBuilder.stopAtStation("C1", "StationC");
    var stopC2 = envBuilder.stopAtStation("C2", "StationC");
    var stopX = envBuilder.stop("X");
    var stopY = envBuilder.stop("Y");
    var realtimeEnv = envBuilder
      .addTrip(
        TripInput.of(MODIFIED_TRIP_ID)
          .withWithTripOnServiceDate(MODIFIED_TOSD_ID)
          .addStop(STOP_A, "12:00:00", "12:00:00")
          .addStop(STOP_B, "12:10:00", "12:10:00")
          .addStop(stopC1, "12:20:00", "12:20:00")
          .addStop(stopX, "12:30:00", "12:30:00")
          // Bent hops can be told apart from the straight lines of hops without a scheduled shape
          .withHopGeometries(
            bentLine(STOP_A, STOP_B),
            bentLine(STOP_B, stopC1),
            bentLine(stopC1, stopX)
          )
      )
      .build();
    realtimeEnv.applyStopPatternUpdate(MODIFIED_TRIP_ID, STOP_A, STOP_B, stopC2, stopY);
    var tripData = realtimeEnv.tripData(MODIFIED_TRIP_ID);
    var scheduledPattern = tripData.scheduledTripPattern();
    var transitService = realtimeEnv.transitService();

    var tripOnServiceDate = transitService.getTripOnServiceDate(id(MODIFIED_TOSD_ID));
    var tripOnServiceDateCalls = new TripOnServiceDateImpl()
      .stopCalls()
      .get(
        DataFetchingSupport.dataFetchingEnvironment(tripOnServiceDate, Map.of(), transitService)
      );

    var leg = new ScheduledTransitLegBuilder()
      .withTripPattern(tripData.tripPattern())
      .withTripTimes(tripData.tripTimes())
      .withStartTime(ANY_TIME)
      .withEndTime(ANY_TIME)
      .withServiceDate(SERVICE_DATE)
      .withZoneId(realtimeEnv.timeZone())
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(3)
      .build();
    var legCalls = new ApiTransitService(transitService).findStopCalls(leg);

    for (var calls : List.of(tripOnServiceDateCalls, legCalls)) {
      var geometries = geometriesToNextCall(calls, transitService);
      assertEquals(4, geometries.size());

      // Unchanged hop: the scheduled shape is kept
      assertTrue(geometries.get(0).equalsExact(scheduledPattern.getHopGeometry(0), TOLERANCE));

      // Hop to another stop of the same station: the scheduled shape is moved to the new stop
      var scheduledHop = scheduledPattern.getHopGeometry(1).getCoordinates();
      var stationHop = geometries.get(1).getCoordinates();
      assertEquals(scheduledHop.length, stationHop.length);
      assertCoordinate(scheduledHop[0], stationHop[0]);
      assertCoordinate(scheduledHop[1], stationHop[1]);
      assertCoordinate(coordinate(stopC2), stationHop[2]);

      // Hop to a stop that is not in the schedule: a straight line between the stops
      var newHop = geometries.get(2).getCoordinates();
      assertEquals(2, newHop.length);
      assertCoordinate(coordinate(stopC2), newHop[0]);
      assertCoordinate(coordinate(stopY), newHop[1]);

      assertNull(geometries.get(3));
    }
  }

  /**
   * The path of a flexible trip is not known, whether its stops are areas or regular stops.
   */
  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void noGeometryToNextCallForFlexibleTrips(boolean regularStopFirst) {
    var flexTripInput = regularStopFirst
      ? TripInput.flex(FLEX_TRIP_ID)
          .addStop(STOP_A, "10:00", "10:00")
          .addStop(STOP_E, "11:00", "11:30")
      : TripInput.flex(FLEX_TRIP_ID)
          .addStop(STOP_D, "10:00", "10:30")
          .addStop(STOP_B, "11:00", "11:00");
    OTPFeature.FlexRouting.testOn(() -> {
      var realtimeEnv = envBuilder.addTrip(flexTripInput).build();
      var tripData = realtimeEnv.tripData(FLEX_TRIP_ID);
      var call = new TripTimeOnDate(
        tripData.tripTimes(),
        0,
        tripData.tripPattern(),
        SERVICE_DATE,
        MIDNIGHT
      );
      var env = DataFetchingSupport.dataFetchingEnvironment(
        call,
        Map.of(),
        realtimeEnv.transitService()
      );
      try {
        assertNull(new StopCallImpl().geometryToNextCall().get(env));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  void flexTrip() {
    OTPFeature.FlexRouting.testOn(() -> {
      var realtimeEnv = envBuilder.addTrip(FLEX_TRIP_INPUT).build();
      var tripData = realtimeEnv.tripData(FLEX_TRIP_ID);
      var tripTimes = tripData.tripTimes();
      var pattern = tripData.tripPattern();

      var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

      var impl = new StopCallImpl();
      var env = DataFetchingSupport.dataFetchingEnvironment(
        call,
        Map.of(),
        realtimeEnv.transitService()
      );

      // using try-catch is not very nice here - ideally we would just add it to the method signature.
      // however, this test is run with the flex feature enabled in a runnable which prevents this.
      try {
        CallSchedule schedule = impl.schedule().get(env);
        assertEquals(new CallSchedule(new TimeWindow(TEN_AM, TEN_AM.plusMinutes(30))), schedule);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private static List<Geometry> geometriesToNextCall(
    Iterable<TripTimeOnDate> calls,
    TransitService transitService
  ) throws Exception {
    var impl = new StopCallImpl();
    var geometries = new ArrayList<Geometry>();
    for (var call : calls) {
      var env = DataFetchingSupport.dataFetchingEnvironment(call, Map.of(), transitService);
      geometries.add(impl.geometryToNextCall().get(env));
    }
    return geometries;
  }

  /** A line from one stop to another that bends away from the straight line between them. */
  private static LineString bentLine(RegularStop from, RegularStop to) {
    var start = coordinate(from);
    var end = coordinate(to);
    var bend = new Coordinate((start.x + end.x) / 2, (start.y + end.y) / 2 + 0.001);
    return GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {
      start,
      bend,
      end,
    });
  }

  private static Coordinate coordinate(RegularStop stop) {
    return stop.getCoordinate().asJtsCoordinate();
  }

  private static void assertCoordinate(Coordinate expected, Coordinate actual) {
    assertTrue(
      expected.equals2D(actual, TOLERANCE),
      "Expected %s, but was %s".formatted(expected, actual)
    );
  }
}

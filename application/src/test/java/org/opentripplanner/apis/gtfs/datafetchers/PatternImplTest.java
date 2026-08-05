package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.schema.DataFetchingEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class PatternImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 6, 27);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);

  private final TripInput TRIP_1 = TripInput.of("Trip1")
    .addStop(envBuilder.stop("A"), "12:00:00")
    .addStop(envBuilder.stop("B"), "12:30:00");

  // Same stop pattern and route as TRIP_1, so both trips end up on the same TripPattern.
  private final TripInput TRIP_2 = TripInput.of("Trip2")
    .addStop(envBuilder.stop("A"), "13:00:00")
    .addStop(envBuilder.stop("B"), "13:30:00")
    .withServiceDates(SERVICE_DATE.plusDays(1));

  @Test
  void codeReturnsPatternId() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_1).build();
    var pattern = realtimeEnv.tripData("Trip1").scheduledTripPattern();

    var impl = new PatternImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      pattern,
      Map.of(),
      realtimeEnv.transitService()
    );

    assertEquals(pattern.getId().toString(), impl.code().get(env));
  }

  @Test
  void directionIdReturnsUnknownByDefault() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_1).build();
    var pattern = realtimeEnv.tripData("Trip1").scheduledTripPattern();

    var impl = new PatternImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      pattern,
      Map.of(),
      realtimeEnv.transitService()
    );

    assertEquals(Direction.UNKNOWN.gtfsCode, impl.directionId().get(env));
  }

  @Test
  void tripsOnServiceDateSynthesizesWhenTripRuns() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_1).build();
    var pattern = realtimeEnv.tripData("Trip1").scheduledTripPattern();
    var trip = realtimeEnv.tripData("Trip1").trip();

    var impl = new PatternImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      pattern,
      Map.of("serviceDate", SERVICE_DATE),
      realtimeEnv.transitService()
    );

    var result = tripsOnServiceDate(impl, env);

    assertEquals(1, result.size());
    assertEquals(trip, result.getFirst().getTrip());
    assertEquals(SERVICE_DATE, result.getFirst().getServiceDate());
    // No real TripOnServiceDate exists, so a synthetic one keyed by the trip id is returned.
    assertEquals(trip.getId(), result.getFirst().getId());
  }

  @Test
  void tripsOnServiceDateExcludesTripsThatDoNotRunOnGivenDate() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_1).addTrip(TRIP_2).build();
    var pattern = realtimeEnv.tripData("Trip1").scheduledTripPattern();
    var trip1 = realtimeEnv.tripData("Trip1").trip();

    var impl = new PatternImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      pattern,
      Map.of("serviceDate", SERVICE_DATE),
      realtimeEnv.transitService()
    );

    var result = tripsOnServiceDate(impl, env);

    assertEquals(List.of(trip1), result.stream().map(TripOnServiceDate::getTrip).toList());
  }

  @Test
  void tripsOnServiceDateReturnsRealTripOnServiceDate() throws Exception {
    var tripInput = TRIP_1.withWithTripOnServiceDate("DSJ1");
    var realtimeEnv = envBuilder.addTrip(tripInput).build();
    var pattern = realtimeEnv.tripData("Trip1").scheduledTripPattern();
    var trip = realtimeEnv.tripData("Trip1").trip();

    var impl = new PatternImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      pattern,
      Map.of("serviceDate", SERVICE_DATE),
      realtimeEnv.transitService()
    );

    var result = tripsOnServiceDate(impl, env);

    assertEquals(1, result.size());
    // The real TripOnServiceDate (keyed by its own id) is preferred over a synthetic one.
    assertEquals("DSJ1", result.getFirst().getId().getId());
    assertEquals(trip, result.getFirst().getTrip());
  }

  private static List<TripOnServiceDate> tripsOnServiceDate(
    PatternImpl impl,
    DataFetchingEnvironment env
  ) throws Exception {
    var result = new ArrayList<TripOnServiceDate>();
    impl.tripsOnServiceDate().get(env).forEach(result::add);
    return result;
  }
}

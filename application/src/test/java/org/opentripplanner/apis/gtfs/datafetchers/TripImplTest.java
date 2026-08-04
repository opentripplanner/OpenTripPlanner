package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class TripImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 6, 3);
  private static final String TRIP_ID = "Trip1";

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .addStop(envBuilder.stop("A"), "12:00:00")
    .addStop(envBuilder.stop("B"), "12:30:00");

  @Test
  void activeDatesReturnsSingleDate() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var trip = realtimeEnv.tripData(TRIP_ID).trip();

    var impl = new TripImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      trip,
      Map.of(),
      realtimeEnv.transitService()
    );

    var activeDates = impl.activeDates().get(env);
    assertEquals(List.of("20230603"), activeDates);
  }

  @Test
  void activeDatesReturnsSortedDates() throws Exception {
    var tripInput = TRIP_INPUT.withServiceDates(
      LocalDate.of(2023, 6, 5),
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 3)
    );
    var realtimeEnv = envBuilder.addTrip(tripInput).build();
    var trip = realtimeEnv.tripData(TRIP_ID).trip();

    var impl = new TripImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      trip,
      Map.of(),
      realtimeEnv.transitService()
    );

    var activeDates = impl.activeDates().get(env);
    assertEquals(List.of("20230601", "20230603", "20230605"), activeDates);
  }

  @Test
  void onServiceDateSynthesizesWhenTripRuns() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var trip = realtimeEnv.tripData(TRIP_ID).trip();

    var impl = new TripImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      trip,
      Map.of("date", SERVICE_DATE),
      realtimeEnv.transitService()
    );

    TripOnServiceDate result = impl.onServiceDate().get(env);

    assertNotNull(result);
    assertEquals(trip, result.getTrip());
    assertEquals(SERVICE_DATE, result.getServiceDate());
    // No real TripOnServiceDate exists, so a synthetic one keyed by the trip id is returned.
    assertEquals(trip.getId(), result.getId());
  }

  @Test
  void onServiceDateReturnsNullWhenTripDoesNotRun() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var trip = realtimeEnv.tripData(TRIP_ID).trip();

    var impl = new TripImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      trip,
      Map.of("date", SERVICE_DATE.plusDays(1)),
      realtimeEnv.transitService()
    );

    assertNull(impl.onServiceDate().get(env));
  }

  @Test
  void onServiceDateReturnsRealTripOnServiceDate() throws Exception {
    var tripInput = TRIP_INPUT.withWithTripOnServiceDate("DSJ1");
    var realtimeEnv = envBuilder.addTrip(tripInput).build();
    var trip = realtimeEnv.tripData(TRIP_ID).trip();

    var impl = new TripImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      trip,
      Map.of("date", SERVICE_DATE),
      realtimeEnv.transitService()
    );

    TripOnServiceDate result = impl.onServiceDate().get(env);

    assertNotNull(result);
    // The real TripOnServiceDate (keyed by its own id) is preferred over a synthetic one.
    assertEquals("DSJ1", result.getId().getId());
    assertEquals(trip, result.getTrip());
  }
}

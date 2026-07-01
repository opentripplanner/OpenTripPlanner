package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;

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
}

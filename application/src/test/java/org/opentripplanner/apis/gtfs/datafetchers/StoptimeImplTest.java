package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.CANCELLED;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.DEFAULT;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.INACCURATE_PREDICTIONS;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.NO_DATA;
import static org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLStopRealTimeState.RECORDED;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.dataFetchingEnvironment;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

class StoptimeImplTest {

  private static final String TRIP_ID = "Trip1";
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 15);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .addStop(STOP_A, "10:00:00", "10:00:00")
    .addStop(STOP_B, "10:30:00", "10:30:00")
    .addStop(STOP_C, "11:00:00", "11:00:00");

  private static final StoptimeImpl SUBJECT = new StoptimeImpl();

  // ---------------------------------------------------------------------------
  // Scheduled trip – always DEFAULT regardless of stop position
  // ---------------------------------------------------------------------------

  @Test
  void scheduledTrip_returnsDefaultForEveryStop() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var scheduledTripTimes = tripData.scheduledTripTimes();
    var pattern = tripData.tripPattern();

    for (int i = 0; i < 3; i++) {
      var call = new TripTimeOnDate(scheduledTripTimes, i, pattern);
      assertEquals(DEFAULT, SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call)));
    }
  }

  // ---------------------------------------------------------------------------
  // RealTimeTripTimes – stop states set explicitly via builder
  // ---------------------------------------------------------------------------

  @Test
  void noDataStop_returnsNoData() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    // mark stop B as NO_DATA
    builder.withNoData(1);
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(realTimeTripTimes, 1, pattern);
    assertEquals(NO_DATA, SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call)));
  }

  @Test
  void cancelledStop_returnsCancelled() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    // mark stop B as CANCELLED
    builder.withCanceled(1);
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(realTimeTripTimes, 1, pattern);
    assertEquals(CANCELLED, SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call)));
  }

  @Test
  void inaccuratePredictionsStop_returnsInaccuratePredictions() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    builder.withInaccuratePredictions(1);
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(realTimeTripTimes, 1, pattern);
    assertEquals(
      INACCURATE_PREDICTIONS,
      SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call))
    );
  }

  @Test
  void recordedStop_returnsRecorded() throws Exception {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    builder.withStopRealTimeState(1, StopRealTimeState.RECORDED);
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(realTimeTripTimes, 1, pattern);
    assertEquals(RECORDED, SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call)));
  }

  @Test
  void unmodifiedRealTimeStop_returnsDefault() throws Exception {
    // A RealTimeTripTimes where no stop state was explicitly set defaults to DEFAULT
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(realTimeTripTimes, 1, pattern);
    assertEquals(DEFAULT, SUBJECT.stopRealTimeState().get(dataFetchingEnvironment(call)));
  }

  @Test
  void stopStateIsPerStop_notSharedAcrossStops() throws Exception {
    // Setting stop B to NO_DATA must not affect stop A or stop C
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    // only stop B
    builder.withNoData(1);
    var realTimeTripTimes = builder.build();
    var pattern = tripData.tripPattern();

    assertEquals(
      DEFAULT,
      SUBJECT.stopRealTimeState().get(
        dataFetchingEnvironment(new TripTimeOnDate(realTimeTripTimes, 0, pattern))
      )
    );
    assertEquals(
      NO_DATA,
      SUBJECT.stopRealTimeState().get(
        dataFetchingEnvironment(new TripTimeOnDate(realTimeTripTimes, 1, pattern))
      )
    );
    assertEquals(
      DEFAULT,
      SUBJECT.stopRealTimeState().get(
        dataFetchingEnvironment(new TripTimeOnDate(realTimeTripTimes, 2, pattern))
      )
    );
  }
}

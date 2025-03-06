package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.STOP_A1;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.STOP_B1;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.TRIP_1_ID;
import static org.opentripplanner.updater.trip.RealtimeTestConstants.TRIP_2_ID;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import uk.org.siri.siri20.EstimatedVehicleJourney;

class SiriFuzzyTripMatcherTest {

  @Test
  void match() {
    TripInput trip1Input = tripInput(TRIP_1_ID);

    var env = RealtimeTestEnvironment.of().addTrip(trip1Input).build();
    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env);
    assertTrue(result.isSuccess());
  }

  @Test
  void multipleMatches() {
    var trip1input = tripInput(TRIP_1_ID);
    var trip2input = tripInput(TRIP_2_ID);

    var env = RealtimeTestEnvironment.of().addTrip(trip1input).addTrip(trip2input).build();

    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env);
    assertTrue(result.isFailure());
    assertEquals(MULTIPLE_FUZZY_TRIP_MATCHES, result.failureValue());
  }

  @Test
  void scheduledStopPoint() {
    var scheduledStopPointId = "ssp-1";
    var trip1input = tripInput(TRIP_1_ID);

    var env = RealtimeTestEnvironment.of().addTrip(trip1input).build();
    env.timetableRepository.addScheduledStopPointMapping(Map.of(id(scheduledStopPointId), STOP_B1));

    var journey = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:10:00", "00:10:00")
          .call(scheduledStopPointId)
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();

    var result = match(journey, env);
    assertTrue(result.isSuccess());
  }

  @Test
  void unknownStopPointRef() {
    var trip1input = tripInput(TRIP_1_ID);

    var env = RealtimeTestEnvironment.of().addTrip(trip1input).build();

    var journey = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:10:00", "00:10:00")
          .call("SOME_MADE_UP_ID")
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();

    var result = match(journey, env);
    assertTrue(result.isFailure());
    assertEquals(NO_FUZZY_TRIP_MATCH, result.failureValue());
  }

  private static Result<TripAndPattern, UpdateError.UpdateErrorType> match(
    EstimatedVehicleJourney evj,
    RealtimeTestEnvironment env
  ) {
    var transitService = env.getTransitService();
    var fuzzyMatcher = new SiriFuzzyTripMatcher(transitService);
    return fuzzyMatcher.match(
      evj,
      new EntityResolver(transitService, env.getFeedId()),
      transitService::findTimetable,
      transitService::findNewTripPatternForModifiedTrip
    );
  }

  private static EstimatedVehicleJourney estimatedVehicleJourney(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:10:00", "00:10:00")
          .call(STOP_B1)
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();
  }

  private static TripInput tripInput(String trip1Id) {
    return TripInput.of(trip1Id)
      .addStop(STOP_A1, "0:10:00", "0:10:00")
      .addStop(STOP_B1, "0:20:00", "0:20:00")
      .build();
  }
}

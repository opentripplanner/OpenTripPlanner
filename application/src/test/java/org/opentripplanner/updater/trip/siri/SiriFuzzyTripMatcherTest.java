package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import uk.org.siri.siri21.EstimatedVehicleJourney;

class SiriFuzzyTripMatcherTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  @Test
  void match() {
    TripInput trip1Input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1Input).build();
    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env);
    assertTrue(result.isSuccess());
  }

  @Test
  void multipleMatches() {
    var trip1input = tripInput(TRIP_1_ID);
    var trip2input = tripInput(TRIP_2_ID);

    var env = ENV_BUILDER.addTrip(trip1input).addTrip(trip2input).build();

    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env);
    assertTrue(result.isFailure());
    assertEquals(MULTIPLE_FUZZY_TRIP_MATCHES, result.failureValue());
  }

  @Test
  void scheduledStopPoint() {
    var scheduledStopPointId = "ssp-1";
    var trip1input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1input).build();
    env.timetableRepository.addScheduledStopPointMapping(Map.of(id(scheduledStopPointId), STOP_B));

    var journey = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
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

    var env = ENV_BUILDER.addTrip(trip1input).build();

    var journey = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
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

  private EstimatedVehicleJourney estimatedVehicleJourney(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:10:00", "00:10:00")
          .call(STOP_B)
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();
  }

  private TripInput tripInput(String trip1Id) {
    return TripInput.of(trip1Id)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(STOP_B, "0:20:00", "0:20:00")
      .build();
  }
}

package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN_STOP;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import uk.org.siri.siri21.EstimatedVehicleJourney;

class SiriFuzzyTripMatcherTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  @Test
  void match() {
    TripInput trip1Input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1Input).build();
    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env);
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  @Test
  void multipleMatches() {
    var trip1input = tripInput(TRIP_1_ID);
    var trip2input = tripInput(TRIP_2_ID);

    var env = ENV_BUILDER.addTrip(trip1input).addTrip(trip2input).build();

    var evj = estimatedVehicleJourney(env);

    assertFailure(MULTIPLE_FUZZY_TRIP_MATCHES, () -> match(evj, env));
  }

  @Test
  void scheduledStopPoint() {
    var scheduledStopPointId = "ssp-1";
    var trip1input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1input)
      .addScheduledStopPointMapping(id(scheduledStopPointId), STOP_B)
      .build();

    var journey = new SiriEtBuilder(env.localTimeParser())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:10:00", "00:10:00")
          .call(scheduledStopPointId)
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();

    var result = match(journey, env);
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  @Test
  void unknownStopPointRef() {
    var trip1input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1input).build();

    var journey = new SiriEtBuilder(env.localTimeParser())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:10:00", "00:10:00")
          .call("SOME_MADE_UP_ID")
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();

    assertFailure(UNKNOWN_STOP, () -> match(journey, env));
  }

  @Test
  void noDepartureTime() {
    var trip1input = tripInput(TRIP_1_ID);

    var env = ENV_BUILDER.addTrip(trip1input).build();

    var journey = new SiriEtBuilder(env.localTimeParser())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected(null, null)
          .call("SOME_MADE_UP_ID")
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();

    assertFailure(INVALID_DEPARTURE_TIME, () -> match(journey, env));
  }

  @Test
  void scheduleWithSecondsDoesNotMatchWithoutTolerance() {
    var env = ENV_BUILDER.addTrip(tripInputWithSeconds(TRIP_1_ID)).build();
    var evj = estimatedVehicleJourney(env);

    assertFailure(NO_FUZZY_TRIP_MATCH, () -> match(evj, env));
  }

  @Test
  void scheduleWithSecondsMatchesWithinTolerance() {
    var env = ENV_BUILDER.addTrip(tripInputWithSeconds(TRIP_1_ID)).build();
    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env, Duration.ofSeconds(59));
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  @Test
  void scheduleBeforeAimedTimesMatchesWithinTolerance() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:09:36", "0:09:36")
      .addStop(STOP_B, "0:19:36", "0:19:36");
    var env = ENV_BUILDER.addTrip(tripInput).build();
    var evj = estimatedVehicleJourney(env);

    var result = match(evj, env, Duration.ofSeconds(30));
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  @Test
  void toleranceIsInclusive() {
    var env = ENV_BUILDER.addTrip(tripInputWithSeconds(TRIP_1_ID)).build();
    var evj = estimatedVehicleJourney(env);

    assertFailure(NO_FUZZY_TRIP_MATCH, () -> match(evj, env, Duration.ofSeconds(23)));
    var result = match(evj, env, Duration.ofSeconds(24));
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  @Test
  void departureOutsideToleranceDoesNotMatch() {
    // The arrival at the last stop is within the tolerance, the departure is not
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:11:00", "0:11:00")
      .addStop(STOP_B, "0:20:24", "0:20:24");
    var env = ENV_BUILDER.addTrip(tripInput).build();
    var evj = estimatedVehicleJourney(env);

    assertFailure(NO_FUZZY_TRIP_MATCH, () -> match(evj, env, Duration.ofSeconds(59)));
  }

  @Test
  void arrivalOutsideToleranceDoesNotMatch() {
    // The departure at the first stop is within the tolerance, the arrival is not
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:10:24", "0:10:24")
      .addStop(STOP_B, "0:21:00", "0:21:00");
    var env = ENV_BUILDER.addTrip(tripInput).build();
    var evj = estimatedVehicleJourney(env);

    assertFailure(NO_FUZZY_TRIP_MATCH, () -> match(evj, env, Duration.ofSeconds(59)));
  }

  @Test
  void multipleMatchesWithinTolerance() {
    var trip2Input = TripInput.of(TRIP_2_ID)
      .addStop(STOP_A, "0:09:40", "0:09:40")
      .addStop(STOP_B, "0:19:40", "0:19:40");
    var env = ENV_BUILDER.addTrip(tripInputWithSeconds(TRIP_1_ID)).addTrip(trip2Input).build();
    var evj = estimatedVehicleJourney(env);

    assertFailure(MULTIPLE_FUZZY_TRIP_MATCHES, () -> match(evj, env, Duration.ofSeconds(59)));
  }

  /**
   * SIRI reports another quay of the same station than the schedule. An unrelated trip arrives
   * at the reported quay within the tolerance. It must not prevent the lookup on sibling quays.
   */
  @Test
  void siblingQuayMatchesWhenReportedQuayHasUnrelatedTripWithinTolerance() {
    var quay1 = ENV_BUILDER.stopAtStation("B1", "B");
    var quay2 = ENV_BUILDER.stopAtStation("B2", "B");
    var stopC = ENV_BUILDER.stop("C");
    var scheduled = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:10:24", "0:10:24")
      .addStop(quay1, "0:20:24", "0:20:24");
    var unrelated = TripInput.of(TRIP_2_ID)
      .addStop(stopC, "0:10:50", "0:10:50")
      .addStop(quay2, "0:20:50", "0:20:50");
    var env = ENV_BUILDER.addTrip(scheduled).addTrip(unrelated).build();

    var evj = estimatedVehicleJourney(env, STOP_A, quay2);

    var result = match(evj, env, Duration.ofSeconds(59));
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  /**
   * The same fallback at zero tolerance: an unrelated trip arrives at the reported quay at the
   * exact second. It must not prevent the lookup on sibling quays.
   */
  @Test
  void siblingQuayMatchesWhenReportedQuayHasUnrelatedTripAtSameSecond() {
    var quay1 = ENV_BUILDER.stopAtStation("B1", "B");
    var quay2 = ENV_BUILDER.stopAtStation("B2", "B");
    var stopC = ENV_BUILDER.stop("C");
    var scheduled = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(quay1, "0:20:00", "0:20:00");
    var unrelated = TripInput.of(TRIP_2_ID)
      .addStop(stopC, "0:10:00", "0:10:00")
      .addStop(quay2, "0:20:00", "0:20:00");
    var env = ENV_BUILDER.addTrip(scheduled).addTrip(unrelated).build();

    var evj = estimatedVehicleJourney(env, STOP_A, quay2);

    var result = match(evj, env);
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  /**
   * A match at the reported quay is preferred over matches at sibling quays, so the update is not
   * rejected as ambiguous.
   */
  @Test
  void reportedQuayIsPreferredOverSiblingQuay() {
    var quay1 = ENV_BUILDER.stopAtStation("B1", "B");
    var quay2 = ENV_BUILDER.stopAtStation("B2", "B");
    var tripToQuay1 = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(quay1, "0:20:00", "0:20:00");
    var tripToQuay2 = TripInput.of(TRIP_2_ID)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(quay2, "0:20:00", "0:20:00");
    var env = ENV_BUILDER.addTrip(tripToQuay1).addTrip(tripToQuay2).build();

    var evj = estimatedVehicleJourney(env, STOP_A, quay2);

    var result = match(evj, env);
    assertEquals(TRIP_2_ID, result.trip().getId().getId());
  }

  /**
   * The trip started yesterday and arrives after midnight. An unrelated trip arrives at the same
   * stop today within the tolerance. It must not prevent the lookup of trips from yesterday.
   */
  @Test
  void tripFromYesterdayMatchesWhenTodayHasUnrelatedTripWithinTolerance() {
    var stopC = ENV_BUILDER.stop("C");
    var overnight = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "23:50:24", "23:50:24")
      .addStop(STOP_B, "24:10:24", "24:10:24");
    var unrelated = TripInput.of(TRIP_2_ID)
      .addStop(stopC, "0:00:30", "0:00:30")
      .addStop(STOP_B, "0:10:30", "0:10:30");
    var env = ENV_BUILDER.addTrip(overnight).addTrip(unrelated).build();

    var evj = overnightJourney(env);

    var result = match(evj, env, Duration.ofSeconds(59));
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  /**
   * The same fallback at zero tolerance: an unrelated trip arrives at the same stop today at the
   * exact second. It must not prevent the lookup of trips from yesterday.
   */
  @Test
  void tripFromYesterdayMatchesWhenTodayHasUnrelatedTripAtSameSecond() {
    var stopC = ENV_BUILDER.stop("C");
    var overnight = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "23:50:00", "23:50:00")
      .addStop(STOP_B, "24:10:00", "24:10:00");
    var unrelated = TripInput.of(TRIP_2_ID)
      .addStop(stopC, "0:00:00", "0:00:00")
      .addStop(STOP_B, "0:10:00", "0:10:00");
    var env = ENV_BUILDER.addTrip(overnight).addTrip(unrelated).build();

    var evj = overnightJourney(env);

    var result = match(evj, env);
    assertEquals(TRIP_1_ID, result.trip().getId().getId());
  }

  private static TripAndPattern match(EstimatedVehicleJourney evj, TransitTestEnvironment env)
    throws UpdateException {
    return match(evj, env, Duration.ZERO);
  }

  private static TripAndPattern match(
    EstimatedVehicleJourney evj,
    TransitTestEnvironment env,
    Duration timeTolerance
  ) throws UpdateException {
    var timetableRepository = env.newTimetableRepository();
    var transitRepository = env.transitRepository();
    var cache = SiriFuzzyTripMatcherCache.create(
      new DefaultTransitService(
        transitRepository,
        DefaultTimetableRepository.scheduledOnly(env.scheduledTimetableData())
      )
    );
    var fuzzyMatcher = new SiriFuzzyTripMatcher(
      cache,
      timetableRepository,
      transitRepository,
      timeTolerance
    );
    return fuzzyMatcher.match(
      EstimatedVehicleJourneyWrapper.of(evj),
      new EntityResolver(timetableRepository, transitRepository, env.feedId()),
      timetableRepository::resolve,
      timetableRepository::getNewTripPatternForModifiedTrip
    );
  }

  private EstimatedVehicleJourney estimatedVehicleJourney(TransitTestEnvironment env) {
    return estimatedVehicleJourney(env, STOP_A, STOP_B);
  }

  /** A journey departing at 00:10:00 from the first stop and arriving at 00:20:00. */
  private static EstimatedVehicleJourney estimatedVehicleJourney(
    TransitTestEnvironment env,
    RegularStop firstStop,
    RegularStop lastStop
  ) {
    return new SiriEtBuilder(env.localTimeParser())
      .withEstimatedCalls(builder ->
        builder
          .call(firstStop)
          .departAimedExpected("00:10:00", "00:10:00")
          .call(lastStop)
          .arriveAimedExpected("00:20:00", "00:20:00")
      )
      .buildEstimatedVehicleJourney();
  }

  private TripInput tripInput(String trip1Id) {
    return TripInput.of(trip1Id)
      .addStop(STOP_A, "0:10:00", "0:10:00")
      .addStop(STOP_B, "0:20:00", "0:20:00");
  }

  /** The schedule has seconds, while {@link #estimatedVehicleJourney} uses whole minutes. */
  private TripInput tripInputWithSeconds(String tripId) {
    return TripInput.of(tripId)
      .addStop(STOP_A, "0:10:24", "0:10:24")
      .addStop(STOP_B, "0:20:24", "0:20:24");
  }

  /**
   * A journey departing from stop A at 23:50:00 on the service date and arriving at stop B at
   * 00:10:00 on the next day.
   */
  private EstimatedVehicleJourney overnightJourney(TransitTestEnvironment env) {
    var evj = new SiriEtBuilder(env.localTimeParser())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("23:50:00", "23:50:00")
          .call(STOP_B)
          .arriveAimedExpected("00:10:00", "00:10:00")
      )
      .buildEstimatedVehicleJourney();
    var lastCall = evj.getEstimatedCalls().getEstimatedCalls().getLast();
    lastCall.setAimedArrivalTime(lastCall.getAimedArrivalTime().plusDays(1));
    lastCall.setExpectedArrivalTime(lastCall.getExpectedArrivalTime().plusDays(1));
    return evj;
  }
}

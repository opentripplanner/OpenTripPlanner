package org.opentripplanner.apis.transmodel.model.timetable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Tests the {@code realTimeJourneyState} field on the {@link DatedServiceJourneyType}.
 * <p>
 * The real-time trip-times state is built directly from the scheduled trip times (via
 * {@code createRealTimeFromScheduledTimes()}) and injected into a {@link TimetableRepository}, so the
 * resolver is exercised against a live {@link TransitService} without depending on any feed-source
 * adapter (GTFS-RT/SIRI) to set the flags.
 */
class DatedServiceJourneyTypeTest {

  private static final String TRIP_ID = "t1";
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 7);

  private static final GraphQL GRAPHQL = buildGraphQL();

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .addStop(STOP_A, "10:00:00", "10:00:00")
    .addStop(STOP_B, "10:10:00", "10:10:00");

  @Test
  void realTimeJourneyState_isNull_whenTripTimesNotFound() {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    // A trip that is valid but not registered in the transit service -> findTripTimes is empty.
    var unknownTrip = Trip.of(id("does-not-exist")).withRoute(envBuilder.route("Runknown")).build();
    var tripOnServiceDate = tripOnServiceDate(unknownTrip);

    assertNull(fetch(env.transitService(), tripOnServiceDate));
  }

  @Test
  void realTimeJourneyState_allFlagsFalse_forScheduledTrip() {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var state = fetch(env.transitService(), tripOnServiceDate(env, TRIP_ID));

    assertFalse(flag(state, "extraJourney"));
    assertFalse(flag(state, "cancellation"));
    assertFalse(flag(state, "timesModified"));
    assertFalse(flag(state, "journeyPatternModified"));
    assertFalse(flag(state, "updated"));
  }

  @Test
  void realTimeJourneyState_cancellationFlag() {
    var state = fetchWithUpdate(builder -> builder.withCanceled());

    assertTrue(flag(state, "cancellation"));
    assertTrue(flag(state, "updated"));
    assertFalse(flag(state, "extraJourney"));
    assertFalse(flag(state, "timesModified"));
    assertFalse(flag(state, "journeyPatternModified"));
  }

  @Test
  void realTimeJourneyState_timesModifiedFlag() {
    var state = fetchWithUpdate(builder -> builder.withRealTimeUpdated());

    assertTrue(flag(state, "timesModified"));
    assertTrue(flag(state, "updated"));
    assertFalse(flag(state, "cancellation"));
    assertFalse(flag(state, "extraJourney"));
    assertFalse(flag(state, "journeyPatternModified"));
  }

  @Test
  void realTimeJourneyState_extraJourneyFlag() {
    var state = fetchWithUpdate(builder -> builder.withAdded());

    assertTrue(flag(state, "extraJourney"));
    assertTrue(flag(state, "updated"));
    assertFalse(flag(state, "cancellation"));
  }

  @Test
  void realTimeJourneyState_journeyPatternModifiedFlag() {
    var state = fetchWithUpdate(builder -> builder.withModifiedTripPattern());

    assertTrue(flag(state, "journeyPatternModified"));
    assertTrue(flag(state, "updated"));
    assertFalse(flag(state, "cancellation"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Builds real-time trip times from the scheduled trip (applying the given customizer to set the
   * desired flags), injects them into a fresh snapshot, and fetches the resolved state.
   */
  private Map<String, Object> fetchWithUpdate(Consumer<RealTimeTripTimesBuilder> customize) {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = env.tripData(TRIP_ID);
    var builder = tripData.scheduledTripTimes().createRealTimeFromScheduledTimes();
    customize.accept(builder);
    var tripTimes = builder.build();
    var update = RealTimeTripUpdate.of(tripData.tripPattern(), tripTimes, SERVICE_DATE).build();
    var transitService = transitServiceWithUpdate(env, update);
    return fetch(transitService, tripOnServiceDate(env, TRIP_ID));
  }

  /** Builds a {@link TransitService} that sees the given real-time update in its snapshot. */
  private static TransitService transitServiceWithUpdate(
    TransitTestEnvironment env,
    RealTimeTripUpdate update
  ) {
    var repo = env.transitRepository();
    var snapshot = new DefaultTimetableRepository(
      repo.getRaptorTransitData(),
      new DefaultTripCalendars()
    );
    snapshot.update(update);
    return new DefaultTransitService(repo, snapshot.commit());
  }

  private static TripOnServiceDate tripOnServiceDate(TransitTestEnvironment env, String tripId) {
    return tripOnServiceDate(env.tripData(tripId).trip());
  }

  private static TripOnServiceDate tripOnServiceDate(Trip trip) {
    return TripOnServiceDate.of(id("tosd-1")).withTrip(trip).withServiceDate(SERVICE_DATE).build();
  }

  // ---------------------------------------------------------------------------
  // GraphQL execution
  // ---------------------------------------------------------------------------

  /** Reads a boolean flag from the resolved real-time journey state map. */
  private static boolean flag(Map<String, Object> state, String name) {
    return (boolean) state.get(name);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> fetch(
    TransitService transitService,
    TripOnServiceDate tripOnServiceDate
  ) {
    var context = new TransmodelRequestContext(null, null, transitService, null);
    var result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query(
          "{ dsj { realTimeJourneyState { extraJourney cancellation timesModified journeyPatternModified updated } } }"
        )
        .root(tripOnServiceDate)
        .context(context)
        .build()
    );
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<String, Object> dsj = (Map<String, Object>) data.get("dsj");
    return (Map<String, Object>) dsj.get("realTimeJourneyState");
  }

  private static GraphQL buildGraphQL() {
    var realTimeJourneyStateType = RealTimeTripStateType.create();
    var dsjType = new DatedServiceJourneyType(new DefaultFeedIdMapper()).create(
      ref("ServiceJourney"),
      ref("JourneyPattern"),
      ref("EstimatedCall"),
      ref("Quay"),
      ref("ReplacedBy"),
      ref("ReplacementFor"),
      realTimeJourneyStateType
    );

    var queryType = GraphQLObjectType.newObject()
      .name("Query")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("dsj")
          .type(dsjType)
          .dataFetcher(DataFetchingEnvironment::getRoot)
          .build()
      )
      .build();

    return GraphQL.newGraphQL(
      GraphQLSchema.newSchema()
        .query(queryType)
        .additionalType(realTimeJourneyStateType)
        .additionalType(stubObjectType("ServiceJourney"))
        .additionalType(stubObjectType("JourneyPattern"))
        .additionalType(stubObjectType("EstimatedCall"))
        .additionalType(stubObjectType("Quay"))
        .additionalType(stubObjectType("ReplacedBy"))
        .additionalType(stubObjectType("ReplacementFor"))
        .additionalDirective(TransmodelDirectives.TIMING_DATA)
        .build()
    ).build();
  }

  private static GraphQLTypeReference ref(String name) {
    return new GraphQLTypeReference(name);
  }

  private static GraphQLObjectType stubObjectType(String name) {
    return GraphQLObjectType.newObject()
      .name(name)
      .field(f -> f.name("_stub").type(Scalars.GraphQLBoolean))
      .build();
  }
}

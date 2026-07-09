package org.opentripplanner.apis.transmodel.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.apis.transmodel.model.plan.RealTimeTripStateType;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

class DatedServiceJourneyTypeTest {

  private static final String TRIP_ID = "t1";
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 7);

  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  private static final TransitTestEnvironment ENV = ENV_BUILDER.addTrip(
    TripInput.of(TRIP_ID)
      .addStop(ENV_BUILDER.stop("A"), "10:00:00", "10:00:00")
      .addStop(ENV_BUILDER.stop("B"), "10:10:00", "10:10:00")
  ).build();

  private static final Trip TRIP = ENV.tripData(TRIP_ID).trip();
  private static final TripPattern PATTERN = ENV.tripData(TRIP_ID).tripPattern();
  private static final TripTimes SCHEDULED_TIMES = ENV.tripData(TRIP_ID).scheduledTripTimes();

  private static final TripOnServiceDate TRIP_ON_SERVICE_DATE = TripOnServiceDate.of(
    FeedScopedIdForTestFactory.id("tosd-1")
  )
    .withTrip(TRIP)
    .withServiceDate(SERVICE_DATE)
    .build();

  @Test
  void realTimeJourneyState_isNull_whenPatternNotFound() {
    var transitService = mock(TransitService.class);
    when(transitService.findPattern(TRIP, SERVICE_DATE)).thenReturn(null);
    assertNull(fetch(transitService));
  }

  @Test
  void realTimeJourneyState_isNull_whenTimetableNotFound() {
    var transitService = mock(TransitService.class);
    when(transitService.findPattern(TRIP, SERVICE_DATE)).thenReturn(PATTERN);
    when(transitService.findTimetable(PATTERN, SERVICE_DATE)).thenReturn(null);
    assertNull(fetch(transitService));
  }

  @Test
  void realTimeJourneyState_allFlagsFalse_forScheduledTrip() {
    var state = fetch(withTripTimes(SCHEDULED_TIMES.createRealTimeFromScheduledTimes().build()));
    assertEquals(false, state.get("extraJourney"));
    assertEquals(false, state.get("cancellation"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("timesModified"));
    assertEquals(false, state.get("journeyPatternModified"));
    assertEquals(false, state.get("updated"));
  }

  @Test
  void realTimeJourneyState_cancellationFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withCanceled();
    var state = fetch(withTripTimes(builder.build()));
    assertEquals(true, state.get("cancellation"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("extraJourney"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("timesModified"));
    assertEquals(false, state.get("journeyPatternModified"));
  }

  @Test
  void realTimeJourneyState_timesModifiedFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withRealTimeUpdated();
    var state = fetch(withTripTimes(builder.build()));
    assertEquals(true, state.get("timesModified"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("cancellation"));
    assertEquals(false, state.get("extraJourney"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("journeyPatternModified"));
  }

  @Test
  void realTimeJourneyState_extraJourneyFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withAdded();
    var state = fetch(withTripTimes(builder.build()));
    assertEquals(true, state.get("extraJourney"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("cancellation"));
  }

  @Test
  void realTimeJourneyState_journeyPatternModifiedFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withModifiedTripPattern();
    var state = fetch(withTripTimes(builder.build()));
    assertEquals(true, state.get("journeyPatternModified"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("cancellation"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Returns a TransitService mock that resolves the given TripTimes for TRIP on SERVICE_DATE. */
  private static TransitService withTripTimes(TripTimes tripTimes) {
    var timetable = mock(Timetable.class);
    when(timetable.getTripTimes(TRIP)).thenReturn(tripTimes);

    var transitService = mock(TransitService.class);
    when(transitService.findPattern(TRIP, SERVICE_DATE)).thenReturn(PATTERN);
    when(transitService.findTimetable(PATTERN, SERVICE_DATE)).thenReturn(timetable);
    return transitService;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> fetch(TransitService transitService) {
    var context = new TransmodelRequestContext(null, null, transitService, null);
    var result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query(
          "{ dsj { realTimeJourneyState { extraJourney cancellation deleted timesModified journeyPatternModified updated } } }"
        )
        .root(TRIP_ON_SERVICE_DATE)
        .context(context)
        .build()
    );
    assert result.getErrors().isEmpty() : "GraphQL errors: " + result.getErrors();
    Map<String, Object> data = result.getData();
    Map<String, Object> dsj = (Map<String, Object>) data.get("dsj");
    return (Map<String, Object>) dsj.get("realTimeJourneyState");
  }

  private static final GraphQL GRAPHQL = buildGraphQL();

  private static GraphQL buildGraphQL() {
    var realTimeJourneyStateType = RealTimeTripStateType.create();
    var dsjType = new DatedServiceJourneyType(mock(FeedScopedIdMapper.class)).create(
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

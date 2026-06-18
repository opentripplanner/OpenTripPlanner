package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelDirectives;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.TripTimes;

class LegTypeRealTimeTripStateTest {

  private static final String TRIP_ID = "t1";
  private static final ZonedDateTime TIME = ZonedDateTime.parse("2025-01-15T10:00:00+01:00");

  // Build the transit environment once and derive pattern + scheduled times from it.
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private static final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private static final RegularStop STOP_B = ENV_BUILDER.stop("B");
  private static final TransitTestEnvironment ENV = ENV_BUILDER.addTrip(
    TripInput.of(TRIP_ID)
      .addStop(STOP_A, "10:00:00", "10:00:00")
      .addStop(STOP_B, "10:10:00", "10:10:00")
  ).build();
  private static final TripPattern PATTERN = ENV.tripData(TRIP_ID).tripPattern();
  private static final TripTimes SCHEDULED_TIMES = ENV.tripData(TRIP_ID).scheduledTripTimes();

  private static final StreetLeg WALK_LEG = StreetLeg.of()
    .withStartTime(TIME)
    .withEndTime(TIME.plusMinutes(5))
    .withMode(TraverseMode.WALK)
    .build();

  private static final GraphQL GRAPHQL = buildGraphQL();

  @Test
  void realTimeTripState_isNull_forNonTransitLeg() {
    assertNull(fetchState(WALK_LEG));
  }

  @Test
  void realTimeTripState_allFlagsFalse_forScheduledTransitLeg() {
    var leg = transitLeg(SCHEDULED_TIMES.createRealTimeFromScheduledTimes());
    var state = fetchState(leg);
    assertEquals(false, state.get("added"));
    assertEquals(false, state.get("canceled"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("timesModified"));
    assertEquals(false, state.get("tripPatternModified"));
    assertEquals(false, state.get("updated"));
  }

  @Test
  void realTimeTripState_canceledFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withCanceled();
    var state = fetchState(transitLeg(builder));
    assertEquals(true, state.get("canceled"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("added"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("timesModified"));
    assertEquals(false, state.get("tripPatternModified"));
  }

  @Test
  void realTimeTripState_timesModifiedFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withRealTimeUpdated();
    var state = fetchState(transitLeg(builder));
    assertEquals(true, state.get("timesModified"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("canceled"));
    assertEquals(false, state.get("added"));
    assertEquals(false, state.get("deleted"));
    assertEquals(false, state.get("tripPatternModified"));
  }

  @Test
  void realTimeTripState_addedFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withAdded();
    var state = fetchState(transitLeg(builder));
    assertEquals(true, state.get("added"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("canceled"));
  }

  @Test
  void realTimeTripState_tripPatternModifiedFlag() {
    var builder = SCHEDULED_TIMES.createRealTimeFromScheduledTimes();
    builder.withModifiedTripPattern();
    var state = fetchState(transitLeg(builder));
    assertEquals(true, state.get("tripPatternModified"));
    assertEquals(true, state.get("updated"));
    assertEquals(false, state.get("canceled"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static ScheduledTransitLeg transitLeg(RealTimeTripTimesBuilder builder) {
    return new ScheduledTransitLegBuilder<>()
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withZoneId(TIME.getZone())
      .withServiceDate(TIME.toLocalDate())
      .withTripTimes(builder.build())
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1)
      .withTripPattern(PATTERN)
      .build();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> fetchState(Object legSource) {
    var result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query(
          "{ leg { realTimeTripState { added canceled deleted timesModified tripPatternModified updated } } }"
        )
        .root(legSource)
        .build()
    );
    assert result.getErrors().isEmpty() : "GraphQL errors: " + result.getErrors();
    Map<String, Object> data = result.getData();
    Map<String, Object> leg = (Map<String, Object>) data.get("leg");
    return (Map<String, Object>) leg.get("realTimeTripState");
  }

  private static GraphQL buildGraphQL() {
    var realTimeTripStateType = RealTimeTripStateType.create();

    // Build LegType with type-reference stubs for all parameters not under test.
    // The realTimeTripState data fetcher only accesses env.getSource() at runtime.
    var legType = LegType.create(
      ref("BookingArrangement"),
      ref("Interchange"),
      ref("PointsOnLink"),
      ref("Authority"),
      ref("Operator"),
      ref("Quay"),
      ref("EstimatedCall"),
      ref("Line"),
      ref("ServiceJourney"),
      ref("DatedServiceJourney"),
      ref("PtSituationElement"),
      stubObjectType("Place"),
      stubObjectType("PathGuidance"),
      ref("ElevationProfileStep"),
      stubObjectType("Emission"),
      realTimeTripStateType,
      ExtendedScalars.DateTime
    );

    // Expose `leg` at the query root, sourced from the execution root object.
    var queryType = GraphQLObjectType.newObject()
      .name("Query")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("leg")
          .type(legType)
          .dataFetcher(DataFetchingEnvironment::getRoot)
          .build()
      )
      .build();

    return GraphQL.newGraphQL(
      GraphQLSchema.newSchema()
        .query(queryType)
        // Register all stub types so GraphQL-Java can resolve the type references
        .additionalType(realTimeTripStateType)
        .additionalType(stubObjectType("BookingArrangement"))
        .additionalType(stubObjectType("Interchange"))
        .additionalType(stubObjectType("PointsOnLink"))
        .additionalType(stubObjectType("Authority"))
        .additionalType(stubObjectType("Operator"))
        .additionalType(stubObjectType("Quay"))
        .additionalType(stubObjectType("EstimatedCall"))
        .additionalType(stubObjectType("Line"))
        .additionalType(stubObjectType("ServiceJourney"))
        .additionalType(stubObjectType("DatedServiceJourney"))
        .additionalType(stubObjectType("PtSituationElement"))
        .additionalType(stubObjectType("ElevationProfileStep"))
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

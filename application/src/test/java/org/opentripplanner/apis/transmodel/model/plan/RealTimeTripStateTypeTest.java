package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.transmodel.model.timetable.RealTimeTripStateType;
import org.opentripplanner.apis.transmodel.model.timetable.TransmodelRealTimeTripStateModel;

class RealTimeTripStateTypeTest {

  private static final TransmodelRealTimeTripStateModel ALL_FALSE =
    new TransmodelRealTimeTripStateModel(false, false, false, false, false);

  private static final TransmodelRealTimeTripStateModel ALL_TRUE =
    new TransmodelRealTimeTripStateModel(true, true, true, true, true);

  // Execute a GraphQL query that selects all six fields from a RealTimeTripState source object.
  private static final GraphQL GRAPHQL = buildGraphQL();

  @Test
  void allFlagsAreFalse_whenNoUpdatesPresent() {
    var result = execute(ALL_FALSE);
    assertFalse(flag(result, "extraJourney"));
    assertFalse(flag(result, "cancellation"));
    assertFalse(flag(result, "timesModified"));
    assertFalse(flag(result, "journeyPatternModified"));
    assertFalse(flag(result, "updated"));
  }

  @Test
  void allFlagsAreTrue_whenAllUpdatesPresent() {
    var result = execute(ALL_TRUE);
    assertTrue(flag(result, "extraJourney"));
    assertTrue(flag(result, "cancellation"));
    assertTrue(flag(result, "timesModified"));
    assertTrue(flag(result, "journeyPatternModified"));
    assertTrue(flag(result, "updated"));
  }

  // Individual flag tests guard against copy-paste errors in the six nearly-identical
  // field definitions in RealTimeTripStateType.

  @Test
  void extraJourneyFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(true, false, false, false, false);
    var result = execute(model);
    assertTrue(flag(result, "extraJourney"));
    assertFalse(flag(result, "cancellation"));
    assertFalse(flag(result, "timesModified"));
    assertFalse(flag(result, "journeyPatternModified"));
    assertFalse(flag(result, "updated"));
  }

  @Test
  void cancellationFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, true, false, false, false);
    var result = execute(model);
    assertFalse(flag(result, "extraJourney"));
    assertTrue(flag(result, "cancellation"));
  }

  @Test
  void timesModifiedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, true, false, false);
    var result = execute(model);
    assertTrue(flag(result, "timesModified"));
    assertFalse(flag(result, "journeyPatternModified"));
  }

  @Test
  void journeyPatternModifiedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, false, true, false);
    var result = execute(model);
    assertFalse(flag(result, "timesModified"));
    assertTrue(flag(result, "journeyPatternModified"));
    assertFalse(flag(result, "updated"));
  }

  @Test
  void updatedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, false, false, true);
    var result = execute(model);
    assertFalse(flag(result, "journeyPatternModified"));
    assertTrue(flag(result, "updated"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static boolean flag(Map<String, Object> state, String name) {
    return (boolean) state.get(name);
  }

  @SuppressWarnings("unchecked")
  private static java.util.Map<String, Object> execute(TransmodelRealTimeTripStateModel model) {
    var result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query(
          "{ state { extraJourney cancellation timesModified journeyPatternModified updated } }"
        )
        .root(model)
        .build()
    );
    assert result.getErrors().isEmpty() : "GraphQL errors: " + result.getErrors();
    java.util.Map<String, Object> data = result.getData();
    return (java.util.Map<String, Object>) data.get("state");
  }

  private static GraphQL buildGraphQL() {
    var realTimeTripStateType = RealTimeTripStateType.create();

    // Wrap in a Query type so graphql-java can build an executable schema
    var queryType = GraphQLObjectType.newObject()
      .name("Query")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("state")
          .type(new GraphQLNonNull(realTimeTripStateType))
          .dataFetcher(env -> env.getRoot())
          .build()
      )
      .build();

    var schema = GraphQLSchema.newSchema().query(queryType).build();
    return GraphQL.newGraphQL(schema).build();
  }
}

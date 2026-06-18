package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;

class RealTimeTripStateTypeTest {

  private static final TransmodelRealTimeTripStateModel ALL_FALSE =
    new TransmodelRealTimeTripStateModel(false, false, false, false, false, false);

  private static final TransmodelRealTimeTripStateModel ALL_TRUE =
    new TransmodelRealTimeTripStateModel(true, true, true, true, true, true);

  // Execute a GraphQL query that selects all six fields from a RealTimeTripState source object.
  private static final GraphQL GRAPHQL = buildGraphQL();

  @Test
  void allFlagsAreFalse_whenNoUpdatesPresent() {
    var result = execute(ALL_FALSE);
    assertEquals(false, result.get("added"));
    assertEquals(false, result.get("canceled"));
    assertEquals(false, result.get("deleted"));
    assertEquals(false, result.get("timesModified"));
    assertEquals(false, result.get("tripPatternModified"));
    assertEquals(false, result.get("updated"));
  }

  @Test
  void allFlagsAreTrue_whenAllUpdatesPresent() {
    var result = execute(ALL_TRUE);
    assertEquals(true, result.get("added"));
    assertEquals(true, result.get("canceled"));
    assertEquals(true, result.get("deleted"));
    assertEquals(true, result.get("timesModified"));
    assertEquals(true, result.get("tripPatternModified"));
    assertEquals(true, result.get("updated"));
  }

  // Individual flag tests guard against copy-paste errors in the six nearly-identical
  // field definitions in RealTimeTripStateType.

  @Test
  void addedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(true, false, false, false, false, false);
    var result = execute(model);
    assertEquals(true, result.get("added"));
    assertEquals(false, result.get("canceled"));
    assertEquals(false, result.get("deleted"));
    assertEquals(false, result.get("timesModified"));
    assertEquals(false, result.get("tripPatternModified"));
    assertEquals(false, result.get("updated"));
  }

  @Test
  void canceledFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, true, false, false, false, false);
    var result = execute(model);
    assertEquals(false, result.get("added"));
    assertEquals(true, result.get("canceled"));
    assertEquals(false, result.get("deleted"));
  }

  @Test
  void deletedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, true, false, false, false);
    var result = execute(model);
    assertEquals(false, result.get("canceled"));
    assertEquals(true, result.get("deleted"));
    assertEquals(false, result.get("timesModified"));
  }

  @Test
  void timesModifiedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, false, true, false, false);
    var result = execute(model);
    assertEquals(false, result.get("deleted"));
    assertEquals(true, result.get("timesModified"));
    assertEquals(false, result.get("tripPatternModified"));
  }

  @Test
  void tripPatternModifiedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, false, false, true, false);
    var result = execute(model);
    assertEquals(false, result.get("timesModified"));
    assertEquals(true, result.get("tripPatternModified"));
    assertEquals(false, result.get("updated"));
  }

  @Test
  void updatedFlagIsIsolated() {
    var model = new TransmodelRealTimeTripStateModel(false, false, false, false, false, true);
    var result = execute(model);
    assertEquals(false, result.get("tripPatternModified"));
    assertEquals(true, result.get("updated"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static java.util.Map<String, Object> execute(TransmodelRealTimeTripStateModel model) {
    var result = GRAPHQL.execute(
      ExecutionInput.newExecutionInput()
        .query("{ state { added canceled deleted timesModified tripPatternModified updated } }")
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

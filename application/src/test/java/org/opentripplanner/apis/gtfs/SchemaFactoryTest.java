package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.Value;
import graphql.schema.AsyncDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.api.request.RouteRequest;

public class SchemaFactoryTest {

  @Test
  void createSchema() {
    var schema = SchemaFactory.createSchemaWithDefaultInjection(new RouteRequest());
    assertNotNull(schema);
  }

  @Test
  void testDefaultValueInjection() {
    var routeRequest = new RouteRequest();
    double walkSpeed = 15;
    routeRequest.withPreferences(preferences ->
      preferences.withWalk(walk -> walk.withSpeed(walkSpeed))
    );
    var maxTransfers = 2;
    routeRequest.withPreferences(preferences ->
      preferences.withTransfer(transfer -> transfer.withMaxTransfers(maxTransfers + 1))
    );
    var numItineraries = 63;
    routeRequest.setNumItineraries(numItineraries);
    var schema = SchemaFactory.createSchemaWithDefaultInjection(routeRequest);
    assertNotNull(schema);

    var defaultSpeed = (FloatValue) getDefaultValueForField(
      schema,
      "WalkPreferencesInput",
      "speed"
    );
    assertEquals(walkSpeed, defaultSpeed.getValue().doubleValue(), 0.01f);

    var defaultMaxTransfers = (IntValue) getDefaultValueForField(
      schema,
      "TransferPreferencesInput",
      "maximumTransfers"
    );
    assertEquals(maxTransfers, defaultMaxTransfers.getValue().intValue());

    var defaultNumberOfItineraries = (IntValue) getDefaultValueForArgument(
      schema,
      "planConnection",
      "first"
    );
    assertEquals(numItineraries, defaultNumberOfItineraries.getValue().intValue());
  }

  @ValueSource(strings = { "plan", "nearest" })
  @ParameterizedTest(name = "\"{0}\" must be a an async fetcher")
  void asyncDataFetchers(String fieldName) {
    OTPFeature.AsyncGraphQLFetchers.testOn(() -> {
      var schema = SchemaFactory.createSchemaWithDefaultInjection(new RouteRequest());
      var fetcher = getQueryType(fieldName, schema);
      assertSame(fetcher.getClass(), AsyncDataFetcher.class);
    });
    OTPFeature.AsyncGraphQLFetchers.testOff(() -> {
      var schema = SchemaFactory.createSchemaWithDefaultInjection(new RouteRequest());
      var fetcher = getQueryType(fieldName, schema);
      assertNotSame(fetcher.getClass(), AsyncDataFetcher.class);
    });
  }

  private static DataFetcher<?> getQueryType(String fieldName, GraphQLSchema schema) {
    return schema
      .getCodeRegistry()
      .getDataFetcher(
        FieldCoordinates.coordinates("QueryType", fieldName),
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name(fieldName)
          .type(GraphQLObjectType.newObject().name(fieldName).build())
          .build()
      );
  }

  private static Value getDefaultValueForField(
    GraphQLSchema schema,
    String inputObjectName,
    String fieldName
  ) {
    GraphQLInputObjectType inputObject = schema.getTypeAs(inputObjectName);
    return (Value) inputObject.getField(fieldName).getInputFieldDefaultValue().getValue();
  }

  private static Value getDefaultValueForArgument(
    GraphQLSchema schema,
    String queryName,
    String argumentName
  ) {
    var query = schema.getQueryType().getField(queryName);
    return (Value) query.getArgument(argumentName).getArgumentDefaultValue().getValue();
  }
}

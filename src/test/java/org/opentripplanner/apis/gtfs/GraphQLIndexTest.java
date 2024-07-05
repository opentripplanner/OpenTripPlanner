package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import graphql.schema.AsyncDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.application.OTPFeature;

public class GraphQLIndexTest {

  @Test
  public void build() {
    var schema = GtfsGraphQLIndex.buildSchema();
    assertNotNull(schema);
  }

  @ValueSource(strings = { "plan", "nearest" })
  @ParameterizedTest(name = "\"{0}\" must be a an async fetcher")
  void asyncDataFetchers(String fieldName) {
    OTPFeature.AsyncGraphQLFetchers.testOn(() -> {
      var schema = GtfsGraphQLIndex.buildSchema();
      var fetcher = getQueryType(fieldName, schema);
      assertSame(fetcher.getClass(), AsyncDataFetcher.class);
    });
    OTPFeature.AsyncGraphQLFetchers.testOff(() -> {
      var schema = GtfsGraphQLIndex.buildSchema();
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
}

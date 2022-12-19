package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import graphql.schema.AsyncDataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GraphQLIndexTest {

  @Test
  public void build() {
    var schema = LegacyGraphQLIndex.buildSchema();
    assertNotNull(schema);
  }

  @ValueSource(strings = { "plan", "nearest" })
  @ParameterizedTest(name = "\"{0}\" must be a an async fetcher")
  void asyncDataFetchers(String fieldName) {
    var schema = LegacyGraphQLIndex.buildSchema();

    var x = schema
      .getCodeRegistry()
      .getDataFetcher(
        FieldCoordinates.coordinates("QueryType", fieldName),
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name(fieldName)
          .type(GraphQLObjectType.newObject().name(fieldName).build())
          .build()
      );

    assertEquals(x.getClass(), AsyncDataFetcher.class);
  }
}

package org.opentripplanner.apis.transmodel.model.scalars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.GraphQL;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TimeScalarFactoryTest {

  @ParameterizedTest
  @CsvSource({
    "0, 00:00:00",
    "43200, 12:00:00",
    "86399, 23:59:59",
    "90000, 01:00:00",
    "-180, 23:57:00",
    "-86400, 00:00:00",
  })
  void serializeTime(int secondsSinceMidnight, String expected) {
    var subject = TimeScalarFactory.createSecondsSinceMidnightAsTimeStringScalar();

    assertEquals(expected, subject.getCoercing().serialize(secondsSinceMidnight));
  }

  @ParameterizedTest
  @CsvSource({
    "0, 00:00:00, 0",
    "86399, 23:59:59, 0",
    "90000, 01:00:00, 1",
    "-180, 23:57:00, -1",
    "-86400, 00:00:00, -1",
    "-86401, 23:59:59, -2",
  })
  void timeAndDayOffset(int secondsSinceMidnight, String expectedTime, int expectedDayOffset) {
    var result = queryTimeAndDayOffset(secondsSinceMidnight);

    assertEquals(Map.of("time", expectedTime, "dayOffset", expectedDayOffset), result);
  }

  private static Object queryTimeAndDayOffset(int secondsSinceMidnight) {
    var query = GraphQLObjectType.newObject()
      .name("Query")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("passingTime")
          .type(TimeScalarFactory.createSecondsSinceMidnightAsTimeObject())
          .dataFetcher(environment -> secondsSinceMidnight)
          .build()
      )
      .build();
    var graphQL = GraphQL.newGraphQL(GraphQLSchema.newSchema().query(query).build()).build();

    var result = graphQL.execute("{ passingTime { time dayOffset } }");

    assertEquals(List.of(), result.getErrors());
    Map<String, Object> data = result.getData();
    return data.get("passingTime");
  }
}

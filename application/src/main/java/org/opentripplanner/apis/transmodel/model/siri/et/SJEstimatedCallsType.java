package org.opentripplanner.apis.transmodel.model.siri.et;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import java.util.List;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.model.TripTimeOnDate;

public class SJEstimatedCallsType {

  private static final String NAME = "SJEstimatedCalls";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "List of visits to quays as part of a vehicle journey, relative to the current quay and for a given date. Includes real-time updates"
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("first")
          .description("The first call in this service journey")
          .type(new GraphQLNonNull(EstimatedCallType.REF))
          .dataFetcher(environment -> tripTimeOnDates(environment).getFirst())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("last")
          .description("The last call in this service journey")
          .type(new GraphQLNonNull(EstimatedCallType.REF))
          .dataFetcher(environment -> tripTimeOnDates(environment).getLast())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("next")
          .description(
            "The list of subsequent calls in this service journey after the current call"
          )
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(EstimatedCallType.REF))))
          .argument(
            GraphQLArgument.newArgument()
              .name("count")
              .description(
                "Number of subsequent calls to return. The default value (null) returns all subsequent calls"
              )
              .type(Scalars.GraphQLInt)
          )
          .dataFetcher(environment -> {
            Integer count = environment.getArgument("count");
            if (count == null) {
              return environment.<TripTimeOnDate>getSource().nextTimes();
            }
            checkStrictlyPositive(count);
            return environment.<TripTimeOnDate>getSource().nextTimes(count);
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("previous")
          .description("The list of previous calls in this service journey before the current call")
          .type(new GraphQLNonNull(new GraphQLList(new GraphQLNonNull(EstimatedCallType.REF))))
          .argument(
            GraphQLArgument.newArgument()
              .name("count")
              .description(
                "Number of previous calls to return. The default value (null) returns all previous calls"
              )
              .type(Scalars.GraphQLInt)
          )
          .dataFetcher(environment -> {
            Integer count = environment.getArgument("count");
            if (count == null) {
              return environment.<TripTimeOnDate>getSource().previousTimes();
            }
            checkStrictlyPositive(count);
            return environment.<TripTimeOnDate>getSource().previousTimes(count);
          })
          .build()
      )
      .build();
  }

  private static void checkStrictlyPositive(int count) {
    if (count <= 0) {
      throw new IllegalArgumentException(
        "The argument 'count' should be a strictly positive value: " + count
      );
    }
  }

  private static List<TripTimeOnDate> tripTimeOnDates(DataFetchingEnvironment environment) {
    return GqlUtil.getTransitService(environment)
      .findTripTimesOnDate(
        environment.<TripTimeOnDate>getSource().getTrip(),
        environment.<TripTimeOnDate>getSource().getServiceDay()
      )
      .orElse(List.of());
  }
}

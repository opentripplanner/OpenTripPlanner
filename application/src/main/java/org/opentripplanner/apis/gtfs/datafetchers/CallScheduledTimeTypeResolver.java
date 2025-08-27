package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime;

public class CallScheduledTimeTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    CallScheduledTime o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o == null) {
      return null;
    }

    return switch (o) {
      case CallScheduledTime.ArrivalDepartureTime adt -> schema.getObjectType(
        "ArrivalDepartureTime"
      );
      case CallScheduledTime.TimeWindow tw -> schema.getObjectType("TimeWindow");
    };
  }
}

package org.opentripplanner.apis.gtfs.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;

public interface StopPosition extends GraphQLDataFetchers.GraphQLStopPosition {
  record PositionAtStop(int position) implements StopPosition {}

  record PositionBetweenStops(int previousPosition, int nextPosition) implements StopPosition {}

  @Override
  default GraphQLObjectType getType(TypeResolutionEnvironment env) {
    var schema = env.getSchema();
    Object o = env.getObject();
    if (o instanceof PositionAtStop) {
      return schema.getObjectType("PositionAtStop");
    }
    if (o instanceof PositionBetweenStops) {
      return schema.getObjectType("PositionBetweenStops");
    }
    return null;
  }
}

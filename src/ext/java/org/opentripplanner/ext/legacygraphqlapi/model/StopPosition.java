package org.opentripplanner.ext.legacygraphqlapi.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public interface StopPosition extends LegacyGraphQLDataFetchers.LegacyGraphQLStopPosition {
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

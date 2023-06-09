package org.opentripplanner.ext.legacygraphqlapi.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public record PositionAtStop(int position)
  implements LegacyGraphQLDataFetchers.LegacyGraphQLStopPosition {
  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    var schema = env.getSchema();
    return schema.getObjectType("PositionAtStop");
  }
}

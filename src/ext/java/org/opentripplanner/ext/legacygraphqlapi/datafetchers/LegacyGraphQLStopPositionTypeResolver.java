package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.ext.legacygraphqlapi.model.StopPosition;

public class LegacyGraphQLStopPositionTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof StopPosition.PositionAtStop) {
      return schema.getObjectType("PositionAtStop");
    }

    return null;
  }
}

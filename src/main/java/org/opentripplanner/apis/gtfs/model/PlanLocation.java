package org.opentripplanner.apis.gtfs.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.StopLocation;

public class PlanLocation implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Object o = env.getObject();
    GraphQLSchema schema = env.getSchema();

    if (o instanceof StopLocation) {
      return schema.getObjectType("Stop");
    }
    if (o instanceof WgsCoordinate) {
      return schema.getObjectType("Coordinate");
    }

    return null;
  }
}

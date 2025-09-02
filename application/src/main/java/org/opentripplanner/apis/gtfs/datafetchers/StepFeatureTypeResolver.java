package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.plan.walkstep.VerticalTransportationUse;
import org.opentripplanner.transit.model.site.Entrance;

public class StepFeatureTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Entrance) {
      return schema.getObjectType("Entrance");
    } else if (o instanceof VerticalTransportationUse) {
      return schema.getObjectType("VerticalTransportationUse");
    }
    return null;
  }
}

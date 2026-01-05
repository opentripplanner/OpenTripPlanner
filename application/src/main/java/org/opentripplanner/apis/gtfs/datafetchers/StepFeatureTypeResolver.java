package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.ElevatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.EscalatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.StairsUse;
import org.opentripplanner.transit.model.site.Entrance;

public class StepFeatureTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof Entrance) {
      return schema.getObjectType("Entrance");
    } else if (o instanceof ElevatorUse) {
      return schema.getObjectType("ElevatorUse");
    } else if (o instanceof EscalatorUse) {
      return schema.getObjectType("EscalatorUse");
    } else if (o instanceof StairsUse) {
      return schema.getObjectType("StairsUse");
    }
    return null;
  }
}

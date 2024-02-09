package org.opentripplanner.apis.gtfs.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;

public interface PlanLocation extends GraphQLDataFetchers.GraphQLPlanLocation {
  @Override
  default GraphQLObjectType getType(TypeResolutionEnvironment env) {
    return null;
  }
}

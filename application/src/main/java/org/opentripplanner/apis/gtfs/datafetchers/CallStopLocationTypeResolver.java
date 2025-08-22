package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;

public class CallStopLocationTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof RegularStop) {
      return schema.getObjectType("Stop");
    }
    if (o instanceof AreaStop) {
      return schema.getObjectType("Location");
    }
    if (o instanceof GroupStop) {
      return schema.getObjectType("LocationGroup");
    }
    return null;
  }
}

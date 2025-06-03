package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.fare.FareOffer;

public class FareProductTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof FareOffer.DefaultFareOffer) {
      return schema.getObjectType("DefaultFareProduct");
    } else if (o instanceof FareOffer.DependentFareOffer) {
      return schema.getObjectType("DependentFareProduct");
    }
    return null;
  }
}

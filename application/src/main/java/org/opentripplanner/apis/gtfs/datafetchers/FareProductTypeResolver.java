package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductLike;

public class FareProductTypeResolver implements TypeResolver {

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
    Object o = environment.getObject();
    GraphQLSchema schema = environment.getSchema();

    if (o instanceof FareProductLike.DefaultFareProduct fp) {
      return schema.getObjectType("DefaultFareProduct");
    } else if (o instanceof FareProductLike.DependentFareProduct dp) {
      return schema.getObjectType("DependentFareProduct");
    }
    return null;
  }
}

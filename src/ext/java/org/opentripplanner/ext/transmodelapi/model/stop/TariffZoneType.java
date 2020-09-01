package org.opentripplanner.ext.transmodelapi.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FareZone;

public class TariffZoneType {
  private static final String NAME = "TariffZone";

  public static GraphQLObjectType createTZ() {
    return GraphQLObjectType
        .newObject()
        .name(NAME)
        .field(GqlUtil.newTransitIdField())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("name")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((FareZone) environment.getSource()).getName())
            .build())
        .build();
  }
}

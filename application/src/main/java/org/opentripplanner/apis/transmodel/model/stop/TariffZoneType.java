package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.site.FareZone;

public class TariffZoneType {

  private static final String NAME = "TariffZone";

  public static GraphQLObjectType createTZ() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .field(GqlUtil.newTransitIdField())
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("name")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((FareZone) environment.getSource()).getName())
          .build()
      )
      .build();
  }
}

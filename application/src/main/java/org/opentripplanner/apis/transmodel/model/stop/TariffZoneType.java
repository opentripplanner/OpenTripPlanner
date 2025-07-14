package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.transit.model.site.FareZone;

public class TariffZoneType {

  private static final String NAME = "TariffZone";

  public static GraphQLObjectType createTZ(IdResolver idResolver) {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .field(GqlUtil.newTransitIdField(idResolver))
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

package org.opentripplanner.apis.transmodel.model.stop;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.transit.model.site.FareZone;

public class TariffZoneType {

  private final FeedScopedIdMapper idMapper;

  private static final String NAME = "TariffZone";

  public TariffZoneType(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  public GraphQLObjectType createTZ() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .field(GqlUtil.newTransitIdField(idMapper))
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

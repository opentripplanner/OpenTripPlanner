package org.opentripplanner.apis.transmodel.model.stop;

import graphql.schema.GraphQLEnumType;
import org.opentripplanner.transit.model.site.StopType;

/**
 * Maps the StopType enum to a GraphQL enum.
 */
public class StopTypeMapper {

  public static final GraphQLEnumType STOP_TYPE = GraphQLEnumType
    .newEnum()
    .name("StopType")
    .value("regular", StopType.REGULAR, "A regular stop defined geographically as a point.")
    .value(
      "flexible_area",
      StopType.FLEXIBLE_AREA,
      "Boarding and alighting is allowed anywhere within the geographic area of this stop."
    )
    .value(
      "flexible_group",
      StopType.FLEXIBLE_GROUP,
      "A stop that consists of multiple other stops, area or regular."
    )
    .build();
}

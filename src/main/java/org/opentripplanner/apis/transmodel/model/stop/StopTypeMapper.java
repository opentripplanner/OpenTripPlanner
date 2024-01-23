package org.opentripplanner.apis.transmodel.model.stop;

import graphql.schema.GraphQLEnumType;
import org.opentripplanner.transit.model.site.StopType;

public class StopTypeMapper {

  public static final GraphQLEnumType STOP_TYPE = GraphQLEnumType
    .newEnum()
    .name("StopType")
    .value("regular", StopType.REGULAR)
    .value("flexible_area", StopType.FLEXIBLE_AREA)
    .value("flexible_group", StopType.FLEXIBLE_GROUP)
    .build();
}

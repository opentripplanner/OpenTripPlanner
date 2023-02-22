package org.opentripplanner.ext.transmodelapi.model.framework;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.STREET_MODE;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

public class StreetModeDurationInputType {

  public static GraphQLInputObjectType create(GqlUtil gqlUtil) {
    return GraphQLInputObjectType
      .newInputObject()
      .name("StreetModeDurationInput")
      .description("A combination of street mode and corresponding duration")
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("streetMode")
          .type(new GraphQLNonNull(STREET_MODE))
          .build()
      )
      .field(
        GraphQLInputObjectField
          .newInputObjectField()
          .name("duration")
          .type(new GraphQLNonNull(gqlUtil.durationScalar))
      )
      .build();
  }
}

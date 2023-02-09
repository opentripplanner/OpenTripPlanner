package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

public class MaxAccessEgressInputType {

  public static GraphQLInputObjectType create(GqlUtil gqlUtil) {
    return GraphQLInputObjectType
      .newInputObject()
      .name("MaxAccessEgressInput")
      .description("Maximum duration for access/egress for street searches per respective mode.")
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("walk")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("bike")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("bikeToPark")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("bikeRental")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("scooterRental")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("car")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("carToPark")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("carPickup")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("carRental")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .field(GraphQLInputObjectField.newInputObjectField()
        .name("flexible")
        .type(gqlUtil.durationScalar)
        .build()
      )
      .build();
  }
}

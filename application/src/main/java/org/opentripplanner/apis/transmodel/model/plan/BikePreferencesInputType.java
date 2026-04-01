package org.opentripplanner.apis.transmodel.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.routing.api.request.preference.BikePreferences;

public class BikePreferencesInputType {

  public static GraphQLInputObjectType create(BikePreferences dft) {
    return GraphQLInputObjectType.newInputObject()
      .name("BikePreferencesInput")
      .description("Bicycle routing preferences.")
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("speed")
          .description("The maximum bike speed along streets, in meters per second.")
          .type(Scalars.GraphQLFloat)
          .defaultValue(dft.speed())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("reluctance")
          .description(
            "A measure of how bad biking is compared to being in transit for equal periods of time. " +
              "Higher values make routing prefer other modes over biking."
          )
          .type(Scalars.GraphQLFloat)
          .defaultValue(dft.reluctance())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("optimisationMethod")
          .description(
            "The set of characteristics that the user wants to optimize for during bicycle routing."
          )
          .type(EnumTypes.VEHICLE_OPTIMISATION_METHOD)
          .defaultValue(dft.optimizeType())
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("triangleFactors")
          .description(
            "When using optimisationMethod 'triangle', these values tell the routing engine " +
              "how important each factor is compared to the others. All values should add up to 1."
          )
          .type(TriangleFactorsInputType.INPUT_TYPE)
          .build()
      )
      .build();
  }
}

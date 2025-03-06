package org.opentripplanner.apis.transmodel.model.plan.legacyvia;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import org.opentripplanner.apis.transmodel.model.framework.CoordinateInputType;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.routing.api.request.ViaLocationDeprecated;

public class ViaLocationInputType {

  public static GraphQLInputObjectType create() {
    return GraphQLInputObjectType.newInputObject()
      .name("ViaLocationInput")
      .description(
        "Input format for specifying a location through either a place reference (id), " +
        "coordinates or both. If both place and coordinates are provided the place ref will be " +
        "used if found, coordinates will only be used if place is not known. The location also " +
        "contain information about the minimum and maximum time the user is willing to stay at " +
        "the via location."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("name")
          .description(
            "The name of the location. This is pass-through information" +
            " and is not used in routing."
          )
          .type(Scalars.GraphQLString)
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("place")
          .description(
            "The id of an element in the OTP model. Currently supports" +
            " Quay, StopPlace, multimodal StopPlace, and GroupOfStopPlaces."
          )
          .type(Scalars.GraphQLString)
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("coordinates")
          .description(
            "Coordinates for the location. This can be used alone or as" +
            " fallback if the place id is not found."
          )
          .type(CoordinateInputType.INPUT_TYPE)
          .build()
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("minSlack")
          .defaultValue(ViaLocationDeprecated.DEFAULT_MIN_SLACK)
          .description(
            "The minimum time the user wants to stay in the via location before continuing his journey"
          )
          .type(TransmodelScalars.DURATION_SCALAR)
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("maxSlack")
          .defaultValue(ViaLocationDeprecated.DEFAULT_MAX_SLACK)
          .description(
            "The maximum time the user wants to stay in the via location before continuing his journey"
          )
          .type(TransmodelScalars.DURATION_SCALAR)
      )
      .build();
  }
}

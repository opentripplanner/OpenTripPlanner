package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLScalarType;

public class LocationInputType {

  public static GraphQLInputObjectType create(GraphQLScalarType dateTimeScalar) {
    return GraphQLInputObjectType.newInputObject()
      .name("Location")
      .description(
        "Input format for specifying a location through either a place reference (id), coordinates " +
          "or both. If both place and coordinates are provided the place ref will be used if found, " +
          "coordinates will only be used if place is not known. " +
          "Alternatively, an onBoardLocation can be used to start the search on-board a vehicle."
      )
      .field(
        GraphQLInputObjectField.newInputObjectField()
          .name("name")
          .description(
            "The name of the location. This is pass-through information" +
              "and is not used in routing."
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
          .name("onBoardLocation")
          .description(
            "Identifies an on-board position on a specific transit trip. " +
              "When set, the search starts from on-board the specified vehicle."
          )
          .type(OnBoardLocationInputType.create(dateTimeScalar))
          .build()
      )
      .build();
  }
}

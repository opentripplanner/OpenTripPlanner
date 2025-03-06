package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

public class LocationInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("Location")
    .description(
      "Input format for specifying a location through either a place reference (id), coordinates " +
      "or both. If both place and coordinates are provided the place ref will be used if found, " +
      "coordinates will only be used if place is not known."
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
    .build();
}

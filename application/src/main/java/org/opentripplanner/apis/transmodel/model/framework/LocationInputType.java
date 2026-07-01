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
          "Alternatively, a serviceJourneyLocation can be used to start the search on-board a vehicle," +
          "or pinpoint a boarding on a specific dated service journey to start routing."
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
          .name("serviceJourneyLocation")
          .description(
            "Identifies a location on a specific transit trip. " +
              "When set, the search starts from the specified vehicle," +
              "considered either to be starting on-board or boarding at the provided stop. " +
              "In this case, the dateTime parameter of the request is ignored, as the time is " +
              "resolved based on timetable data instead."
          )
          .type(ServiceJourneyLocationInputType.create(dateTimeScalar))
          .build()
      )
      .build();
  }
}

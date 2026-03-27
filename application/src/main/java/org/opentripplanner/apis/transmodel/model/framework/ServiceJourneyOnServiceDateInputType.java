package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNonNull;

public class ServiceJourneyOnServiceDateInputType {

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("ServiceJourneyOnServiceDate")
    .description("Identifies a service journey by service journey ID and service date.")
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("serviceJourneyId")
        .description("The service journey ID.")
        .type(new GraphQLNonNull(Scalars.GraphQLString))
        .build()
    )
    .field(
      GraphQLInputObjectField.newInputObjectField()
        .name("serviceDate")
        .description("The service date of the trip, in ISO 8601 format (YYYY-MM-DD).")
        .type(new GraphQLNonNull(TransmodelScalars.DATE_SCALAR))
        .build()
    )
    .build();
}

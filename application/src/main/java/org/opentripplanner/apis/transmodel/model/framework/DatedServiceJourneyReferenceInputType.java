package org.opentripplanner.apis.transmodel.model.framework;

import static graphql.Directives.OneOfDirective;
import static graphql.Scalars.GraphQLString;

import graphql.schema.GraphQLInputObjectType;

public class DatedServiceJourneyReferenceInputType {

  public static final String FIELD_SERVICE_JOURNEY_ON_SERVICE_DATE = "serviceJourneyOnServiceDate";
  public static final String FIELD_DATED_SERVICE_JOURNEY_ID = "datedServiceJourneyId";

  public static final GraphQLInputObjectType INPUT_TYPE = GraphQLInputObjectType.newInputObject()
    .name("DatedServiceJourneyReference")
    .description(
      "Identifies a specific service journey on a specific service date. " +
        "Exactly one of the fields must be set."
    )
    .withDirective(OneOfDirective)
    .field(b ->
      b
        .name(FIELD_SERVICE_JOURNEY_ON_SERVICE_DATE)
        .description("Identifies the service journey by service journey ID and service date.")
        .type(ServiceJourneyOnServiceDateInputType.INPUT_TYPE)
    )
    .field(b ->
      b
        .name(FIELD_DATED_SERVICE_JOURNEY_ID)
        .description(
          "Identifies the service journey by a dated service journey ID " +
            "(e.g. from NeTEx data where a service journey on a date has a unique ID)."
        )
        .type(GraphQLString)
    )
    .build();
}

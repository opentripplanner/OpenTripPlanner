package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.model.transfer.Transfer;

public class InterchangeType {

  public static GraphQLObjectType create(
      GraphQLOutputType lineType, GraphQLOutputType serviceJourneyType
  ) {
    return GraphQLObjectType.newObject()
        .name("Interchange")
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("staySeated")
                .description("Time that the trip departs. NOT IMPLEMENTED")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> false)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("guaranteed")
                .description("Time that the trip departs. NOT IMPLEMENTED")
                .type(Scalars.GraphQLBoolean)
                .dataFetcher(environment -> false)
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("FromLine")
                .type(lineType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getFromRoute())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("ToLine")
                .type(lineType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getToRoute())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("FromServiceJourney")
                .type(serviceJourneyType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getFromTrip())
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("ToServiceJourney")
                .type(serviceJourneyType)
                .dataFetcher(environment -> ((Transfer) environment.getSource()).getToTrip())
                .build())
        .build();
  }
}

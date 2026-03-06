package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.transit.model.network.ReplacementForRelation;

public class ReplacementForRelationType {

  private static final String NAME = "ReplacementForRelation";

  private static final GraphQLTypeReference DATED_SERVICE_JOURNEY_REF = new GraphQLTypeReference(
    "DatedServiceJourney"
  );

  public GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "Relation for indicating the DatedServiceJourney which is replaced by a newer one. Exists as a\n" +
          "place to put additional information on the replacement when we get SIRI 2.1 support."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(DATED_SERVICE_JOURNEY_REF)
          .description("The replaced DatedServiceJourney.")
          .dataFetcher(environment -> replacementForRelation(environment).getTripOnServiceDate())
          .build()
      )
      .build();
  }

  private static ReplacementForRelation replacementForRelation(
    DataFetchingEnvironment environment
  ) {
    return environment.getSource();
  }
}

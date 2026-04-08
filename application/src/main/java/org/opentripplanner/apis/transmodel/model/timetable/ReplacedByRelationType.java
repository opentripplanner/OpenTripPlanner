package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.transit.model.network.ReplacedByRelation;

public class ReplacedByRelationType {

  private static final String NAME = "ReplacedByRelation";

  private static final GraphQLTypeReference DATED_SERVICE_JOURNEY_REF = new GraphQLTypeReference(
    "DatedServiceJourney"
  );

  public GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        "Relation for indicating the DatedServiceJourney which is replacing an older one. Exists as a\n" +
          "place to put additional information on the replacement when we get SIRI 2.1 support."
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("datedServiceJourney")
          .type(DATED_SERVICE_JOURNEY_REF)
          .description("The replacing DatedServiceJourney.")
          .dataFetcher(environment -> replacedByRelation(environment).getTripOnServiceDate())
          .build()
      )
      .build();
  }

  private static ReplacedByRelation replacedByRelation(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}

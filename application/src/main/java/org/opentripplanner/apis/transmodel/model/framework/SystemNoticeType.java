package org.opentripplanner.apis.transmodel.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.model.SystemNotice;

public class SystemNoticeType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name("SystemNotice")
      .description(
        """
        A system notice is used to tag elements with system information for debugging or other
        system related purpose. One use-case is to run a routing search with
        `itineraryFilters.debug=listAll`. This will then tag itineraries instead of removing
        them from the result. This make it possible to inspect the itinerary-filter-chain. A
        SystemNotice only have english text, because the primary user are technical staff, like
        testers and developers.

        **NOTE!** _A SystemNotice is for debugging the system, avoid putting logic on it in the
        client. The tags and usage may change without notice._"""
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("tag")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((SystemNotice) env.getSource()).tag())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("text")
          .type(Scalars.GraphQLString)
          .dataFetcher(env -> ((SystemNotice) env.getSource()).text())
          .build()
      )
      .build();
  }
}

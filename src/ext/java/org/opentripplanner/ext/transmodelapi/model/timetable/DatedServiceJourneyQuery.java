package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.mapping.TransitIdMapper;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.FeedScopedId;

/**
 * A GraphQL query for retrieving data on DatedServiceJourneys
 */
public class DatedServiceJourneyQuery {

    public static GraphQLFieldDefinition createGetById(
            GraphQLOutputType datedServiceJourneyType
    ) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name("datedServiceJourney")
                .type(datedServiceJourneyType)
                .description("Get a single dated service journey based on its id")
                .argument(GraphQLArgument.newArgument()
                        .name("id")
                        .type(Scalars.GraphQLString))
                .dataFetcher(environment -> {
                    FeedScopedId id =
                            TransitIdMapper.mapIDToDomain(environment.getArgument("id"));

                    return GqlUtil.getRoutingService(environment)
                            .getTripOnServiceDateById(id);
                })
                .build();
    }

}


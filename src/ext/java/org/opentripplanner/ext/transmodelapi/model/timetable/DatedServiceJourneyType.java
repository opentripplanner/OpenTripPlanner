package org.opentripplanner.ext.transmodelapi.model.timetable;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.SERVICE_ALTERATION;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.model.TripOnServiceDate;

/**
 * A DatedServiceJourney GraphQL Type for use in endpoints fetching DatedServiceJourney data
 */
public class DatedServiceJourneyType {

    private static final String NAME = "DatedServiceJourney";
    public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

    public static GraphQLObjectType create(GraphQLOutputType serviceJourneyType) {
        return GraphQLObjectType.newObject()
                .name(NAME)
                .description("A planned journey on a specific day")
                .field(GqlUtil.newTransitIdField())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("OperatingDayDate")
                        .type(Scalars.GraphQLString)
                        .dataFetcher(environment -> (
                                tripOnServiceDate(environment).getServiceDate().toString()
                        ))
                )
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ServiceJourney")
                        .type(new GraphQLNonNull(serviceJourneyType))
                        .dataFetcher(environment -> (
                                        tripOnServiceDate(environment).getTrip()
                                )
                        ))
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("TripAlteration")
                        .type(SERVICE_ALTERATION)
                        .dataFetcher(environment -> tripOnServiceDate(environment).getTripAlteration())
                )
                .build();
    }

    private static TripOnServiceDate tripOnServiceDate(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
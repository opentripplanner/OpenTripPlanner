package org.opentripplanner.ext.transmodelapi.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.ext.transmodelapi.model.EnumTypes;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;

public class InterchangeType {

    public static GraphQLObjectType create(
            GraphQLOutputType lineType, GraphQLOutputType serviceJourneyType
    ) {
        return GraphQLObjectType.newObject()
                .name("Interchange")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("staySeated")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> constraint(env).isStaySeated())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("guaranteed")
                        .type(Scalars.GraphQLBoolean)
                        .dataFetcher(env -> constraint(env).isGuaranteed())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("priority")
                        .description(
                                "The transfer priority is used to decide where a transfer should "
                                + "happen, at the highest prioritized location. If the guarantied "
                                + "flag is set it take precedence priority. A guarantied ALLOWED "
                                + "transfer is preferred over a PREFERRED none-guarantied transfer."
                        )
                        .type(EnumTypes.INTERCHANGE_PRIORITY)
                        .dataFetcher(env -> constraint(env).getPriority())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("maximumWaitTime")
                        .description(
                                "Maximum time after scheduled departure time the connecting "
                                + "transport is guarantied to wait for the delayed trip. [NOT "
                                + "RESPECTED DURING ROUTING, JUST PASSED THROUGH]"
                        )
                        .type(Scalars.GraphQLInt)
                        .dataFetcher(env -> constraint(env).getMaxWaitTime())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("FromLine")
                        .deprecate(
                                "This is the same as using the `FromServiceJourney { line }` field.")
                        .type(lineType)
                        .dataFetcher(env -> transfer(env).getFrom().getTrip().getRoute())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ToLine")
                        .deprecate(
                                "This is the same as using the `ToServiceJourney { line }` field.")
                        .type(lineType)
                        .dataFetcher(env -> transfer(env).getTo().getTrip().getRoute())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("FromServiceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(env -> transfer(env).getFrom().getTrip())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("ToServiceJourney")
                        .type(serviceJourneyType)
                        .dataFetcher(env -> transfer(env).getTo().getTrip())
                        .build())
                .build();
    }

    private static ConstrainedTransfer transfer(DataFetchingEnvironment environment) {
        return environment.getSource();
    }

    private static TransferConstraint constraint(DataFetchingEnvironment environment) {
        return transfer(environment).getTransferConstraint();
    }
}

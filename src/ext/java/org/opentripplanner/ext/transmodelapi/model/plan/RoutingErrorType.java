package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.api.mapping.PlannerErrorMapper;

import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.INPUT_FIELD;
import static org.opentripplanner.ext.transmodelapi.model.EnumTypes.ROUTING_ERROR_CODE;

public class RoutingErrorType {
    public static GraphQLObjectType create() {
        return GraphQLObjectType.newObject()
                .name("RoutingError")
                .description("Description of the reason, why the planner did not return any results")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("code")
                        .description("An enum describing the reason")
                        .type(new GraphQLNonNull(ROUTING_ERROR_CODE))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("inputField")
                        .description("An enum describing the field which should be changed, in order for the search to succeed")
                        .type(INPUT_FIELD)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("description")
                        .description("A textual description of why the search failed. The clients are expected to have their own translations based on the code, for user visible error messages.")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .dataFetcher(env -> PlannerErrorMapper.mapMessage(env.getSource()).message.get())
                        .build())
                .build();
    }
}

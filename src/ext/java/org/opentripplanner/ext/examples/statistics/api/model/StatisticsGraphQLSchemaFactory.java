package org.opentripplanner.ext.examples.statistics.api.model;

import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.routing.graph.GraphIndex;

public class StatisticsGraphQLSchemaFactory {

    private GraphQLSchema schema;

    private GraphQLOutputType graphStatisticsType = new GraphQLTypeReference("GraphStatistics");

    private Relay relay = new Relay();

    private GraphQLInterfaceType nodeInterface = relay.nodeInterface(
            e -> (e.getObject() instanceof GraphStatistics)
                    ? (GraphQLObjectType) graphStatisticsType
                    : null
    );

    private StatisticsGraphQLSchemaFactory(final GraphStatistics index) {
        graphStatisticsType = createType(graphStatisticsType)
                .description("Routing graph statistics")
                .field(createId())
                .field(intField("stops"))
                .build()
        ;
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("QueryType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("graphStatistics")
                        .type(graphStatisticsType)
                        .dataFetcher(env -> index)
                        .build()
                )
                .build()
        ;
        schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
    }

    private GraphQLFieldDefinition.Builder createId() {
        return GraphQLFieldDefinition
                .newFieldDefinition()
                .name("id")
                .type(new GraphQLNonNull(Scalars.GraphQLID));
    }

    public static GraphQLSchema createSchema(final GraphIndex index) {
        return new StatisticsGraphQLSchemaFactory(new GraphStatistics(index)).schema;
    }

    private GraphQLFieldDefinition intField(String fieldName) {
        return GraphQLFieldDefinition.newFieldDefinition()
                .name(fieldName)
                .type(new GraphQLNonNull(Scalars.GraphQLInt))
                .build();
    }

    private GraphQLObjectType.Builder createType(GraphQLOutputType ref) {
        return GraphQLObjectType.newObject().name(ref.getName()).withInterface(nodeInterface);
    }
}

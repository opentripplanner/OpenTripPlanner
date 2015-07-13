package org.opentripplanner.index;

import graphql.Scalars;
import graphql.schema.GraphQLFieldArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.GraphIndex;

import java.util.stream.Collectors;

public class IndexGraphQLSchema {

    public GraphQLObjectType agencyType;
    public GraphQLObjectType routeType;
    public GraphQLObjectType queryType;
    public GraphQLSchema indexSchema;

    public IndexGraphQLSchema(GraphIndex index) {

        routeType = GraphQLObjectType.newObject()
                .name("Route")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("shortName")
                        .type(Scalars.GraphQLString)
                        .build())
                .build();

        agencyType = GraphQLObjectType.newObject()
                .name("Agency")
                .description("Agency in the graph")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("id")
                        .description("Agency id")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("name")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("url")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("timezone")
                        .type(new GraphQLNonNull(Scalars.GraphQLString))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("lang")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("phone")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("fareUrl")
                        .type(Scalars.GraphQLString)
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("routes")
                        .type(new GraphQLList(routeType))
                        .dataFetcher(dataFetchingEnvironment -> index.routeForId.values().stream()
                                .filter(route -> route.getAgency() == dataFetchingEnvironment.getSource())
                                .collect(Collectors.toList()))
                        .build())
                .build();

        queryType = GraphQLObjectType.newObject()
                .name("QueryType")
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("agency")
                        .type(agencyType)
                        .argument(GraphQLFieldArgument.newFieldArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(dataFetchingEnvironment ->
                                index.agencyForId.get((String) dataFetchingEnvironment.getArgument("id")))
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("agencies")
                        .type(new GraphQLList(agencyType))
                        .dataFetcher(dataFetchingEnvironment ->
                                index.agencyForId.values())
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition()
                        .name("route")
                        .type(routeType)
                        .argument(GraphQLFieldArgument.newFieldArgument()
                                .name("id")
                                .type(new GraphQLNonNull(Scalars.GraphQLString))
                                .build())
                        .dataFetcher(dataFetchingEnvironment ->
                                index.routeForId.get(GtfsLibrary.convertIdFromString(dataFetchingEnvironment.getArgument("id"))))
                        .build())
                .build();

        indexSchema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();
    }
}

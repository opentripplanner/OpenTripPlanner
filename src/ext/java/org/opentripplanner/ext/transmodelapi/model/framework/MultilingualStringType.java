package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.Map;

public class MultilingualStringType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType
        .newObject()
        .name("MultilingualString")
        .description("Text with language")
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("value")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getValue())
            .build())
        .field(GraphQLFieldDefinition
            .newFieldDefinition()
            .name("language")
            .type(Scalars.GraphQLString)
            .dataFetcher(environment -> ((Map.Entry<String, String>) environment.getSource()).getKey())
            .build())
        .build();
  }
}

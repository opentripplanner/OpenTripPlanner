package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.model.KeyValue;

public class KeyValueType {
  public static final GraphQLObjectType TYPE = GraphQLObjectType.newObject()
      .name("KeyValue")
      .field(GraphQLFieldDefinition.newFieldDefinition()
          .name("key")
          .description("Identifier of value.")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((KeyValue) environment.getSource()).getKey())
          .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
          .name("value")
          .description("The actual value")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((KeyValue) environment.getSource()).getValue())
          .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
          .name("typeOfKey")
          .description("Identifier of type of key")
          .type(Scalars.GraphQLString)
          .dataFetcher(environment -> ((KeyValue) environment.getSource()).getTypeOfKey())
          .build())
      .build();

}

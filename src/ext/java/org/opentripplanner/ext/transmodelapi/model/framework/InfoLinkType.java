package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.routing.alertpatch.AlertUrl;

public class InfoLinkType {

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
        .name("infoLink")
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("uri")
                .type(Scalars.GraphQLString)
                .description("URI")
                .dataFetcher(environment -> {
                    AlertUrl source = environment.getSource();
                    return source.uri;
                })
                .build())
        .field(GraphQLFieldDefinition.newFieldDefinition()
                .name("label")
                .type(Scalars.GraphQLString)
                .description("Label")
                .dataFetcher(environment -> {
                    AlertUrl source = environment.getSource();
                    return source.label;
                })
                .build())
        .build();
  }
}

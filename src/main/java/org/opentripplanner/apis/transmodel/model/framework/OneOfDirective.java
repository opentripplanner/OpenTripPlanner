package org.opentripplanner.apis.transmodel.model.framework;

import graphql.introspection.Introspection;
import graphql.schema.GraphQLDirective;

public class OneOfDirective {

  public static final GraphQLDirective ONE_OF_DIRECTIVE = GraphQLDirective
    .newDirective()
    .description("One and only one of the fields in the (input) type must be non null.")
    .validLocations(
      Introspection.DirectiveLocation.INPUT_OBJECT,
      Introspection.DirectiveLocation.OBJECT
    )
    .name("OneOf")
    .build();
}

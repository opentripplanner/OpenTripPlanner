package org.opentripplanner.apis.transmodel.model.framework;

import graphql.introspection.Introspection;
import graphql.schema.GraphQLDirective;

public class TransmodelDirectives {

  public static final GraphQLDirective TIMING_DATA = GraphQLDirective.newDirective()
    .name("timingData")
    .description("Add timing data to prometheus, if Actuator API is enabled")
    .validLocation(Introspection.DirectiveLocation.FIELD_DEFINITION)
    .build();
}

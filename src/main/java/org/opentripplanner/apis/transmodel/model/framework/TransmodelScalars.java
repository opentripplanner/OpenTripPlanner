package org.opentripplanner.apis.transmodel.model.framework;

import graphql.introspection.Introspection;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import org.opentripplanner.apis.transmodel.model.scalars.DoubleFunctionFactory;
import org.opentripplanner.apis.transmodel.model.scalars.LocalTimeScalarFactory;
import org.opentripplanner.apis.transmodel.model.scalars.TimeScalarFactory;
import org.opentripplanner.framework.graphql.scalar.DateScalarFactory;
import org.opentripplanner.framework.graphql.scalar.DurationScalarFactory;

/**
 * This class contains all Transmodel custom scalars, except the
 * {@link org.opentripplanner.apis.transmodel.support.GqlUtil#dateTimeScalar}.
 */
public class TransmodelScalars {

  public static final GraphQLScalarType DATE_SCALAR;
  public static final GraphQLScalarType DOUBLE_FUNCTION_SCALAR;
  public static final GraphQLScalarType LOCAL_TIME_SCALAR;
  public static final GraphQLObjectType TIME_SCALAR;
  public static final GraphQLScalarType DURATION_SCALAR;
  public static final GraphQLDirective TIMING_DATA;

  static {
    DATE_SCALAR = DateScalarFactory.createTransmodelDateScalar();
    DOUBLE_FUNCTION_SCALAR = DoubleFunctionFactory.createDoubleFunctionScalar();
    LOCAL_TIME_SCALAR = LocalTimeScalarFactory.createLocalTimeScalar();
    TIME_SCALAR = TimeScalarFactory.createSecondsSinceMidnightAsTimeObject();
    DURATION_SCALAR = DurationScalarFactory.createDurationScalar();
    TIMING_DATA =
      GraphQLDirective
        .newDirective()
        .name("timingData")
        .description("Add timing data to prometheus, if Actuator API is enabled")
        .validLocation(Introspection.DirectiveLocation.FIELD_DEFINITION)
        .build();
  }
}

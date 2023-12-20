package org.opentripplanner.apis.transmodel.model.framework;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.transmodel.model.siri.sx.ValidityPeriod;
import org.opentripplanner.apis.transmodel.support.GqlUtil;

public class ValidityPeriodType {

  public static GraphQLObjectType create(GqlUtil gqlUtil) {
    return GraphQLObjectType
      .newObject()
      .name("ValidityPeriod")
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("startTime")
          .type(gqlUtil.dateTimeScalar)
          .description("Start of validity period")
          .dataFetcher(environment -> {
            ValidityPeriod period = environment.getSource();
            return period != null ? period.startTime() : null;
          })
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("endTime")
          .type(gqlUtil.dateTimeScalar)
          .description("End of validity period. Will return 'null' if validity is open-ended.")
          .dataFetcher(environment -> {
            ValidityPeriod period = environment.getSource();
            return period != null ? period.endTime() : null;
          })
          .build()
      )
      .build();
  }
}
